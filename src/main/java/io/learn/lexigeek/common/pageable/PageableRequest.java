package io.learn.lexigeek.common.pageable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Getter
@Setter
public class PageableRequest {

    @Min(1)
    private Integer page;

    @Min(1)
    private Integer pageSize;

    private String sort;

    private OrderString order;

    private boolean singlePage;

    public PageableRequest(final Integer page, final Integer pageSize, final String sort, final OrderString order, final Boolean singlePage) {
        this.page = nonNull(page) ? page : 1;
        this.pageSize = nonNull(pageSize) ? pageSize : PageableUtils.DEFAULT_PAGE_SIZE;
        this.sort = sort;
        final Sort.Direction direction = order == OrderString.desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        this.sortBy = new ArrayList<>();
        this.singlePage = singlePage != null && singlePage;

        if (nonNull(sort)) {
            this.sortBy.add(new SortOrder(this.sort, direction));
        }
    }

    @Builder
    private static PageableRequest create(final Integer page, final Integer pageSize, final String sort, final OrderString order, final Boolean singlePage) {
        return new PageableRequest(page, pageSize, sort, order, singlePage);
    }

    @Valid
    private List<SortOrder> sortBy;

    public void addDefaultSorts(final SortOrder... sorts) {
        if (sortBy.isEmpty()) {
            sortBy.addAll(List.of(sorts));
        }
    }

    public SortOrder getFirstSort() {
        if (sortBy.isEmpty()) {
            return new SortOrder(null, null);
        } else {
            return sortBy.getFirst();
        }
    }
}

