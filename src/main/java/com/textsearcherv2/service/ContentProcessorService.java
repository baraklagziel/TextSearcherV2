package com.textsearcherv2.service;

import com.textsearcherv2.model.TextPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.textsearcherv2.controller.ControllerConstants.CORES;
import static com.textsearcherv2.service.FileReaderService.CHUNK_SIZE_LIMIT;
import static com.textsearcherv2.service.ServiceConstants.LINE_DELIMITER;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@Service
public class ContentProcessorService {

    private static final Logger logger = LogManager.getLogger(MatcherService.class);

    @Autowired
    final MatcherService matcherService;
    // Executor Service for processing chunks
    private ExecutorService matcherExecutor = Executors.newFixedThreadPool(CORES); // adjust the thr


    /**
     * Process the content in chunks.
     *
     * @param content The content to be processed.
     * @return A CompletableFuture that completes with a list of strings representing the processed content.
     */
    public CompletableFuture<List<String>> processContentInChunksStep(String content) {
        logger.info("in process content to chunk stage: content size {}", content.length());
        return processContentInChunks(content, CHUNK_SIZE_LIMIT);
    }

    private String logException(Throwable throwable) {
        logger.info("Exception occurred during content processing", throwable);
        return null; // Returning null as an error signal
    }

    /**
     * Creates chunks of lines from the given content part
     *
     * @param contentPart The content part to create chunks from
     * @param chunkSize   The size of each chunk
     * @return A collection of lists of strings representing the chunks
     */
    private Collection<List<String>> createChunks(String contentPart, int chunkSize) {
        BufferedReader reader = new BufferedReader(new StringReader(contentPart));
        AtomicInteger counter = new AtomicInteger();
        return reader.lines()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                .values();
    }

    /**
     * Process the result list.
     *
     * @param resultList The list to be processed.
     * @return A string representing the processed result.
     */
    private String processResult(List<?> resultList) {
        logger.info("in process result stage: {}", resultList);
        if (resultList == null || resultList.isEmpty()) {
            return ""; // Return an empty string or handle it as per your business logic
        }

        if (resultList.get(0) instanceof String) {
            return resultList.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")); // Concatenating with a comma and space
        }
        // If resultList contains objects of other types, handle accordingly

        return ""; // Default return, in case of unexpected data types
    }

    /**
     * Processes the content in chunks.
     *
     * @param contentPart The content part to be processed.
     * @param chunkSize The size of each chunk.
     * @return A CompletableFuture that completes with a list of strings representing the processed content.
     */
    private CompletableFuture<List<String>> processContentInChunks(String contentPart, int chunkSize) {
        Collection<List<String>> chunks = createChunks(contentPart, chunkSize);
        List<CompletableFuture<String>> futures = processChunks(chunks);
        return combineFutures(futures);
    }

    /**
     * Processes the given chunks of content asynchronously.
     *
     * @param chunks The collection of lists of strings representing the chunks of content.
     * @return A list of CompletableFutures, each representing the processing result for a chunk of content.
     */
    private List<CompletableFuture<String>> processChunks(Collection<List<String>> chunks) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (List<String> chunkLines : chunks) {
            String joinedChunkLines = String.join(LINE_DELIMITER, chunkLines);
            CompletableFuture<String> future = matcherService.match(joinedChunkLines, matcherExecutor)
                    .thenApply(this::processResult)
                    .exceptionally(this::logException);
            futures.add(future);
        }
        return futures;
    }

    /**
     * Combines a list of CompletableFutures into a single CompletableFuture that completes with a list of strings.
     *
     * @param futures The list of CompletableFutures representing the processing result for each chunk.
     * @return A CompletableFuture that completes with a list of strings representing the combined processing results.
     */
    private CompletableFuture<List<String>> combineFutures(List<CompletableFuture<String>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
}
