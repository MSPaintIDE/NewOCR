package com.uddernetworks.newocr.configuration;

/**
 * A simple supplier that throws {@link ReflectiveOperationException}.
 *
 * @param <T> The type to supply
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
@FunctionalInterface
public interface ReflexiveSupplier<T> {
    T get() throws ReflectiveOperationException;
}
