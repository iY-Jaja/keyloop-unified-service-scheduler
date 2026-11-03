package com.keyloop.scheduler.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public class PageResponse<T> {

    public final List<T> content;
    public final int page;
    public final int size;
    public final long totalElements;
    public final int totalPages;
    public final boolean first;
    public final boolean last;

    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
}
