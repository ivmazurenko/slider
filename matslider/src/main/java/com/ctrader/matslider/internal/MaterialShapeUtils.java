package com.ctrader.matslider.internal;

import androidx.annotation.NonNull;

public class MaterialShapeUtils {

    private MaterialShapeUtils() {
    }

    @NonNull
    static CornerTreatment createCornerTreatment(@CornerFamily int cornerFamily) {
        switch (cornerFamily) {
            case CornerFamily.ROUNDED:
                return new RoundedCornerTreatment();
            case CornerFamily.CUT:
                return new CutCornerTreatment();
            default:
                return createDefaultCornerTreatment();
        }
    }

    @NonNull
    static CornerTreatment createDefaultCornerTreatment() {
        return new RoundedCornerTreatment();
    }

    @NonNull
    static EdgeTreatment createDefaultEdgeTreatment() {
        return new EdgeTreatment();
    }

}
