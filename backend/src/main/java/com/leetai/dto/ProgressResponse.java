package com.leetai.dto;

public class ProgressResponse {
    private long solved;
    private long total;

    public ProgressResponse() {}

    public ProgressResponse(long solved, long total) {
        this.solved = solved;
        this.total = total;
    }

    public long getSolved() { return solved; }
    public void setSolved(long solved) { this.solved = solved; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
