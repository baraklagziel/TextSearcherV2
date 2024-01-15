package com.textsearcherv2.controller;

import com.textsearcherv2.service.ProcessingService;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.textsearcherv2.controller.ControllerConstants.*;

@RestController
@RequestMapping(value = V1)
@Log4j2
@SpringBootApplication(scanBasePackages = "com.textsearcherv2")
public class TextSearcherController {
    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(TextSearcherController.class);

    @Autowired
    private ProcessingService processingService;

    /**
     * Endpoint to process a list of URLs. Receives a POST request with a JSON body containing a list of URLs as strings.
     * This method starts the processing of URLs and immediate response is returned to the caller.
     *
     * @param urls A list of URLs received in the request body to be processed.
     * @return A {@link ResponseEntity} the HTTP Status 200 (OK) and a message indicating the successful start of the process.
     *
     * <p>Example of a Curl command:
     * <br>curl -X POST -H "Content-Type: application/json" -d '["http://example1.com", "http://example2.com"]' http://localhost:8080/PROCESS_URLS</p>
     *
     * @throws IllegalArgumentException if the urls list is null or empty.
     */
    @PostMapping(value = PROCESS_URL)
    public ResponseEntity<String> processUrls(@RequestBody List<String> urls) {
        logger.info("Received request to process URLs: {}", urls);
        processingService.start(urls, CHUNK_SIZE_LIMIT);
        logger.info("Processing of URLs started");
        return ResponseEntity.ok("Processing Finished");
    }
}
