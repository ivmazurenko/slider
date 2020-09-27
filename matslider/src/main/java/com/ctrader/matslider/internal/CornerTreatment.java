/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctrader.matslider.internal;

import android.graphics.RectF;

import androidx.annotation.NonNull;

public class CornerTreatment {

    @Deprecated
    public void getCornerPath(float angle, float interpolation, @NonNull ShapePath shapePath) {
    }

    public void getCornerPath(@NonNull ShapePath shapePath, float angle, float interpolation, float radius) {
        getCornerPath(angle, interpolation, shapePath);
    }

    public void getCornerPath(
            @NonNull ShapePath shapePath,
            float angle,
            float interpolation,
            @NonNull RectF bounds,
            @NonNull CornerSize size) {
        getCornerPath(shapePath, angle, interpolation, size.getCornerSize(bounds));
    }
}
