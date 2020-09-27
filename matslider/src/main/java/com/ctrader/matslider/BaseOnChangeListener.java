package com.ctrader.matslider;

import androidx.annotation.NonNull;

public interface BaseOnChangeListener<S> {

  void onValueChange(@NonNull S slider, float value, boolean fromUser);
}
