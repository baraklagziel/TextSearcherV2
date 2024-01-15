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
import java.util.stream.Collectors;

import static com.textsearcherv2.controller.ControllerConstants.CORES;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@Service
public class ContentProcessorService {
    private static final int CHUNK_SIZE_LIMIT = 1000; // Specify Chunk Size
    private static final String LINE_DELIMITER = "\n";
    private static final Logger logger = LogManager.getLogger(MatcherService.class);

    @Autowired
    final MatcherService matcherService; // Assuming
    // Executor Service for processing chunks
    private ExecutorService matcherExecutor = Executors.newFixedThreadPool(CORES); // adjust the thr


    public CompletableFuture<List<String>> processContentInChunksStep(String content) {
        logger.info("in process content to chunk stage: content size {}", content.length());
        return processContentInChunks(content, CHUNK_SIZE_LIMIT);
    }

//    private CompletableFuture<List<String>> processContentInChunks(String contentPart, int chunkSize) {
//        List<CompletableFuture<String>> futures = new ArrayList<>();
//        Collection<List<String>> chunks = createChunks(contentPart, chunkSize);
//        for (List<String> chunkLines : chunks) {
//            String joinedChunkLines = String.join(LINE_DELIMITER, chunkLines);
//            CompletableFuture<String> future = matcherService.match(joinedChunkLines, matcherExecutor)
//                    .thenApply(this::processResult)
//                    .exceptionally(this::logException);
//            futures.add(future);
//        }
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                .thenApply(v -> futures.stream()
//                        .map(CompletableFuture::join)
//                        .collect(Collectors.toList()));
//    }

    private String logException(Throwable throwable) {
//        LOGGER.log(Level.SEVERE, "Exception occurred during content processing", throwable);
        return null; // Returning null or a default value as per your error handling strategy
    }


    private Collection<List<String>> createChunks(String contentPart, int chunkSize) {
        BufferedReader reader = new BufferedReader(new StringReader(contentPart));
        AtomicInteger counter = new AtomicInteger();
        return reader.lines()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                .values();
    }

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


        private CompletableFuture<List<String>> processContentInChunks(String contentPart, int chunkSize) {
        Collection<List<String>> chunks = createChunks(contentPart, chunkSize);
        List<CompletableFuture<String>> futures = processChunks(chunks);
        return combineFutures(futures);
    }

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

    private CompletableFuture<List<String>> combineFutures(List<CompletableFuture<String>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
}
