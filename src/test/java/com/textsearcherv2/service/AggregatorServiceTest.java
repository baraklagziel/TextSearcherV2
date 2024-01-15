package com.textsearcherv2.service;

import com.textsearcherv2.model.TextPosition;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AggregatorServiceTest class aims to test methods in the AggregatorService.java.
 * Specifically, it tests the findMatchesInContent method and checks if it correctly finds and returns
 * the matches of individual words in the provided content. 
 */
 class AggregatorServiceTest {

    /**
     * Tests the findMatchesInContent method of AggregatorService.
     * The test is focused on the case where the content contains one word, with expected behaviour
     * that the method should return this word in a map with one TextPosition where it is located.
     */
    @Test
     void testFindMatchesInContent_singleWord() throws ExecutionException, InterruptedException {
        //init
        String content = "word";
        AggregatorService aggregatorService = new AggregatorService();

        //when
        CompletableFuture<Map<String, List<TextPosition>>> resultFuture = aggregatorService.findMatchesInContent(content);
        Map<String, List<TextPosition>> resultMap = resultFuture.get();

        //then
        assertEquals(1, resultMap.size(),"There should be a single entry in the result map.");
        assertTrue(resultMap.containsKey("word"), "The map should contain an entry for the key 'word'.");
        assertEquals(Collections.singletonList(new TextPosition(0, 0)),resultMap.get(content),
                "There should be a single TextPosition with line and column both set to 0.");
    }

    /**
     * Tests the findMatchesInContent method of AggregatorService.
     * The test is focused on the case where the content is empty, with expected behaviour
     * that the method should return an empty map.
     */
    @Test
     void testFindMatchesInContent_emptyContent() throws ExecutionException, InterruptedException {
        //init
        String content = "";
        AggregatorService aggregatorService = new AggregatorService();

        //when
        CompletableFuture<Map<String, List<TextPosition>>> resultFuture = aggregatorService.findMatchesInContent(content);
        Map<String, List<TextPosition>> resultMap = resultFuture.get();

        //then
        assertTrue(resultMap.isEmpty(), "The result map should be empty.");
    }
}