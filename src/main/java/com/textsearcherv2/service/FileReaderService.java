package com.textsearcherv2.service;

import com.textsearcherv2.model.TextPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.textsearcherv2.validation.UrlValidationService;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.textsearcherv2.controller.ControllerConstants.CORES;
import static com.textsearcherv2.service.MatcherService.PERSON_NAMES;

@Service
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@Log4j2
public class FileReaderService {
    public static int CHUNK_SIZE_LIMIT;
    private String content;

    private final AtomicInteger CHAR_OFFSET_SUM = new AtomicInteger(0);

    @Autowired
    private UrlValidationService urlValidationService;

    @Autowired
    private MatcherService matcherService;

    @Autowired
    private AggregatorService aggregatorService;

    @Autowired
    private ContentProcessorService contentProcessorService;

    private static final Logger logger = LogManager.getLogger(FileReaderService.class);

    private Map<String, List<TextPosition>> aggregatedResults = new ConcurrentHashMap<>();

    private List<Map<String, List<TextPosition>>> resultsByPart = new ArrayList<>();

    // Executor Service for processing chunks
    private ExecutorService matcherExecutor = Executors.newFixedThreadPool(CORES); // adjust the thread count as needed

    /**
     * Asynchronously fetches the content from a given URL.
     *
     * @param url The URL to fetch content from.
     * @return A CompletableFuture that, when completed, will contain the content fetched from the URL.
     * If the URL is invalid or unsafe, the CompletableFuture will complete exceptionally with an IllegalArgumentException.
     */
    @Async
    public CompletableFuture<Void> fetchContentAndProcess(String url, int linePerPart) {
        if (!urlValidationService.isValidUrl(url)) {
            logger.warn("Invalid or unsafe URL provided: {}", url);
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalArgumentException("Invalid or unsafe URL"));
            return failedFuture;
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        Function<HttpResponse<String>, String> extractBody = HttpResponse::body;

        // Exception handler
        Function<Throwable, Void> exceptionHandler = ex -> {
            logger.error("Exception occurred while processing", ex);
            return null;
        };

        // Chain all steps
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(extractBody) // Extract body from the response
                .thenCompose(contentChunk -> contentProcessorService.processContentInChunksStep(contentChunk)) // Map Stage: Process content in chunks
                .thenApply(this::transformListToMap) // Convert List to Map if needed
                .thenCompose(aggregatorService::aggregateAndPrintResults) // Reduce Stage: Aggregate the processed content
                .thenAcceptAsync(result -> {
                    // Handle the aggregated result here
                }, matcherExecutor)
                .exceptionally(exceptionHandler);

    }
    private AtomicInteger charOffsetSum = new AtomicInteger(0);

    private Map<String, List<TextPosition>> transformListToMap(List<String> contentList) {
        Map<String, List<TextPosition>> namePositions = new HashMap<>();

        int cumulativeCharCount = 0;  // Tracks the cumulative number of characters processed.

        for (int lineNumber = 0; lineNumber < contentList.size(); lineNumber++) {
            String line = contentList.get(lineNumber);
            int lineLength = line.length();

            for (String name : PERSON_NAMES) {
                int charIndex = line.indexOf(name);
                while (charIndex >= 0) {
                    // Calculate the actual character offset in the entire text.
                    int actualCharOffset = cumulativeCharCount + charIndex;
                    charOffsetSum.addAndGet(actualCharOffset);

                    namePositions.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(new TextPosition(lineNumber * CHUNK_SIZE_LIMIT, actualCharOffset));

                    // Search for the next occurrence of the name in the line.
                    charIndex = line.indexOf(name, charIndex + name.length());
                }
            }

            cumulativeCharCount += lineLength + 1;  // Add the line length and 1 for the newline character.
        }

        return namePositions;
    }
    // Parse a string into a TextPosition object
    public static TextPosition parseTextPosition(String positionData) {
        try {
            String[] parts = positionData.split(",");
            if (parts.length == 2) {
                int lineNumber = Integer.parseInt(parts[0].trim());
                int columnNumber = Integer.parseInt(parts[1].trim());
                return new TextPosition(lineNumber, columnNumber);
            }
        } catch (NumberFormatException e) {
            // Log error or handle it as per your application's error handling strategy
        }
        // Return null or throw an exception as per your error handling strategy
        return null;
    }

