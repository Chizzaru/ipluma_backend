package com.github.ws_ncip_pnpki.dto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
public class OffsetBasedPageRequest implements Pageable {

    private final int limit;
    private final int offset;
    private final Sort sort;

    public OffsetBasedPageRequest(int offset, int limit, Sort sort) {
        if (offset < 0) throw new IllegalArgumentException("Offset must be >= 0");
        if (limit < 1) throw new IllegalArgumentException("Limit must be >= 1");

        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }


    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetBasedPageRequest((int) getOffset() + getPageSize(), getPageSize(), getSort());
    }

    @Override
    public Pageable previousOrFirst() {
        int newOffset = (int) getOffset() - getPageSize();
        newOffset = Math.max(newOffset, 0);
        return new OffsetBasedPageRequest(newOffset, getPageSize(), getSort());
    }

    @Override
    public Pageable first() {
        return new OffsetBasedPageRequest(0, getPageSize(), getSort());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero!");
        }

        int newOffset = pageNumber * this.limit;
        return new OffsetBasedPageRequest(newOffset, this.limit, this.sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > limit;
    }
}
