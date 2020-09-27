/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctrader.matslider.internal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;

public class MaterialResources {
    private MaterialResources() {
    }

    @Nullable
    public static ColorStateList getColorStateList(
            @NonNull Context context, @NonNull TypedArray attributes, @StyleableRes int index) {
        if (attributes.hasValue(index)) {
            int resourceId = attributes.getResourceId(index, 0);
            if (resourceId != 0) {
                ColorStateList value = AppCompatResources.getColorStateList(context, resourceId);
                if (value != null) {
                    return value;
                }
            }
        }

        return attributes.getColorStateList(index);
    }
}
