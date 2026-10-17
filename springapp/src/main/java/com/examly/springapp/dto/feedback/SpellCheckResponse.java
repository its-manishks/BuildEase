package com.examly.springapp.dto.feedback;

import java.util.List;

/** Likely misspellings: the word, its position [start, end) in the text, and up to 3 suggestions. */
public record SpellCheckResponse(List<Issue> issues) {

    public record Issue(String word, int start, int end, List<String> suggestions) {
    }
}