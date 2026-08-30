package com.leetai.dto;

import java.util.List;

/**
 * A deliberately plain, explicit pagination envelope. We used to return
 * Spring Data's Page<T> straight from the controller, but its JSON shape
 * isn't something we control — it has changed across Spring Data versions
 * (pagination metadata has moved between top-level fields and a nested
 * "page" object), which silently broke the frontend's totalElements count.
 * Returning this instead means the response shape only changes when we
 * change it.
 */
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PagedResponse() {}

    public PagedResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
