package io.learn.lexigeek.common.dto;

public interface RangeForm<T extends Comparable<? super T>> {

    T min();

    T max();
}
