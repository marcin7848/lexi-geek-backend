package io.learn.lexigeek.common.pageable;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageDto<T> {

    private int page;

    private int pageSize;

    private long total;

    private String sort;

    private OrderString order;

    private boolean singlePage;

    private List<T> items;
}
