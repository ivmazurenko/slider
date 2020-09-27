package com.ctrader.matslider.internal;

import android.graphics.RectF;

import androidx.annotation.NonNull;

public class RoundedCornerTreatment {

    float radius = -1;

    public RoundedCornerTreatment() {
    }

    public void getCornerPath(
            @NonNull ShapePath shapePath,
            float angle,
            float interpolation,
            @NonNull RectF bounds,
            @NonNull CornerSize size) {
        getCornerPath(shapePath, angle, interpolation, size.getCornerSize(bounds));
    }
    
    public void getCornerPath(
            @NonNull ShapePath shapePath, float angle, float interpolation, float radius) {
        shapePath.reset(0, radius * interpolation, ShapePath.ANGLE_LEFT, 180 - angle);
        shapePath.addArc(0, 0, 2 * radius * interpolation, 2 * radius * interpolation, 180, angle);
    }
}
