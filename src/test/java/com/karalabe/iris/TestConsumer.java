package com.karalabe.iris;

@FunctionalInterface
public interface TestConsumer<T> {
    void accept(T t) throws Exception;
}