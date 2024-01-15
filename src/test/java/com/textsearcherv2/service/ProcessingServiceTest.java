package com.textsearcherv2.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


@SpringBootTest
class ProcessingServiceTest {

    @Mock
    private FileReaderService fileReaderService;

    @InjectMocks
    private ProcessingService unit;

    /**
     * This method is used to test the `start` method of the `ProcessingService` class.
     *
     * The `start` method starts processing a list of file URLs asynchronously, dividing the files into parts based
     * on the specified number of lines per part.
     *
     * The method first calls the `getFutureListFromUrl` method of the `fileReaderService` object, passing the `fileURLs`
     * and `linesPerPart` parameters. This method retrieves the list of CompletableFuture objects that represent the
     * asynchronous completion of fetching and processing content from multiple URLs.
     *
     * The test then verifies that the `getFutureListFromUrl` method of the `fileReaderService` object was called with the
     * expected `fileURLs` and `linesPerPart` parameters.
     *
     * As the test method is annotated with `@Disabled`, it will be skipped by the test execution framework.
     *
     * Note: This documentation does not show the author or version tags, and does not include example code.
     */
    @Test
    @Disabled
     void startTest() {
        List<String> fileURLs = List.of("file1", "file2");
        int linesPerPart = 8;
        CompletableFuture<Void> future = new CompletableFuture<>();
        Mockito.when(fileReaderService.getFutureListFromUrl(fileURLs, linesPerPart))
                .thenReturn(List.of(future));
      
        unit.start(fileURLs, linesPerPart);

        Mockito.verify(fileReaderService).getFutureListFromUrl(eq(fileURLs), eq(linesPerPart));
    }
}