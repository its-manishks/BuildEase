package com.examly.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.feedback.ImproveTextRequest;
import com.examly.springapp.dto.feedback.ImproveTextResponse;
import com.examly.springapp.dto.feedback.SpellCheckRequest;
import com.examly.springapp.dto.feedback.SpellCheckResponse;
import com.examly.springapp.service.ai.WritingAssistService;
import com.examly.springapp.service.spelling.SpellCheckService;

import jakarta.validation.Valid;

/**
 * Writing help for the review form (User role). Purely advisory: the review is only saved by the
 * existing POST /api/feedback, with all its eligibility and duplicate rules unchanged.
 */
@RestController
@RequestMapping("/api/feedback")
public class WritingAssistController {

    private final SpellCheckService spellCheckService;
    private final WritingAssistService writingAssistService;

    public WritingAssistController(SpellCheckService spellCheckService, WritingAssistService writingAssistService) {
        this.spellCheckService = spellCheckService;
        this.writingAssistService = writingAssistService;
    }

    // Returns likely misspellings with suggestions (local dictionary; the text is not stored or logged).
    @PostMapping("/spellcheck")
    public ResponseEntity<SpellCheckResponse> spellcheck(@Valid @RequestBody SpellCheckRequest request) {
        return ResponseEntity.ok(new SpellCheckResponse(spellCheckService.check(request.text()).stream()
                .map(i -> new SpellCheckResponse.Issue(i.word(), i.start(), i.end(), i.suggestions()))
                .toList()));
    }

    // Suggests a clearer wording of the review (Gemini, on demand); 503 AI_UNAVAILABLE when switched off.
    @PostMapping("/improve")
    public ResponseEntity<ImproveTextResponse> improve(@Valid @RequestBody ImproveTextRequest request) {
        return ResponseEntity.ok(new ImproveTextResponse(
                writingAssistService.improve(SecurityUtils.getCurrentUserId(), request.text())));
    }
}