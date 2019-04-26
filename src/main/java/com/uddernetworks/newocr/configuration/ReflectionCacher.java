package com.uddernetworks.newocr.configuration;

import java.lang.reflect.Constructor;
import java.util.Optional;

/**
 * A simple class to cache constructors from classes.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public interface ReflectionCacher {

    /**
     * Gets a constructor from the internal cache from the given class, or generate one via constructorGenerator.
     * The generation of one from constructorGenerator then saves it in the local cache.
     *
     * @param clazz                The class to generate or fetch the constructor from
     * @param constructorGenerator The supplier to make the constructor if one is not found
     * @param <T>                  The class type
     * @return The constructor, if found
     */
    <T> Optional<Constructor<T>> getOrLookupConstructor(Class<T> clazz, ReflexiveSupplier<Constructor<T>> constructorGenerator);

}
