package com.textsearcherv2.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.*;

import static com.textsearcherv2.controller.ControllerConstants.CORES;

@Service
@AllArgsConstructor
@Log4j2
public class ProcessingService {
    private static final long THREAD_WAIT_SECONDS = 60;
    private List<String> contents = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(ProcessingService.class);
    private ExecutorService executorService;

    public ProcessingService() {
        executorService = Executors.newFixedThreadPool(CORES);
    }

    @Autowired
    private FileReaderService fileReaderService;


    // Extracted constant for thread pool size
    private static final int THREAD_POOL_SIZE = 1;
    ExecutorService matcherExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


    // In the class where start method is
    public void start(List<String> fileURLs, int linesPerPart) {
        // Get a list of CompletableFuture<Void> for each URL
        List<CompletableFuture<Void>> allFutures = fileReaderService.getFutureListFromUrl(fileURLs, linesPerPart);

        // Use CompletableFuture.allOf to wait for all futures to complete
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));

        // Optionally, you can add a completion handler
        allDoneFuture.thenRun(() -> {
            // Code to execute when all futures are complete
            System.out.println("All processing completed.");
        });

        // If you need to block and wait for all to complete (though generally not recommended in async programming):
         allDoneFuture.join();
    }


    public void shutdown() {
        executorService.shutdown();
        awaitThreadPoolTerminationOrShutdownNow();
    }

    private void awaitThreadPoolTerminationOrShutdownNow() {
        try {
            if (!executorService.awaitTermination(THREAD_WAIT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(THREAD_WAIT_SECONDS, TimeUnit.SECONDS)) {
                    logger.warn("ThreadPoolExecutor did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public CompletableFuture<String> storeContent(CompletableFuture<String> fetchedContentFuture, String url) {
        return fetchedContentFuture.thenApply(respContent -> {
            this.contents.add(respContent);
            logger.info("Content fetched and stored successfully for URL: {}, the content length: {}", url,
                    respContent.getBytes().length);
            return respContent;
        }).exceptionally(ex -> {
            logger.error("Error occurred while fetching URL", ex);
            return null;
        });
    }

    private String handleMatchingException(Throwable ex) {
        logger.error("Exception occurred while matching content: " + ex.getMessage());
        return null;
    }

    private Void handleStoringException(Throwable ex) {
        logger.error("Exception occurred while storing content: " + ex.getMessage());
        return null;
    }

    private void validateUrls(List<String> urls) {
        for (String url : urls) {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
        }
    }
}