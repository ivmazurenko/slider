package com.ctrader.matslider;

import androidx.annotation.NonNull;

public interface BaseOnSliderTouchListener<S> {
  void onStartTrackingTouch(@NonNull S slider);

  void onStopTrackingTouch(@NonNull S slider);
}
