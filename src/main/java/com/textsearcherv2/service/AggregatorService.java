package com.textsearcherv2.service;

import com.textsearcherv2.model.TextPosition;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.textsearcherv2.service.MatcherService.PERSON_NAMES;

@Service
@Log4j2
@NoArgsConstructor(force = true)
public class AggregatorService {

    private final Map<String, List<TextPosition>> aggregatedResults = new ConcurrentHashMap<>();
    private List<CompletableFuture<Void>> futures = new ArrayList<>();

    public CompletableFuture<Void> aggregateAndPrintResults(Map<String, List<TextPosition>> textPositionsByName) {
        return CompletableFuture.runAsync(() -> executePositionAggregationAndLogging(textPositionsByName));
    }

    private void executePositionAggregationAndLogging(Map<String, List<TextPosition>> textPositionsByName) {
//        aggregatePositionsMap(textPositionsByName);
        logNamePositions(textPositionsByName);
    }

    private void aggregatePositionsMap(Map<String, List<TextPosition>> textPositionsByName) {
        textPositionsByName.forEach(this::addTextPositionByNameAndFilter);
        textPositionsByName.entrySet().removeIf(entry -> !PERSON_NAMES.contains(entry.getKey()));
    }

    private void addTextPositionByNameAndFilter(String name, List<TextPosition> textPositions) {
        addTextPositionByName(Map.of(name, textPositions), name, (TextPosition) textPositions);
    }

    private void logNamePositions(Map<String, List<TextPosition>> textPositionsByName) {
        logMatchResults(textPositionsByName);
    }


    private void addTextPositionByName(Map<String, List<TextPosition>> textPositionsByName, String name, TextPosition position) {
        textPositionsByName.merge(name, new ArrayList<>(Collections.singletonList(position)),
                (existingPositions, newPosition) -> {
                    existingPositions.addAll(newPosition);
                    return existingPositions;
                });
    }

//    private void printResults(Map<String, List<TextPosition>> results) {
//        results.entrySet().removeIf(entry -> !PERSON_NAMES.contains(entry.getKey()));
//        aggregatorSubmitter(results);
//    }

    private void logMatchResults(Map<String, List<TextPosition>> positionsMap) {
        for (Map.Entry<String, List<TextPosition>> item : positionsMap.entrySet()) {
            if (PERSON_NAMES.contains(item.getKey())) {
                String positionsString = item.getValue().stream()
                        .map(TextPosition::toString)
                        .collect(Collectors.joining(", ", "[", "]"));
                System.out.println(item.getKey() + " --> " + positionsString);
            }
        }
    }


//    private void aggregatorSubmitter(Map<String, List<TextPosition>> positionsMap) {
//        logMatchResults(positionsMap);
//    }


    public CompletableFuture<Map<String, List<TextPosition>>> findMatchesInContent(String content) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, List<TextPosition>> matches = new HashMap<>();
            String[] lines = content.split("\n");

            Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
            for (int i = 0; i < lines.length; i++) {
                Matcher wordMatcher = wordPattern.matcher(lines[i]);
                while (wordMatcher.find()) {
                    String word = wordMatcher.group();
                    int columnNumber = wordMatcher.start();
                    matches.computeIfAbsent(word, k -> new ArrayList<>())
                            .add(new TextPosition(i, columnNumber));
                }
            }
            return matches;
        });
    }

    public CompletableFuture<Void> aggregate(String content) {
        return findMatchesInContent(content).thenApply(matches -> {
            Map<String, List<TextPosition>> result = new HashMap<>();
            matches.forEach((key, positions) -> {
                TextPosition aggregatedPosition = constructTextPosition(positions);
                result.put(key, Arrays.asList(aggregatedPosition));
                List<TextPosition> list = new ArrayList<>(positions);
                mergeLists(key, list);
            });
            return result;
        }).thenCompose(matches -> this.aggregateAndPrintResults(matches));
    }




    private TextPosition constructTextPosition(List<TextPosition> positions) {
        // TODO: Revise this logic based on how you want to construct TextPosition from a list of TextPositions
        TextPosition textPosition = new TextPosition();
        // Example: set to the first position, or combine them in some way
        if (!positions.isEmpty()) {
            textPosition.setLineNumber(positions.get(0).getLineNumber());
            textPosition.setColumnNumber(positions.get(0).getColumnNumber());
        }
        return textPosition;
    }

    private void mergeLists(String key, List<TextPosition> value) {
        aggregatedResults.merge(key, value, (existingList, newList) -> {
            existingList.addAll(newList);
            return existingList;
        });
    }

    Map<String, List<TextPosition>> mergeMaps(Map<String, List<TextPosition>> map1, Map<String, List<TextPosition>> map2) {
        Map<String, List<TextPosition>> result = new HashMap<>(map1);
        map2.forEach((key, value) -> result.merge(key, value, (existing, newValues) -> {
            existing.addAll(newValues);
            return existing;
        }));
        return result;
    }


}