    private String processResult(List<?> resultList) {
        if (resultList instanceof List) {
            List<String> matchedContents = (List<String>) resultList;
            return matchedContents.stream()
                    .map(CompletableFuture::completedFuture)
                    .toList().toString();
        } else {
//            LOGGER.error("The result should be of type List<String>");
            return null;
        }
    }

    private String logException(Throwable ex) {
//        LOGGER.error("Exception in processing chunk", ex);
        return null;
    }


    private Collection<List<String>> createChunks(String contentPart, int chunkSize) {
        BufferedReader reader = new BufferedReader(new StringReader(contentPart));
        AtomicInteger counter = new AtomicInteger();
        return reader.lines()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                .values();
    }

    public CompletableFuture<Void> storeContent(CompletableFuture<List<String>> fetchedContentFuture, String url) {
        return fetchedContentFuture.thenAccept(respContent -> {
            this.content = String.join(" ", respContent);
            logger.info("Content fetched and stored successfully for URL: {}, the content length: {}", url, this.content.getBytes().length);
        }).exceptionally(ex -> {
            logger.error("Error occurred while fetching URL", ex);
            return null;
        });
    }
    // Fetches and processes content for a single URL
    private CompletableFuture<String> fetchContentAndProcessSingleWorker(String fileURL) {
        // Implement fetching and processing logic here
        // For example, fetching content from the URL and then processing it
        // Placeholder return value
        return CompletableFuture.completedFuture("processedData");
    }

    // Applies additional processing to the data
    private CompletableFuture<Void> applyWorkers(String processData) {
        // Implement additional processing logic here
        // For example, applying some transformations or calculations
        // Placeholder logic
        return CompletableFuture.runAsync(() -> System.out.println("Data: " + processData));
    }

    // Chains fetching and processing for a single URL
    private CompletableFuture<Void> fetchAndApplyProcess(String fileURL) {
        return this.fetchContentAndProcessSingleWorker(fileURL).thenCompose(this::applyWorkers);
    }

    // Processes multiple URLs
    public List<CompletableFuture<Void>> getFutureListFromUrl(final List<String> fileURLs, int linePerPart) {
        CHUNK_SIZE_LIMIT = linePerPart;
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (String fileURL : fileURLs) {
            CompletableFuture<Void> future = this.fetchContentAndProcess(fileURL, 1000);
            allFutures.add(future);
        }
        return allFutures;
    }

    // Default method to process URLs with a standard line per part
    public List<CompletableFuture<Void>> getFutureListFromUrl(final List<String> fileURLs) {
        return getFutureListFromUrl(fileURLs, 100); // default line per part
    }

//    private CompletableFuture<String> fetchContentAndProcessSingleWorker(String fileURL) {
//        // Your existing logic
//        return CompletableFuture.completedFuture("processedData");
//    }
//
//    private CompletableFuture<Void> applyWorkers(String processData) {
//        // Your existing logic
//        return CompletableFuture.runAsync(() -> System.out.println("Data: " + processData));
//    }
//
//    private CompletableFuture<Void> fetchAndApplyProcess(String fileURL) {
//        return this.fetchContentAndProcessSingleWorker(fileURL).thenCompose(this::applyWorkers);
//    }
//
//
//
//    private List<CompletableFuture<Void>> createFuturesForParts(List<String> parts) {
//        return parts.stream()
//                .map(part -> CompletableFuture.runAsync(matcherService.createMatchTask(part, matcherExecutor), matcherExecutor))
//                .collect(Collectors.toList());
//    }
//
//    List<CompletableFuture<Void>> getFutureListFromUrl(final List<String> fileURLs) {
//        return getFutureListFromUrl(fileURLs, 100); // default line per part
//    }
//
//    List<CompletableFuture<Void>> getFutureListFromUrl(final List<String> fileURLs, int linePerPart) {
//        CHUNK_SIZE_LIMIT = linePerPart;
//        List<CompletableFuture<Void>> allFutures = new ArrayList<>();
//
//        for (String fileURL : fileURLs) {
//            CompletableFuture<Void> future = this.fetchAndApplyProcess(fileURL);
//            allFutures.add(future);
//        }
//        return allFutures;
//    }


}
