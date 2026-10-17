package com.examly.springapp.service.spelling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.examly.springapp.model.Material;
import com.examly.springapp.repository.MaterialRepo;

/**
 * Spelling suggestions for the feedback form. Uses the bundled SCOWL-based dictionary plus the
 * names of materials in the catalogue (so product names are not flagged). Runs entirely on the
 * server: the text is not sent anywhere and is not logged. If the dictionary cannot be loaded,
 * it returns no suggestions and feedback submission is unaffected.
 */
@Service
public class SpellCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpellCheckService.class);
    private static final String DICTIONARY = "spelling/en_US_words.txt";
    private static final int MAX_ISSUES = 20;
    private static final long CATALOGUE_REFRESH_MS = 10 * 60_000L;
    /** Building-trade words a general dictionary may miss. */
    private static final Set<String> TRADE_WORDS = Set.of("plywood", "rebar", "rebars", "grout", "putty", "primer",
            "tmt", "opc", "ppc", "cpvc", "upvc", "pvc", "gi", "mdf", "hdf", "sanitaryware", "aggregates",
            "waterproofing", "underlay", "laminate", "laminates", "screed", "plaster", "plastering", "tiling");

    private final MaterialRepo materialRepo;
    private volatile SpellChecker checker;
    private volatile Set<String> catalogueWords = Set.of();
    private volatile long catalogueLoadedAt;

    public SpellCheckService(MaterialRepo materialRepo) {
        this.materialRepo = materialRepo;
    }

    public List<SpellChecker.Issue> check(String text) {
        SpellChecker spellChecker = checker();
        if (spellChecker == null) {
            return List.of();
        }
        return spellChecker.check(text, extraWords(), MAX_ISSUES);
    }

    private SpellChecker checker() {
        if (checker == null) {
            synchronized (this) {
                if (checker == null) {
                    checker = loadDictionary();
                }
            }
        }
        return checker;
    }

    private SpellChecker loadDictionary() {
        Set<String> words = new HashSet<>(170_000);
        Set<String> names = new HashSet<>(30_000);
        try (InputStream in = new ClassPathResource(DICTIONARY).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (Character.isUpperCase(line.charAt(0))) {
                    names.add(line.toLowerCase(Locale.ROOT));
                } else {
                    words.add(line);
                }
            }
            LOGGER.info("Spelling dictionary loaded: {} words, {} names", words.size(), names.size());
            return new SpellChecker(words, names);
        } catch (IOException ex) {
            LOGGER.warn("Spelling dictionary could not be loaded; spelling suggestions are disabled");
            // an empty dictionary yields no suggestions, so the form simply shows none
            return new SpellChecker(Set.of(), Set.of());
        }
    }

    /** Trade words + every word of the current material names (refreshed every 10 minutes). */
    private Set<String> extraWords() {
        long now = System.currentTimeMillis();
        if (now - catalogueLoadedAt > CATALOGUE_REFRESH_MS) {
            Set<String> fresh = new HashSet<>(TRADE_WORDS);
            for (Material material : materialRepo.findAll()) {
                if (material.getMaterialName() == null) {
                    continue;
                }
                for (String token : material.getMaterialName().toLowerCase(Locale.ROOT).split("[^a-z']+")) {
                    if (token.length() >= 2) {
                        fresh.add(token);
                    }
                }
            }
            catalogueWords = fresh;
            catalogueLoadedAt = now;
        }
        return catalogueWords;
    }
}