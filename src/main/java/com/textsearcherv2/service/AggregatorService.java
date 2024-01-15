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

    /**
     * Executes position aggregation and logging for the given text positions.
     *
     * @param textPositionsByName a map containing the text positions grouped by name
     */
    private void executePositionAggregationAndLogging(Map<String, List<TextPosition>> textPositionsByName) {
        logNamePositions(textPositionsByName);
    }

    /**
     * Aggregates the given positions map by adding the text positions to the corresponding name,
     * and filters out entries whose key is not present in the list of person names.
     *
     * @param textPositionsByName the map of text positions to be aggregated, with names as keys and
     *                            corresponding text positions as values
     */
    private void aggregatePositionsMap(Map<String, List<TextPosition>> textPositionsByName) {
        textPositionsByName.forEach(this::addTextPositionByNameAndFilter);
        textPositionsByName.entrySet().removeIf(entry -> !PERSON_NAMES.contains(entry.getKey()));
    }

    /**
     * Adds a list of TextPosition objects to the existing map of text positions by name.
     *
     * @param name          the name of the text positions
     * @param textPositions the list of text positions to be added
     */
    private void addTextPositionByNameAndFilter(String name, List<TextPosition> textPositions) {
        addTextPositionByName(Map.of(name, textPositions), name, (TextPosition) textPositions);
    }

    /**
     * Logs the positions of text names in the given map.
     *
     * @param textPositionsByName a map containing text names as keys and a list of their corresponding text positions as values
     */
    private void logNamePositions(Map<String, List<TextPosition>> textPositionsByName) {
        logMatchResults(textPositionsByName);
    }


    /**
     * Adds a TextPosition to the specified Map of text positions by name.
     *
     * @param textPositionsByName The Map of text positions by name.
     * @param name The name of the text position to add.
     * @param position The TextPosition to be added.
     */
    private void addTextPositionByName(Map<String, List<TextPosition>> textPositionsByName, String name, TextPosition position) {
        textPositionsByName.merge(name, new ArrayList<>(Collections.singletonList(position)),
                (existingPositions, newPosition) -> {
                    existingPositions.addAll(newPosition);
                    return existingPositions;
                });
    }

    /**
     * Logs the match results for each person name in the provided positions map.
     *
     * @param positionsMap the map containing the person names as keys and their corresponding positions as values
     */
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

    /**
     * Finds matches of individual words in the given content and returns a CompletableFuture
     * which will eventually hold a map of words and their corresponding positions in the content.
     *
     * @param content The content in which the matches should be found
     * @return A CompletableFuture holding a map of words and their positions
     */
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

    /**
     * Aggregates the matches found in the given content and returns the aggregated result as a {@link CompletableFuture}.
     *
     * @param content the content to find matches in
     * @return a {@link CompletableFuture} that completes with the aggregated result
     */
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

    /**
     * Constructs a new TextPosition object based on a given list of TextPositions.
     *
     * @param positions the list of TextPositions to be used for constructing the new TextPosition
     * @return a new TextPosition object
     */
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

    /**
     * Merges the given list of {@code TextPosition} objects with the existing list associated with the specified key.
     * If the key does not exist, a new key-value pair will be created.
     *
     * @param key The key associated with the list of {@code TextPosition} objects.
     * @param value The list of {@code TextPosition} objects to be merged.
     */
    private void mergeLists(String key, List<TextPosition> value) {
        aggregatedResults.merge(key, value, (existingList, newList) -> {
            existingList.addAll(newList);
            return existingList;
        });
    }

    /**
     * Merges two maps of type Map<String, List<TextPosition>>. It copies all entries from map1 to a new map,
     * then adds or updates the entries from map2. If a key already exists in the new map, the lists of TextPosition
     * from map2 are appended to the values of that key.
     *
     * @param map1 the first map to be merged
     * @param map2 the second map to be merged
     * @return a new map that is the result of merging map1 and map2
     */
    Map<String, List<TextPosition>> mergeMaps(Map<String, List<TextPosition>> map1, Map<String, List<TextPosition>> map2) {
        Map<String, List<TextPosition>> result = new HashMap<>(map1);
        map2.forEach((key, value) -> result.merge(key, value, (existing, newValues) -> {
            existing.addAll(newValues);
            return existing;
        }));
        return result;
    }
}
