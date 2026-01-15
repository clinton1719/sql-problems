import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class GenerateTagsIndex {
  static final Path PROBLEMS_DIR = Paths.get("problems");
  static final Path TAGS_DIR = Paths.get("tags");
  static final Path MAIN_README = Paths.get("README.md");

  static final Pattern FRONTMATTER_PATTERN = Pattern.compile("(?s)^---\\s*\\n(.*?)\\n---");
  static final Pattern TAGS_PATTERN = Pattern.compile("tags:\\s*\\[(.*?)\\]");
  static final Pattern TITLE_PATTERN = Pattern.compile("title:\\s*\"?(.*?)\"?(?:\\n|$)");
  static final Pattern ID_PATTERN = Pattern.compile("id:\\s*(\\d+)");
  static final Pattern DIFFICULTY_PATTERN = Pattern.compile("difficulty:\\s*(.*?)(?:\\n|$)");

  static class ProblemInfo {
    String id;
    String title;
    String difficulty;
    String relativePath;
    List<String> tags;

    ProblemInfo(String id, String title, String difficulty, String path, List<String> tags) {
      this.id = id;
      this.title = title;
      this.difficulty = difficulty;
      this.relativePath = path.replace("\\", "/");
      this.tags = tags;
    }

    // Helper to get a color emoji based on difficulty
    String getDifficultyIcon() {
      return switch (difficulty.toLowerCase()) {
        case "easy" -> "🟢";
        case "medium" -> "🟡";
        case "hard" -> "🔴";
        default -> "⚪";
      };
    }
  }

  static void main() throws IOException {
    if (!Files.exists(TAGS_DIR)) Files.createDirectories(TAGS_DIR);

    Map<String, List<ProblemInfo>> tagMap = new TreeMap<>();
    List<ProblemInfo> allProblems = new ArrayList<>();
    int easyCount = 0, mediumCount = 0, hardCount = 0;

    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(PROBLEMS_DIR)) {
      for (Path problemDir : dirStream) {
        if (!Files.isDirectory(problemDir)) continue;

        Path markdownFile = problemDir.resolve("index.md");
        if (!Files.exists(markdownFile)) {
          markdownFile = problemDir.resolve("README.md");
          if (!Files.exists(markdownFile)) continue;
        }

        String content = Files.readString(markdownFile);
        Matcher fmMatcher = FRONTMATTER_PATTERN.matcher(content);
        if (!fmMatcher.find()) continue;

        String fm = fmMatcher.group(1);
        String id = getMatch(ID_PATTERN, fm);
        String title = getMatch(TITLE_PATTERN, fm);
        String diff = getMatch(DIFFICULTY_PATTERN, fm);
        String rawTags = getMatch(TAGS_PATTERN, fm);

        if (id == null || title == null) continue;

        // Update Difficulty stats
        if ("Easy".equalsIgnoreCase(diff)) easyCount++;
        else if ("Medium".equalsIgnoreCase(diff)) mediumCount++;
        else if ("Hard".equalsIgnoreCase(diff)) hardCount++;

        List<String> problemTags = new ArrayList<>();
        for (String t : rawTags.split(",")) {
          String cleanTag = t.replace("\"", "").replace("'", "").trim();
          if (!cleanTag.isEmpty()) {
            problemTags.add(cleanTag);
            tagMap.putIfAbsent(cleanTag, new ArrayList<>());
          }
        }

        ProblemInfo info =
            new ProblemInfo(id, title, diff, "problems/" + problemDir.getFileName(), problemTags);
        allProblems.add(info);

        for (String tag : problemTags) {
          tagMap.get(tag).add(info);
        }
      }
    }

    // 1. Generate Tag Files with <kbd> labels
    for (Map.Entry<String, List<ProblemInfo>> entry : tagMap.entrySet()) {
      String tag = entry.getKey();
      List<ProblemInfo> problems = entry.getValue();
      problems.sort(Comparator.comparingInt(p -> Integer.parseInt(p.id)));

      try (BufferedWriter writer = Files.newBufferedWriter(TAGS_DIR.resolve(tag + ".md"))) {
        writer.write("# 🚩 Tag: " + tag + "\n\n");
        for (ProblemInfo p : problems) {
          writer.write(
              String.format(
                  "- [%s. %s](../%s) <kbd>%s</kbd>\n",
                  p.id, p.title, p.relativePath, p.difficulty));
        }
      }
    }

    // 2. Generate Main README.md with Dashboard
    allProblems.sort(Comparator.comparingInt(p -> Integer.parseInt(p.id)));
    try (BufferedWriter writer = Files.newBufferedWriter(MAIN_README)) {
      writer.write("# 🚀 LeetCode Solutions\n\n");

      writer.write("## 📊 Statistics\n\n");
      writer.write("| Total | 🟢 Easy | 🟡 Medium | 🔴 Hard |\n");
      writer.write("| --- | --- | --- | --- |\n");
      writer.write(
          String.format(
              "| %d | %d | %d | %d |\n\n", allProblems.size(), easyCount, mediumCount, hardCount));

      writer.write("## 🏷️ Tag Cloud\n\n");
      for (String tag : tagMap.keySet()) {
        writer.write(String.format("[`%s`](tags/%s.md) ", tag, tag));
      }
      writer.write("\n\n---\n\n");

      writer.write("## 📚 Problem List\n\n");
      writer.write("| # | Title | Difficulty | Tags |\n");
      writer.write("| --- | --- | --- | --- |\n");
      for (ProblemInfo p : allProblems) {
        // Create links for tags in the table too
        List<String> tagLinks = new ArrayList<>();
        for (String t : p.tags) tagLinks.add(String.format("[%s](tags/%s.md)", t, t));

        writer.write(
            String.format(
                "| %s | [%s](%s) | %s %s | %s |\n",
                p.id,
                p.title,
                p.relativePath,
                p.getDifficultyIcon(),
                p.difficulty,
                String.join(", ", tagLinks)));
      }
    }

    System.out.println("✅ Generated Dashboard and " + tagMap.size() + " tag files.");
  }

  private static String getMatch(Pattern p, String text) {
    Matcher m = p.matcher(text);
    return m.find() ? m.group(1).trim() : "";
  }
}
