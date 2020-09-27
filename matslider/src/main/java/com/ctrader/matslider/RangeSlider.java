package com.ctrader.matslider;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.AbsSavedState;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ctrader.matslider.RangeSlider.OnChangeListener;
import com.ctrader.matslider.RangeSlider.OnSliderTouchListener;

import java.util.ArrayList;
import java.util.List;

public class RangeSlider extends BaseSlider<RangeSlider, OnChangeListener, OnSliderTouchListener> {

  private float minSeparation;
  private int separationUnit;

  public RangeSlider(@NonNull Context context) {
    this(context, null);
  }

  public RangeSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.sliderStyle);
  }

  public RangeSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    TypedArray a =
            context.obtainStyledAttributes(attrs, R.styleable.RangeSlider, defStyleAttr, DEF_STYLE_RES);
    if (a.hasValue(R.styleable.RangeSlider_values)) {
      int valuesId = a.getResourceId(R.styleable.RangeSlider_values, 0);
      TypedArray values = a.getResources().obtainTypedArray(valuesId);
      setValues(convertToFloat(values));
    }

    minSeparation = a.getDimension(R.styleable.RangeSlider_minSeparation, 0);
    a.recycle();
  }

  public interface OnChangeListener extends BaseOnChangeListener<RangeSlider> {}

  public interface OnSliderTouchListener extends BaseOnSliderTouchListener<RangeSlider> {}

  @Override
  public void setValues(@NonNull Float... values) {
    super.setValues(values);
  }

  @Override
  public void setValues(@NonNull List<Float> values) {
    super.setValues(values);
  }


  @NonNull
  @Override
  public List<Float> getValues() {
    return super.getValues();
  }

  private static List<Float> convertToFloat(TypedArray values) {
    List<Float> ret = new ArrayList<>();
    for (int i = 0; i < values.length(); ++i) {
      ret.add(values.getFloat(i, -1));
    }
    return ret;
  }

  @Override
  public float getMinSeparation() {
    return minSeparation;
  }

  public void setMinSeparation(@Dimension float minSeparation) {
    this.minSeparation = minSeparation;
    separationUnit = UNIT_PX;
    setSeparationUnit(separationUnit);
  }

  public void setMinSeparationValue(float minSeparation) {
    this.minSeparation = minSeparation;
    separationUnit = UNIT_VALUE;
    setSeparationUnit(separationUnit);
  }

  @Override
  @NonNull
  public Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();

    RangeSliderState sliderState = new RangeSliderState(superState);
    sliderState.minSeparation = this.minSeparation;
    sliderState.separationUnit = this.separationUnit;

    return sliderState;
  }

  @Override
  protected void onRestoreInstanceState(@Nullable Parcelable state) {
    RangeSliderState savedState = (RangeSliderState) state;
    super.onRestoreInstanceState(savedState.getSuperState());

    this.minSeparation = savedState.minSeparation;
    this.separationUnit = savedState.separationUnit;
    setSeparationUnit(separationUnit);
  }

  static class RangeSliderState extends AbsSavedState {

    private float minSeparation;
    private int separationUnit;

    RangeSliderState(Parcelable superState) {
      super(superState);
    }

    private RangeSliderState(Parcel in) {
      super((Parcelable) in.readParcelable(RangeSliderState.class.getClassLoader()));
      minSeparation = in.readFloat();
      separationUnit = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeFloat(minSeparation);
      out.writeInt(separationUnit);
    }

    public static final Parcelable.Creator<RangeSliderState> CREATOR =
        new Parcelable.Creator<RangeSliderState>() {
          @Override
          public RangeSliderState createFromParcel(Parcel in) {
            return new RangeSliderState(in);
          }

          @Override
          public RangeSliderState[] newArray(int size) {
            return new RangeSliderState[size];
          }
        };
  }
}
