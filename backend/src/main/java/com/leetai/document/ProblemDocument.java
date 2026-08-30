package com.leetai.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * What Elasticsearch stores and searches — deliberately lighter than the
 * Problem JPA entity. MySQL is the source of truth for all fields;
 * this is just the search projection.
 *
 * Two field types in use:
 *  - TEXT: analysed (tokenised, lowercased, stemmed) — used for free-text
 *    search on name and description (multi_match queries).
 *  - KEYWORD: not analysed — used for exact-match filters on difficulty
 *    and status (term queries). You can't do an exact filter on a TEXT
 *    field; you need KEYWORD for that.
 */
@Document(indexName = "problems")
@Setting(settingPath = "elasticsearch/problem-settings.json")
public class ProblemDocument {

    @Id
    private String id; // maps to Problem.id (as String — ES uses String IDs)

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String difficulty; // EASY | MEDIUM | HARD

    @Field(type = FieldType.Keyword)
    private String status; // DRAFT | PUBLISHED

    @Field(type = FieldType.Keyword)
    private String slug;

    public ProblemDocument() {}

    public ProblemDocument(String id, String name, String description,
                            String difficulty, String status, String slug) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.status = status;
        this.slug = slug;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
