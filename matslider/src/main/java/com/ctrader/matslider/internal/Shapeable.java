package com.ctrader.matslider.internal;

import androidx.annotation.NonNull;

public interface Shapeable {

    void setShapeAppearanceModel(@NonNull ShapeAppearanceModel shapeAppearanceModel);

    @NonNull
    ShapeAppearanceModel getShapeAppearanceModel();
}
