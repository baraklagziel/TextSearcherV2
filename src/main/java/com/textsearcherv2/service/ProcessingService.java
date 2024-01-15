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
import static com.textsearcherv2.service.ServiceConstants.THREAD_WAIT_SECONDS;

@Service
@AllArgsConstructor
@Log4j2
public class ProcessingService {
    private List<String> contents = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(ProcessingService.class);
    private ExecutorService executorService;

    public ProcessingService() {
        executorService = Executors.newFixedThreadPool(CORES);
    }

    @Autowired
    private FileReaderService fileReaderService;

    ExecutorService matcherExecutor = Executors.newFixedThreadPool(CORES);

    /**
     * Starts processing a list of file URLs asynchronously, dividing the files into parts based on the specified
     * number of lines per part.
     *
     * @param fileURLs     The list of file URLs to process.
     * @param linesPerPart The number of lines per part to divide the files into.
     */
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


    /**
     * Shuts down the executor service and waits for any pending tasks to complete.
     * If the tasks take too long to complete, they will be forcibly terminated.
     *
     * @see ExecutorService#shutdown()
     */
    public void shutdown() {
        executorService.shutdown();
        awaitThreadPoolTerminationOrShutdownNow();
    }

    /**
     * Waits for the termination of the executorService or forcefully shuts it down.
     * If the executorService does not terminate within THREAD_WAIT_SECONDS, it will be forcefully shut down.
     * If the executorService is still not terminated after being shut down, a warning message will be logged.
     * If the thread is interrupted while waiting for termination, the executorService is forcefully shut down and the thread's interrupt status is set.
     */
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

    /**
     * Stores the fetched content and returns it as a CompletableFuture.
     *
     * @param fetchedContentFuture the CompletableFuture containing the fetched content
     * @param url                  the URL from which the content was fetched
     * @return a CompletableFuture that completes successfully with the fetched content,
     *         or null if an error occurred
     */
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

    /**
     * Handles a matching exception by logging the error message and returning null.
     *
     * @param ex the matching exception
     * @return null
     */
    private String handleMatchingException(Throwable ex) {
        logger.error("Exception occurred while matching content: " + ex.getMessage());
        return null;
    }

    /**
     * Handles and logs any exception occurred while storing content.
     *
     * @param ex the exception that occurred while storing content
     * @return null
     */
    private Void handleStoringException(Throwable ex) {
        logger.error("Exception occurred while storing content: " + ex.getMessage());
        return null;
    }

    /**
     * Validates a list of URLs.
     *
     * @param urls the list of URLs to validate
     * @throws IllegalArgumentException if any URL is null or empty
     */
    private void validateUrls(List<String> urls) {
        for (String url : urls) {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
        }
    }
}