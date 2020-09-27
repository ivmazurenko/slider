package com.ctrader.matslider;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo.Scope;

public interface BaseOnChangeListener<S> {

  void onValueChange(@NonNull S slider, float value, boolean fromUser);
}
