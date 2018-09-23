package com.uddernetworks.newocr.altsearcher.feature;

public enum FeatureType {
    VERTICAL_LINE(VerticalLineFeature.class),
//    HORIZONTAL_LINE(HorizontalLineFeature.class),
//    CURVE(CurveFeature.class)
    ;

    private Class<? extends Feature> featureClass;

    FeatureType(Class<? extends Feature> featureClass) {
        this.featureClass = featureClass;
    }

    public Class<? extends Feature> getFeatureClass() {
        return this.featureClass;
    }

    public Feature getFeature() throws IllegalAccessException, InstantiationException {
        return this.featureClass.newInstance();
    }
}
