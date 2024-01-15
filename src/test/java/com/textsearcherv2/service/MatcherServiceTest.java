package com.textsearcherv2.service;

import com.textsearcherv2.model.TextPosition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("test")
 class MatcherServiceTest {
   
    @Autowired
    private MatcherService matcherService;

    @MockBean
    private AggregatorService aggregatorService;


    /**
     * Returns a CompletableFuture that represents the asynchronous computation of a ConcurrentHashMap
     * containing the positions of given names in the provided content.
     *
     * <p>
     * The method splits the content into lines and searches for occurrences of names in each line.
     * For each occurrence, a TextPosition object is created with the line number and column number and
     * added to the corresponding queue in the ConcurrentHashMap.
     * </p>
     *
     * @param content the content in which to search for names
     * @return a CompletableFuture that will hold the ConcurrentHashMap with the positions of names
     * @throws Exception if an error occurs during the computation
     */
    @Test
    void testGetContentMap() throws Exception {

        // sample content
        String content = "John and Mary are friends.\nBrian and John live in the same street.\n";

        // test the getContentMap method
        CompletableFuture<ConcurrentHashMap<String, ConcurrentLinkedQueue<TextPosition>>> contentMapFuture = matcherService.getContentMap(content);

        ConcurrentHashMap<String, ConcurrentLinkedQueue<TextPosition>> contentMap = contentMapFuture.get(5, TimeUnit.SECONDS);

        assertThat(contentMap).isNotNull();

        // expecting 2 matches for John
        assertThat(contentMap.get("John")).hasSize(2);
        assertThat(contentMap.get("John")).containsExactlyInAnyOrder(new TextPosition(0, 0), new TextPosition(1, 10));
    }

}