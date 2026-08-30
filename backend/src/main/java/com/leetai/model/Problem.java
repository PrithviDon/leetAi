package com.leetai.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String slug;          // "two-sum" — auto-generated from name

    private String name;          // "Two Sum"

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;   // markdown/plain text problem statement

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String starterCodeJs;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String starterCodePython;

    // The function name the user's code must expose, used to build the harness.
    private String functionName;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TestCase> testCases = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    private String createdBy;   // admin's email
    private String updatedBy;   // admin's email
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public enum Difficulty { EASY, MEDIUM, HARD }
    public enum Status { DRAFT, PUBLISHED }

    // --- getters/setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStarterCodeJs() { return starterCodeJs; }
    public void setStarterCodeJs(String starterCodeJs) { this.starterCodeJs = starterCodeJs; }
    public String getStarterCodePython() { return starterCodePython; }
    public void setStarterCodePython(String starterCodePython) { this.starterCodePython = starterCodePython; }
    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }
    public List<TestCase> getTestCases() { return testCases; }
    public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
