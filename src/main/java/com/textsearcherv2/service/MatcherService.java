package com.textsearcherv2.service;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.textsearcherv2.model.TextPosition;

import static com.textsearcherv2.controller.ControllerConstants.CORES;


@Service
@Log4j2
public class MatcherService {
    @Autowired
    private final AggregatorService aggregatorService;

    @Autowired
    public MatcherService(@Lazy AggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }
    public static final Set<String> PERSON_NAMES = new HashSet<>(Arrays.asList(
            "James","John","Robert","Michael","William","David","Richard","Charles","Joseph","Thomas","Christopher",
            "Daniel","Paul","Mark","Donald","George","Kenneth","Steven","Edward","Brian","Ronald","Anthony","Kevin",
            "Jason","Matthew","Gary","Timothy","Jose","Larry","Jeffrey","Frank","Scott","Eric","Stephen","Andrew",
            "Raymond","Gregory","Joshua","Jerry","Dennis","Walter","Patrick","Peter","Harold","Douglas","Henry",
            "Carl","Arthur","Ryan","Roger"
    ));
    private static final Logger logger = LogManager.getLogger(MatcherService.class);
    private static final ExecutorService executorService = Executors.newFixedThreadPool(CORES); // create a thread pool with a fixed number of threads



    public CompletableFuture<List<String>> match(String content, final ExecutorService matcherExecutor) {
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();

        List<String> contentParts = Arrays.asList(content.split("\n"));
        for (String part : contentParts) {
            CompletableFuture<List<String>> future = getContentMap(part)
                    .thenComposeAsync(this::joinFutureContentMap, matcherExecutor);
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()));
    }

    private CompletableFuture<List<String>> joinFutureContentMap(ConcurrentHashMap<String, ConcurrentLinkedQueue<TextPosition>> contentMap) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> result = new ArrayList<>();
            contentMap.forEach((name, positionsQueue) -> {
                for (TextPosition position : positionsQueue) {
                    result.add(name + " at " + position);
                }
            });
            return result;
        });
    }


    private Optional<List<String>> joinFutureContentMap(CompletableFuture<ConcurrentHashMap<String, ConcurrentLinkedQueue<TextPosition>>> futureContentMap) {
        try {
            ConcurrentHashMap<String, ConcurrentLinkedQueue<TextPosition>> contentMap = futureContentMap.get(); // Blocks and waits for the future to complete
            List<String> result = new ArrayList<>();
            contentMap.forEach((key, value) -> {
                // Process the map and add to result as needed
                // Example: result.add(key + ": " + value.toString());
            });
            return Optional.of(result);
        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
            // Example: log the error or return an empty Optional
            return Optional.empty();
        }
    }

     CompletableFuture<ConcurrentHashMap<String, ConcurrentLinkedQueue<TextPosition>>> getContentMap(String content) {
         return CompletableFuture.supplyAsync(() -> {
             ConcurrentHashMap<String, ConcurrentLinkedQueue<TextPosition>> contentMap = new ConcurrentHashMap<>();

             String[] lines = content.split("\n");
             for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
                 String line = lines[lineNumber];
                 for (String name : PERSON_NAMES) {
                     int charIndex = line.indexOf(name);
                     while (charIndex >= 0) {
                         ConcurrentLinkedQueue<TextPosition> positions = contentMap.computeIfAbsent(name, k -> new ConcurrentLinkedQueue<>());
                         positions.add(new TextPosition(lineNumber, charIndex));
                         // Search for the next occurrence of the name in the line
                         charIndex = line.indexOf(name, charIndex + 1);
                     }
                 }
             }
             return contentMap;
         }, executorService);
    }


    private Map<String, List<TextPosition>> getPositionsMap(String content) {

        Map<String, List<TextPosition>> namePositionsMap = new HashMap<>();

        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            namePositionsMap = findMatches(lines[i], namePositionsMap, i);
        }

        return namePositionsMap;
    }

    private Map<String, List<TextPosition>> findMatches(String line,
                                                        Map<String, List<TextPosition>> namePositionsMap,
                                                        int lineNumber) {
        Map<String, List<TextPosition>> textPositionListByName = new HashMap<>();

        for (String name : PERSON_NAMES) {
            int charIndex = line.indexOf(name);

            if (charIndex != -1) {
                List<TextPosition> positions = namePositionsMap.computeIfAbsent(name, k -> new ArrayList<>());
                positions.add(new TextPosition(lineNumber, charIndex));

                textPositionListByName.putIfAbsent(name, new ArrayList<>());
                textPositionListByName.get(name).addAll(positions);
            }
        }

        return textPositionListByName;
    }



    // Method to create Match Task
    Runnable createMatchTask(String content, final ExecutorService matcherExecutor) {

        CompletableFuture<List<String>> matchResultFuture = this.match(content, matcherExecutor);

        matchResultFuture.join();

        return () -> this.match(content, matcherExecutor);
    }
}