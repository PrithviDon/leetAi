package com.leetai.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "problem_id")
    @JsonBackReference
    private Problem problem;

    // JSON-encoded array of args, e.g. "[[2,7,11,15], 9]"
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String inputJson;

    // JSON-encoded expected output, e.g. "[0,1]"
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String expectedOutputJson;

    // Hidden test cases are used for grading but never shown to the user.
    private boolean hidden;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }
    public String getInputJson() { return inputJson; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }
    public String getExpectedOutputJson() { return expectedOutputJson; }
    public void setExpectedOutputJson(String expectedOutputJson) { this.expectedOutputJson = expectedOutputJson; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
