package com.textsearcherv2.service;

import com.textsearcherv2.validation.UrlValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@SpringBootTest
public class FileReaderServiceTest {

    @Autowired
    private FileReaderService fileReaderService;

    @MockBean
    private UrlValidationService urlValidationService;

    @MockBean
    private MatcherService matcherService;

    @MockBean
    private AggregatorService aggregatorService;

    @MockBean
    private ContentProcessorService contentProcessorService;

    /**
     * Asynchronously fetches the content from a given URL and processes it.
     *
     * @return A CompletableFuture that, when completed, will indicate the completion of the operation.
     * @throws ExecutionException   If the future completes exceptionally.
     * @throws InterruptedException If the thread is interrupted while waiting for the future to complete.
     */
    @Test
    public void testFetchContentAndProcess() throws ExecutionException, InterruptedException {
        String url = "http://test-url.com";
        int linePerPart = 1000;

        // Mock validation service to always return true for testing purposes
        when(urlValidationService.isValidUrl(any())).thenReturn(true);

        // Mock processContentInChunksStep to return an empty list wrapped in a completed future
        when(contentProcessorService.processContentInChunksStep(any()))
                .thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));

        // Mock aggregateAndPrintResults to return a completed future without a value
        when(aggregatorService.aggregateAndPrintResults(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> result = fileReaderService.fetchContentAndProcess(url, linePerPart);

        // Wait for the asynchronous operation to complete
        result.get(); // This throws ExecutionException or InterruptedException if the future completes exceptionally

        // Assertions
        assertThat(result).isNotNull();
        assertThat(result.isCompletedExceptionally()).isFalse(); // Ensure it did not complete exceptionally
    }

}