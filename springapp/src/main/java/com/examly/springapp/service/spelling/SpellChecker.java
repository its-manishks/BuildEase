package com.examly.springapp.service.spelling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dictionary-based spelling suggestions (pure Java: no network, no AI, nothing leaves the server).
 * Unknown words get candidates within edit distance 1 (then 2 for words of 4-10 letters), ranked by:
 * same first letter, a doubled/undoubled letter ("wil" -> will, "wett" -> wet), contains the typed letters
 * in order (most typos drop a letter: "brokn" -> broken),
 * similar length, longer common prefix. Proper names count as known words but are never suggested.
 * It only suggests; it never changes the user's text.
 */
public final class SpellChecker {

    /** A likely misspelling at [start, end) of the text, with up to 3 suggestions. */
    public record Issue(String word, int start, int end, List<String> suggestions) {
    }

    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)*");
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final int MAX_SUGGESTIONS = 3;

    private final Set<String> dictionary;
    private final Set<String> properNames;

    /** dictionary: common words (lower case, used for suggestions); properNames: lower-cased names (known only). */
    public SpellChecker(Set<String> dictionary, Set<String> properNames) {
        this.dictionary = dictionary;
        this.properNames = properNames;
    }

    public List<Issue> check(String text, Set<String> extraWords, int maxIssues) {
        List<Issue> issues = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return issues;
        }
        Matcher matcher = WORD.matcher(text);
        while (matcher.find() && issues.size() < maxIssues) {
            String word = matcher.group();
            if (shouldSkip(word, text, matcher.start())) {
                continue;
            }
            String lower = word.toLowerCase(Locale.ROOT);
            if (isKnown(lower, extraWords)) {
                continue;
            }
            List<String> suggestions = suggest(lower, extraWords);
            if (!suggestions.isEmpty()) {
                boolean capitalised = Character.isUpperCase(word.charAt(0));
                issues.add(new Issue(word, matcher.start(), matcher.end(), suggestions.stream()
                        .map(s -> capitalised ? Character.toUpperCase(s.charAt(0)) + s.substring(1) : s).toList()));
            }
        }
        return issues;
    }

    public boolean isKnown(String lower, Set<String> extraWords) {
        if (dictionary.contains(lower) || properNames.contains(lower) || extraWords.contains(lower)) {
            return true;
        }
        return lower.endsWith("'s") && dictionary.contains(lower.substring(0, lower.length() - 2));
    }

    /** Short words, acronyms, CamelCase and capitalised names in mid-sentence are not checked. */
    private static boolean shouldSkip(String word, String text, int start) {
        if (word.length() < 3) {
            return true;
        }
        for (int i = 1; i < word.length(); i++) {
            if (Character.isUpperCase(word.charAt(i))) {
                return true;
            }
        }
        return Character.isUpperCase(word.charAt(0)) && !isSentenceStart(text, start);
    }

    private static boolean isSentenceStart(String text, int start) {
        for (int i = start - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == '"' || c == '(') {
                continue;
            }
            return c == '.' || c == '!' || c == '?' || c == '\n';
        }
        return true;
    }

    List<String> suggest(String lower, Set<String> extraWords) {
        Set<String> first = edits(lower);
        Set<String> found = new HashSet<>();
        for (String candidate : first) {
            if (isSuggestable(candidate, extraWords)) {
                found.add(candidate);
            }
        }
        int distance = 1;
        if (found.isEmpty() && lower.length() >= 4 && lower.length() <= 10) {
            distance = 2;
            for (String one : first) {
                for (String two : edits(one)) {
                    if (isSuggestable(two, extraWords)) {
                        found.add(two);
                    }
                }
            }
        }
        final int d = distance;
        return found.stream()
                .sorted(Comparator.comparingInt((String s) -> s.charAt(0) == lower.charAt(0) ? 0 : 1)
                        .thenComparingInt(s -> isDoubledLetterSlip(lower, s) ? 0 : 1)
                        .thenComparingInt(s -> isSubsequence(lower, s) ? 0 : 1)
                        .thenComparingInt(s -> Math.abs(s.length() - lower.length()) + d)
                        .thenComparing(s -> -commonPrefix(s, lower))
                        .thenComparing(Comparator.naturalOrder()))
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    /** All strings one edit away: deletion, transposition, replacement, insertion. */
    private static Set<String> edits(String word) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i <= word.length(); i++) {
            String left = word.substring(0, i);
            String right = word.substring(i);
            if (!right.isEmpty()) {
                result.add(left + right.substring(1));
            }
            if (right.length() > 1) {
                result.add(left + right.charAt(1) + right.charAt(0) + right.substring(2));
            }
            for (int c = 0; c < LETTERS.length(); c++) {
                char letter = LETTERS.charAt(c);
                if (!right.isEmpty()) {
                    result.add(left + letter + right.substring(1));
                }
                result.add(left + letter + right);
            }
        }
        result.remove(word);
        return result;
    }

    /** Common words and catalogue terms can be suggested; proper names cannot. */
    private boolean isSuggestable(String word, Set<String> extraWords) {
        return dictionary.contains(word) || extraWords.contains(word);
    }

    /** True when the two words differ only by one doubled letter ("wil"/"will", "wett"/"wet"). */
    private static boolean isDoubledLetterSlip(String typed, String candidate) {
        String longer = typed.length() > candidate.length() ? typed : candidate;
        String shorter = longer == typed ? candidate : typed;
        if (longer.length() != shorter.length() + 1) {
            return false;
        }
        for (int i = 0; i < longer.length() - 1; i++) {
            if (longer.charAt(i) == longer.charAt(i + 1)
                    && (longer.substring(0, i) + longer.substring(i + 1)).equals(shorter)) {
                return true;
            }
        }
        return false;
    }

    /** True when every letter of typed appears in candidate in the same order. */
    private static boolean isSubsequence(String typed, String candidate) {
        int i = 0;
        for (int j = 0; j < candidate.length() && i < typed.length(); j++) {
            if (typed.charAt(i) == candidate.charAt(j)) {
                i++;
            }
        }
        return i == typed.length();
    }

    private static int commonPrefix(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }
}