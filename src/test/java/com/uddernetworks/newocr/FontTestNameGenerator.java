package com.uddernetworks.newocr;

import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;

public class FontTestNameGenerator implements DisplayNameGenerator {

    @Override
    public String generateDisplayNameForClass(Class<?> testClass) {
        var className = testClass.getSimpleName();
        if (!className.startsWith("Font")) return className;
        return className.substring(4);
    }

    @Override
    public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
        return generateDisplayNameForClass(nestedClass);
    }

    @Override
    public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
        var className = testClass.getSimpleName();
        var methodName = testMethod.getName();
        var defaultName = className + "#" + methodName;
        if (!className.startsWith("Font")) return defaultName;

        if (methodName.equals("accuracyTest")) {
            return className.substring(4) + " Accuracy";
        }

        return defaultName;
    }
}
