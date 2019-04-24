package com.uddernetworks.newocr.configuration;

/**
 * A simple supplier that throws {@link ReflectiveOperationException}.
 * @param <T> The type to supply
 */
@FunctionalInterface
public interface ReflexiveSupplier<T> {
    T get() throws ReflectiveOperationException;
}
