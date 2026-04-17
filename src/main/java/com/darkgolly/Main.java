package com.darkgolly;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Main {
    private static final String SEPARATOR = ";";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Используйте: java -Xmx1G -jar {название проекта}.jar {полный путь к входному файлу}");
            return;
        }
        long start = System.currentTimeMillis();
        Path inputPath = Path.of(args[0]);
        List<List<String>> resultGroups;

        try (BufferedReader reader = createBufferedReader(inputPath)) {
            List<String> lines = reader.lines().toList();
            resultGroups = findGroups(lines);
        }
        long countGroupsWithMoreThanOneElement = resultGroups.stream().filter(group -> group.size() > 1).count();

        try (PrintWriter writer = new PrintWriter("output.txt", StandardCharsets.UTF_8)) {
            writer.println("Количество групп с более чем одним элементом: " + countGroupsWithMoreThanOneElement);
            int idx = 1;
            for (List<String> group : resultGroups) {
                writer.println("\nГруппа " + idx++);
                for (String row : group) {
                    writer.println(row);
                }
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Групп с более чем одним элементом: " + countGroupsWithMoreThanOneElement);
        System.out.println("Время выполнения: " + (end - start) + " мс");
    }

    private static BufferedReader createBufferedReader(Path path) throws IOException {
        if (path.toString().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(path.toFile())), StandardCharsets.UTF_8));
        } else {
            return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        }
    }

    public static List<List<String>> findGroups(List<String> lines) {
        List<Map<String, Integer>> wordToGroupByColumn = new ArrayList<>();
        List<List<String>> groups = new ArrayList<>();
        Map<Integer, Integer> groupMerges = new HashMap<>();

        for (String line : lines) {
            String[] words = line.split(SEPARATOR);
            SortedSet<Integer> relatedGroups = new TreeSet<>();
            Map<String, Integer> newWords = new HashMap<>();

            for (int i = 0; i < words.length; i++) {
                String word = words[i];

                if (wordToGroupByColumn.size() <= i) {
                    wordToGroupByColumn.add(new HashMap<>());
                }

                if (isWordEmpty(word)) continue;
                if (isWordIncorrect(word)) break;

                Map<String, Integer> columnMap = wordToGroupByColumn.get(i);
                Integer groupId = columnMap.get(word);

                if (groupId != null) {
                    relatedGroups.add(resolveGroup(groupId, groupMerges));
                } else {
                    newWords.put(word, i);
                }
            }

            int groupId = assignGroup(relatedGroups, groups);

            for (Map.Entry<String, Integer> entry : newWords.entrySet()) {
                wordToGroupByColumn.get(entry.getValue()).put(entry.getKey(), groupId);
            }

            mergeGroups(groupId, relatedGroups, groupMerges, groups);
            groups.get(groupId).add(line);
        }

        groups.removeIf(Objects::isNull);
        groups.sort(Comparator.<List<String>>comparingInt(List::size).reversed());
        return groups;
    }

    private static int assignGroup(SortedSet<Integer> relatedGroups, List<List<String>> groups) {
        if (relatedGroups.isEmpty()) {
            groups.add(new ArrayList<>());
            return groups.size() - 1;
        }
        return relatedGroups.first();
    }

    private static void mergeGroups(int targetGroup,
                                    SortedSet<Integer> relatedGroups,
                                    Map<Integer, Integer> groupMerges,
                                    List<List<String>> groups) {
        for (int group : relatedGroups) {
            if (group != targetGroup) {
                groupMerges.put(group, targetGroup);
                groups.get(targetGroup).addAll(groups.get(group));
                groups.set(group, null);
            }
        }
    }

    private static int resolveGroup(int group, Map<Integer, Integer> groupMerges) {
        while (groupMerges.containsKey(group)) {
            group = groupMerges.get(group);
        }
        return group;
    }

    private static boolean isWordEmpty(String word) {
        return word.isEmpty() || word.equals("\"\"");
    }

    private static boolean isWordIncorrect(String word) {
        return word.matches("\"[0-9]*\"[0-9]*\"");
    }
}
