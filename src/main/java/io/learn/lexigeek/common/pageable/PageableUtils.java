package io.learn.lexigeek.common.pageable;

import io.learn.lexigeek.common.exception.PageableRequestTooLargeException;
import io.learn.lexigeek.common.validation.ErrorCodes;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.data.domain.Sort.Direction.ASC;

@UtilityClass
@Slf4j
public class PageableUtils {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int FULL_PAGE_SIZE = 999999999;

    private static final PageRequest DEFAULT_PAGE_REQUEST = PageRequest.of(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);

    public Pageable createPageable(final PageableRequest pageableRequest) {
        return createPageable(pageableRequest, new HashMap<>(), true);
    }

    public Pageable createPageable(final PageableRequest pageableRequest,
                                   final Map<String, String> customMapping,
                                   final boolean ignoreCase) {
        if (isNull(pageableRequest)) {
            return DEFAULT_PAGE_REQUEST;
        }

        final Pageable pageable = getPageable(pageableRequest);

        if ((long) pageable.getPageNumber() * pageable.getPageSize() > Integer.MAX_VALUE) {
            throw new PageableRequestTooLargeException(ErrorCodes.PAGEABLE_REQUEST_TOO_LARGE);
        }

        if (hasSortDirectionWithoutSortingParameter(pageableRequest)) {
            return pageable;
        }

        final List<Sort.Order> sortOrders = pageableRequest.getSortBy().stream()
                .map(order -> createOrder(customMapping, order, ignoreCase))
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sortOrders));
    }

    public <T> PageDto<T> toDto(final Page<T> page, final PageableRequest pageableRequest) {
        final SortOrder sortOrder = pageableRequest.getFirstSort();
        final PageDto<T> dto = new PageDto<>();
        dto.setItems(page.getContent());
        dto.setPage(zeroIndexedToOneIndexed(page.getNumber()));
        dto.setPageSize(pageableRequest.isSinglePage() ? (int) page.getTotalElements() : page.getSize());
        dto.setTotal(page.getTotalElements());
        dto.setSort(sortOrder.getField());
        dto.setOrder(sortOrder.getDirection() == Sort.Direction.DESC ? OrderString.desc : OrderString.asc);
        dto.setSinglePage(pageableRequest.isSinglePage());
        return dto;
    }

    public <T> PageDto<T> listToPageDto(final List<T> list, final PageableRequest pageableRequest, final long total,
                                        final Comparator<T> comparator) {
        final Pageable pageable = getPageable(pageableRequest);
        List<T> modifiableList = sort(list, comparator, pageableRequest);
        final int start = Math.min((int) pageable.getOffset(), (int) total);
        final int end = Math.min((start + pageable.getPageSize()), (int) total);
        final Page<T> page = new PageImpl<>(modifiableList.subList(start, end), pageable, total);
        return toDto(page, pageableRequest);
    }

    public <T> PageDto<T> pagedListToPageDto(final List<T> list, final PageableRequest pageableRequest, final long total) {
        final Pageable pageable = getPageable(pageableRequest);
        final Page<T> page = new PageImpl<>(list, pageable, total);
        return toDto(page, pageableRequest);
    }

    public <T> Comparator<T> getComparator(final Class<T> clazz, final PageableRequest pageableRequest) {
        final Comparator<T> defaultComparator = (o1, o2) -> 0;
        return getComparator(clazz, pageableRequest, defaultComparator);
    }

    @SuppressWarnings({"unchecked", "java:S3011"})
    public <T> Comparator<T> getComparator(final Class<T> clazz, final PageableRequest pageableRequest, final Comparator<T> defaultComparator) {
        if (pageableRequest == null || pageableRequest.getSortBy() == null || pageableRequest.getSortBy().isEmpty()) {
            return defaultComparator;
        }

        return (o1, o2) -> {
            for (final SortOrder sortOrder : pageableRequest.getSortBy()) {
                try {
                    final Field field = clazz.getDeclaredField(sortOrder.getField());
                    field.setAccessible(true);

                    final Object val1 = field.get(o1);
                    final Object val2 = field.get(o2);

                    final Comparable<Object> comp1 = (Comparable<Object>) val1;
                    final Comparable<Object> comp2 = (Comparable<Object>) val2;

                    return comp1.compareTo(comp2);
                } catch (final Exception e) {
                    log.warn("There were a problem with comparing objects using pageable", e);
                    return 0;
                }
            }

            return 0;
        };
    }

    private Pageable getPageable(final PageableRequest pageableRequest) {
        if (pageableRequest.isSinglePage()) {
            return PageRequest.of(0, FULL_PAGE_SIZE);
        }

        final int pageNumber = nonNull(pageableRequest.getPage())
                ? oneIndexedToZeroIndexed(pageableRequest.getPage())
                : DEFAULT_PAGE_NUMBER;
        final int pageSize = nonNull(pageableRequest.getPageSize())
                ? pageableRequest.getPageSize()
                : DEFAULT_PAGE_SIZE;
        return PageRequest.of(pageNumber, pageSize);
    }

    private int zeroIndexedToOneIndexed(final int pageNumber) {
        return pageNumber + 1;
    }

    private static Sort.Order createOrder(final Map<String, String> customMapping, final SortOrder order, final boolean ignoreCase) {
        final Sort.Order sortOrder = new Sort.Order(order.getDirection(), customMapping.getOrDefault(order.getField(), order.getField()));
        return ignoreCase ? sortOrder.ignoreCase() : sortOrder;
    }

    private int oneIndexedToZeroIndexed(final int pageNumber) {
        return pageNumber - 1;
    }

    private static boolean hasSortDirectionWithoutSortingParameter(final PageableRequest pageableRequest) {
        return pageableRequest.getSortBy().stream()
                .anyMatch(sortOrder -> isNull(sortOrder.getField()) && nonNull(sortOrder.getDirection()));
    }

    private static <T> List<T> sort(final List<T> list, final Comparator<T> comparator, final PageableRequest pageableRequest) {
        if (list.size() < 2) {
            return list;
        }

        List<T> modifiableList = new ArrayList<>(list);

        if (pageableRequest.getFirstSort().getDirection() == ASC) {
            modifiableList.sort(comparator);
        } else {
            modifiableList.sort(comparator.reversed());
        }

        return modifiableList;
    }
}
