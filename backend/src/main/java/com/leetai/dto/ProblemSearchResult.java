package com.leetai.dto;

/**
 * What a search result looks like in the list view — deliberately minimal.
 * Full problem detail (description, starter code, test cases) is only
 * fetched when the user actually opens a problem, not while browsing.
 */
public class ProblemSearchResult {
    private String slug;
    private String name;
    private String difficulty;

    // Set after the ES/DB lookup, from the caller's own solved-slugs set —
    // not part of the search index itself.
    private boolean solved;

    public ProblemSearchResult() {}

    public ProblemSearchResult(String slug, String name, String difficulty) {
        this.slug = slug;
        this.name = name;
        this.difficulty = difficulty;
    }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }
}
