package moon.example.analyzer;

import java.util.Collection;

public final class NameSuggester {
  private static final int MAX_DISTANCE = 2;

  private NameSuggester() {
  }

  public static String suggest(String unknownName, Collection<String> knownNames) {
    String bestName = null;
    int bestDistance = MAX_DISTANCE + 1;

    for (String knownName : knownNames) {
      int distance = levenshteinDistance(unknownName, knownName);

      if (distance < bestDistance) {
        bestDistance = distance;
        bestName = knownName;
      }
    }

    return bestName;
  }

  public static String suggestionHint(String unknownName, Collection<String> knownNames) {
    String suggestion = suggest(unknownName, knownNames);

    return (suggestion == null) ? null : "did you mean '" + suggestion + "'?";
  }

  static int levenshteinDistance(String first, String second) {
    int[] previousRow = new int[second.length() + 1];
    int[] currentRow = new int[second.length() + 1];

    for (int j = 0; j <= second.length(); j++) {
      previousRow[j] = j;
    }

    for (int i = 1; i <= first.length(); i++) {
      currentRow[0] = i;

      for (int j = 1; j <= second.length(); j++) {
        int substitutionCost = (first.charAt(i - 1) == second.charAt(j - 1)) ? 0 : 1;

        currentRow[j] = Math.min(
                Math.min(currentRow[j - 1] + 1, previousRow[j] + 1),
                previousRow[j - 1] + substitutionCost);
      }

      int[] swap = previousRow;
      previousRow = currentRow;
      currentRow = swap;
    }

    return previousRow[second.length()];
  }
}
