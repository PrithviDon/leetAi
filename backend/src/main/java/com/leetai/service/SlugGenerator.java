package com.leetai.service;

import com.leetai.repository.ProblemRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Sole responsibility: turn a problem name into a unique, URL-safe slug.
 */
@Component
public class SlugGenerator {

    private final ProblemRepository problemRepository;

    public SlugGenerator(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    public String generate(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        String candidate = base;
        int suffix = 2;
        while (problemRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
