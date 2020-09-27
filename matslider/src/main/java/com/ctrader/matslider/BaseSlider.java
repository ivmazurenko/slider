package com.ctrader.matslider;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.SeekBar;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.ctrader.matslider.internal.MaterialResources;
import com.ctrader.matslider.internal.MaterialShapeDrawable;
import com.ctrader.matslider.internal.ShapeAppearanceModel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static androidx.core.math.MathUtils.clamp;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT;
import static java.lang.Float.compare;
import static java.lang.Math.abs;
import static java.lang.Math.max;

abstract class BaseSlider<
        S extends BaseSlider<S, L, T>,
        L extends BaseOnChangeListener<S>,
        T extends BaseOnSliderTouchListener<S>>
        extends View {

    private static final String TAG = BaseSlider.class.getSimpleName();
    private static final String EXCEPTION_ILLEGAL_VALUE =
            "Slider value(%s) must be greater or equal to valueFrom(%s), and lower or equal to"
                    + " valueTo(%s)";
    private static final String EXCEPTION_ILLEGAL_DISCRETE_VALUE =
            "Value(%s) must be equal to valueFrom(%s) plus a multiple of stepSize(%s) when using"
                    + " stepSize(%s)";
    private static final String EXCEPTION_ILLEGAL_VALUE_FROM =
            "valueFrom(%s) must be smaller than valueTo(%s)";
    private static final String EXCEPTION_ILLEGAL_VALUE_TO =
            "valueTo(%s) must be greater than valueFrom(%s)";
    private static final String EXCEPTION_ILLEGAL_STEP_SIZE =
            "The stepSize(%s) must be 0, or a factor of the valueFrom(%s)-valueTo(%s) range";
    private static final String WARNING_FLOATING_POINT_ERRROR =
            "Floating point value used for %s(%s). Using floats can have rounding errors which may"
                    + " result in incorrect values. Instead, consider using integers with a custom"
                    + " LabelFormatter to display the  value correctly.";

    private static final int TIMEOUT_SEND_ACCESSIBILITY_EVENT = 200;
    private static final int HALO_ALPHA = 63;
    private static final double THRESHOLD = .0001;

    static final int DEF_STYLE_RES = R.style.Widget_MaterialComponents_Slider;
    static final int UNIT_VALUE = 1;
    static final int UNIT_PX = 0;

    @NonNull
    private final Paint inactiveTrackPaint;
    @NonNull
    private final Paint activeTrackPaint;
    @NonNull
    private final Paint thumbPaint;
    @NonNull
    private final Paint haloPaint;
    @NonNull
    private final Paint inactiveTicksPaint;
    @NonNull
    private final Paint activeTicksPaint;
    @NonNull
    private final AccessibilityHelper accessibilityHelper;
    private final AccessibilityManager accessibilityManager;
    private AccessibilityEventSender accessibilityEventSender;


    @NonNull
    private final List<L> changeListeners = new ArrayList<>();
    @NonNull
    private final List<T> touchListeners = new ArrayList<>();

    private final int scaledTouchSlop;

    private int minTrackSidePadding;
    private int defaultThumbRadius;

    private int widgetHeight;
    private int trackHeight;
    private int trackSidePadding;
    private int trackTop;
    private int thumbRadius;
    private int haloRadius;
    private float touchDownX;
    private MotionEvent lastEvent;
    private boolean thumbIsPressed = false;
    private float valueFrom;
    private float valueTo;


    private ArrayList<Float> values = new ArrayList<>();

    private int activeThumbIdx = -1;

    private int focusedThumbIdx = -1;
    private float stepSize = 0.0f;
    private float[] ticksCoordinates;
    private boolean tickVisible = true;
    private int trackWidth;
    private boolean forceDrawCompatHalo;
    private boolean isLongPress = false;
    private boolean dirtyConfig;

    @NonNull
    private ColorStateList haloColor;
    @NonNull
    private ColorStateList tickColorActive;
    @NonNull
    private ColorStateList tickColorInactive;
    @NonNull
    private ColorStateList trackColorActive;
    @NonNull
    private ColorStateList trackColorInactive;

    @NonNull
    private final MaterialShapeDrawable thumbDrawable = new MaterialShapeDrawable();

    private float touchPosition;
    @SeparationUnit
    private int separationUnit = UNIT_PX;


    @IntDef({UNIT_PX, UNIT_VALUE})
    @Retention(RetentionPolicy.SOURCE)
    @interface SeparationUnit {
    }

    public BaseSlider(@NonNull Context context) {
        this(context, null);
    }

    public BaseSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.sliderStyle);
    }

    public BaseSlider(
            @NonNull Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        context = getContext();

        inactiveTrackPaint = new Paint();
        inactiveTrackPaint.setStyle(Style.STROKE);
        inactiveTrackPaint.setStrokeCap(Cap.ROUND);

        activeTrackPaint = new Paint();
        activeTrackPaint.setStyle(Style.STROKE);
        activeTrackPaint.setStrokeCap(Cap.ROUND);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Style.FILL);
        thumbPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

        haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloPaint.setStyle(Style.FILL);

        inactiveTicksPaint = new Paint();
        inactiveTicksPaint.setStyle(Style.STROKE);
        inactiveTicksPaint.setStrokeCap(Cap.ROUND);

        activeTicksPaint = new Paint();
        activeTicksPaint.setStyle(Style.STROKE);
        activeTicksPaint.setStrokeCap(Cap.ROUND);

        loadResources(context.getResources());

        processAttributes(context, attrs, defStyleAttr);

        setFocusable(true);
        setClickable(true);


        thumbDrawable.setShadowCompatibilityMode(MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS);

        scaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        accessibilityHelper = new AccessibilityHelper(this);
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper);

        accessibilityManager =
                (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    private void loadResources(@NonNull Resources resources) {
        widgetHeight = resources.getDimensionPixelSize(R.dimen.mtrl_slider_widget_height);

        minTrackSidePadding = resources.getDimensionPixelOffset(R.dimen.mtrl_slider_track_side_padding);
        trackSidePadding = minTrackSidePadding;

        defaultThumbRadius = resources.getDimensionPixelSize(R.dimen.mtrl_slider_thumb_radius);

        trackTop = resources.getDimensionPixelOffset(R.dimen.mtrl_slider_track_top);
    }

    private void processAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a =context.obtainStyledAttributes(attrs, R.styleable.Slider, defStyleAttr, DEF_STYLE_RES);
        valueFrom = a.getFloat(R.styleable.Slider_android_valueFrom, 0.0f);
        valueTo = a.getFloat(R.styleable.Slider_android_valueTo, 1.0f);
        setValues(valueFrom);
        stepSize = a.getFloat(R.styleable.Slider_android_stepSize, 0.0f);

        boolean hasTrackColor = a.hasValue(R.styleable.Slider_trackColor);

        int trackColorInactiveRes =
                hasTrackColor ? R.styleable.Slider_trackColor : R.styleable.Slider_trackColorInactive;
        int trackColorActiveRes =
                hasTrackColor ? R.styleable.Slider_trackColor : R.styleable.Slider_trackColorActive;

        ColorStateList trackColorInactive =
                MaterialResources.getColorStateList(context, a, trackColorInactiveRes);
        setTrackInactiveTintList(
                trackColorInactive != null
                        ? trackColorInactive
                        : AppCompatResources.getColorStateList(
                        context, R.color.material_slider_inactive_track_color));
        ColorStateList trackColorActive =
                MaterialResources.getColorStateList(context, a, trackColorActiveRes);
        setTrackActiveTintList(
                trackColorActive != null
                        ? trackColorActive
                        : AppCompatResources.getColorStateList(
                        context, R.color.material_slider_active_track_color));
        ColorStateList thumbColor =
                MaterialResources.getColorStateList(context, a, R.styleable.Slider_thumbColor);
        thumbDrawable.setFillColor(thumbColor);

        if (a.hasValue(R.styleable.Slider_thumbStrokeColor)) {
            setThumbStrokeColor(
                    MaterialResources.getColorStateList(context, a, R.styleable.Slider_thumbStrokeColor));
        }
        setThumbStrokeWidth(a.getDimension(R.styleable.Slider_thumbStrokeWidth, 0));

        ColorStateList haloColor =
                MaterialResources.getColorStateList(context, a, R.styleable.Slider_haloColor);
        setHaloTintList(
                haloColor != null
                        ? haloColor
                        : AppCompatResources.getColorStateList(context, R.color.material_slider_halo_color));

        tickVisible = a.getBoolean(R.styleable.Slider_tickVisible, true);
        boolean hasTickColor = a.hasValue(R.styleable.Slider_tickColor);
        int tickColorInactiveRes =
                hasTickColor ? R.styleable.Slider_tickColor : R.styleable.Slider_tickColorInactive;
        int tickColorActiveRes =
                hasTickColor ? R.styleable.Slider_tickColor : R.styleable.Slider_tickColorActive;
        ColorStateList tickColorInactive =
                MaterialResources.getColorStateList(context, a, tickColorInactiveRes);
        setTickInactiveTintList(
                tickColorInactive != null
                        ? tickColorInactive
                        : AppCompatResources.getColorStateList(
                        context, R.color.material_slider_inactive_tick_marks_color));
        ColorStateList tickColorActive =
                MaterialResources.getColorStateList(context, a, tickColorActiveRes);
        setTickActiveTintList(
                tickColorActive != null
                        ? tickColorActive
                        : AppCompatResources.getColorStateList(
                        context, R.color.material_slider_active_tick_marks_color));

        setThumbRadius(a.getDimensionPixelSize(R.styleable.Slider_thumbRadius, 0));
        setHaloRadius(a.getDimensionPixelSize(R.styleable.Slider_haloRadius, 0));

        setThumbElevation(a.getDimension(R.styleable.Slider_thumbElevation, 0));

        setTrackHeight(a.getDimensionPixelSize(R.styleable.Slider_trackHeight, 0));

        if (!a.getBoolean(R.styleable.Slider_android_enabled, true)) {
            setEnabled(false);
        }

        a.recycle();
    }

    private void maybeIncreaseTrackSidePadding() {
        int increasedSidePadding = max(thumbRadius - defaultThumbRadius, 0);
        trackSidePadding = minTrackSidePadding + increasedSidePadding;
        if (ViewCompat.isLaidOut(this)) {
            updateTrackWidth(getWidth());
        }
    }

    private void validateValueFrom() {
        if (valueFrom >= valueTo) {
            throw new IllegalStateException(
                    String.format(
                            EXCEPTION_ILLEGAL_VALUE_FROM, Float.toString(valueFrom), Float.toString(valueTo)));
        }
    }

    private void validateValueTo() {
        if (valueTo <= valueFrom) {
            throw new IllegalStateException(
                    String.format(
                            EXCEPTION_ILLEGAL_VALUE_TO, Float.toString(valueTo), Float.toString(valueFrom)));
        }
    }

    private boolean valueLandsOnTick(float value) {


        double potentialTickValue =
                new BigDecimal(Float.toString(value))
                        .subtract(new BigDecimal(Float.toString(valueFrom)))
                        .divide(new BigDecimal(Float.toString(stepSize)), MathContext.DECIMAL64)
                        .doubleValue();


        return Math.abs(Math.round(potentialTickValue) - potentialTickValue) < THRESHOLD;
    }

    private void validateStepSize() {
        if (stepSize > 0.0f && !valueLandsOnTick(valueTo)) {
            throw new IllegalStateException(
                    String.format(
                            EXCEPTION_ILLEGAL_STEP_SIZE,
                            Float.toString(stepSize),
                            Float.toString(valueFrom),
                            Float.toString(valueTo)));
        }
    }

    private void validateValues() {
        for (Float value : values) {
            if (value < valueFrom || value > valueTo) {
                throw new IllegalStateException(
                        String.format(
                                EXCEPTION_ILLEGAL_VALUE,
                                Float.toString(value),
                                Float.toString(valueFrom),
                                Float.toString(valueTo)));
            }
            if (stepSize > 0.0f && !valueLandsOnTick(value)) {
                throw new IllegalStateException(
                        String.format(
                                EXCEPTION_ILLEGAL_DISCRETE_VALUE,
                                Float.toString(value),
                                Float.toString(valueFrom),
                                Float.toString(stepSize),
                                Float.toString(stepSize)));
            }
        }
    }

    private void warnAboutFloatingPointError() {
        if (stepSize == 0) {

            return;
        }

        if ((int) stepSize != stepSize) {
            Log.w(TAG, String.format(WARNING_FLOATING_POINT_ERRROR, "stepSize", stepSize));
        }

        if ((int) valueFrom != valueFrom) {
            Log.w(TAG, String.format(WARNING_FLOATING_POINT_ERRROR, "valueFrom", valueFrom));
        }

        if ((int) valueTo != valueTo) {
            Log.w(TAG, String.format(WARNING_FLOATING_POINT_ERRROR, "valueTo", valueTo));
        }
    }

    private void validateConfigurationIfDirty() {
        if (dirtyConfig) {
            validateValueFrom();
            validateValueTo();
            validateStepSize();
            validateValues();
            warnAboutFloatingPointError();
            dirtyConfig = false;
        }
    }

    public float getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(float valueFrom) {
        this.valueFrom = valueFrom;
        dirtyConfig = true;
        postInvalidate();
    }

    public float getValueTo() {
        return valueTo;
    }

    public void setValueTo(float valueTo) {
        this.valueTo = valueTo;
        dirtyConfig = true;
        postInvalidate();
    }

    @NonNull
    List<Float> getValues() {
        return new ArrayList<>(values);
    }

    void setValues(@NonNull Float... values) {
        ArrayList<Float> list = new ArrayList<>();
        Collections.addAll(list, values);
        setValuesInternal(list);
    }

    void setValues(@NonNull List<Float> values) {
        setValuesInternal(new ArrayList<>(values));
    }

    private void setValuesInternal(@NonNull ArrayList<Float> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one value must be set");
        }

        Collections.sort(values);

        if (this.values.size() == values.size()) {
            if (this.values.equals(values)) {
                return;
            }
        }

        this.values = values;
        dirtyConfig = true;

        focusedThumbIdx = 0;
        updateHaloHotspot();
        dispatchOnChangedProgramatically();
        postInvalidate();
    }


    public float getStepSize() {
        return stepSize;
    }

    public void setStepSize(float stepSize) {
        if (stepSize < 0.0f) {
            throw new IllegalArgumentException(
                    String.format(
                            EXCEPTION_ILLEGAL_STEP_SIZE,
                            Float.toString(stepSize),
                            Float.toString(valueFrom),
                            Float.toString(valueTo)));
        }
        if (this.stepSize != stepSize) {
            this.stepSize = stepSize;
            dirtyConfig = true;
            postInvalidate();
        }
    }

    public int getFocusedThumbIndex() {
        return focusedThumbIdx;
    }

    public void setFocusedThumbIndex(int index) {
        if (index < 0 || index >= values.size()) {
            throw new IllegalArgumentException("index out of range");
        }
        focusedThumbIdx = index;
        accessibilityHelper.requestKeyboardFocusForVirtualView(focusedThumbIdx);
        postInvalidate();
    }

    protected void setActiveThumbIndex(int index) {
        activeThumbIdx = index;
    }

    public int getActiveThumbIndex() {
        return activeThumbIdx;
    }

    public void addOnChangeListener(@Nullable L listener) {
        changeListeners.add(listener);
    }

    public void removeOnChangeListener(@NonNull L listener) {
        changeListeners.remove(listener);
    }

    public void clearOnChangeListeners() {
        changeListeners.clear();
    }

    public void addOnSliderTouchListener(@NonNull T listener) {
        touchListeners.add(listener);
    }

    public void removeOnSliderTouchListener(@NonNull T listener) {
        touchListeners.remove(listener);
    }

    public void clearOnSliderTouchListeners() {
        touchListeners.clear();
    }

    public float getThumbElevation() {
        return thumbDrawable.getElevation();
    }

    public void setThumbElevation(float elevation) {
        thumbDrawable.setElevation(elevation);
    }

    public void setThumbElevationResource(@DimenRes int elevation) {
        setThumbElevation(getResources().getDimension(elevation));
    }

    @Dimension
    public int getThumbRadius() {
        return thumbRadius;
    }

    public void setThumbRadius(@IntRange(from = 0) @Dimension int radius) {
        if (radius == thumbRadius) {
            return;
        }

        thumbRadius = radius;
        maybeIncreaseTrackSidePadding();

        thumbDrawable.setShapeAppearanceModel(
                ShapeAppearanceModel.builder().setAllCorners(thumbRadius).build());
        thumbDrawable.setBounds(0, 0, thumbRadius * 2, thumbRadius * 2);

        postInvalidate();
    }

    public void setThumbRadiusResource(@DimenRes int radius) {
        setThumbRadius(getResources().getDimensionPixelSize(radius));
    }

    public void setThumbStrokeColor(@Nullable ColorStateList thumbStrokeColor) {
        thumbDrawable.setStrokeColor(thumbStrokeColor);
        postInvalidate();
    }

    public void setThumbStrokeColorResource(@ColorRes int thumbStrokeColorResourceId) {
        if (thumbStrokeColorResourceId != 0) {
            setThumbStrokeColor(
                    AppCompatResources.getColorStateList(getContext(), thumbStrokeColorResourceId));
        }
    }

    public ColorStateList getThumbStrokeColor() {
        return thumbDrawable.getStrokeColor();
    }

    public void setThumbStrokeWidth(float thumbStrokeWidth) {
        thumbDrawable.setStrokeWidth(thumbStrokeWidth);
        postInvalidate();
    }

    public void setThumbStrokeWidthResource(@DimenRes int thumbStrokeWidthResourceId) {
        if (thumbStrokeWidthResourceId != 0) {
            setThumbStrokeWidth(getResources().getDimension(thumbStrokeWidthResourceId));
        }
    }

    public float getThumbStrokeWidth() {
        return thumbDrawable.getStrokeWidth();
    }

    @Dimension()
    public int getHaloRadius() {
        return haloRadius;
    }

    public void setHaloRadius(@IntRange(from = 0) @Dimension int radius) {
        if (radius == haloRadius) {
            return;
        }

        haloRadius = radius;
        Drawable background = getBackground();
        if (!shouldDrawCompatHalo() && background instanceof RippleDrawable) {
            DrawableUtils.setRippleDrawableRadius((RippleDrawable) background, haloRadius);
            return;
        }

        postInvalidate();
    }

    public void setHaloRadiusResource(@DimenRes int radius) {
        setHaloRadius(getResources().getDimensionPixelSize(radius));
    }

    @Dimension()
    public int getTrackSidePadding() {
        return trackSidePadding;
    }

    @Dimension()
    public int getTrackWidth() {
        return trackWidth;
    }

    @Dimension()
    public int getTrackHeight() {
        return trackHeight;
    }

    public void setTrackHeight(@IntRange(from = 0) @Dimension int trackHeight) {
        if (this.trackHeight != trackHeight) {
            this.trackHeight = trackHeight;
            invalidateTrack();
            postInvalidate();
        }
    }

    @NonNull
    public ColorStateList getHaloTintList() {
        return haloColor;
    }

    public void setHaloTintList(@NonNull ColorStateList haloColor) {
        if (haloColor.equals(this.haloColor)) {
            return;
        }

        this.haloColor = haloColor;
        Drawable background = getBackground();
        if (!shouldDrawCompatHalo() && background instanceof RippleDrawable) {
            ((RippleDrawable) background).setColor(haloColor);
            return;
        }

        haloPaint.setColor(getColorForState(haloColor));
        haloPaint.setAlpha(HALO_ALPHA);
        invalidate();
    }

    @NonNull
    public ColorStateList getThumbTintList() {
        return thumbDrawable.getFillColor();
    }

    public void setThumbTintList(@NonNull ColorStateList thumbColor) {
        thumbDrawable.setFillColor(thumbColor);
    }

    @NonNull
    public ColorStateList getTickTintList() {
        if (!tickColorInactive.equals(tickColorActive)) {
            throw new IllegalStateException(
                    "The inactive and active ticks are different colors. Use the getTickColorInactive() and"
                            + " getTickColorActive() methods instead.");
        }
        return tickColorActive;
    }

    public void setTickTintList(@NonNull ColorStateList tickColor) {
        setTickInactiveTintList(tickColor);
        setTickActiveTintList(tickColor);
    }

    @NonNull
    public ColorStateList getTickActiveTintList() {
        return tickColorActive;
    }

    public void setTickActiveTintList(@NonNull ColorStateList tickColor) {
        if (tickColor.equals(tickColorActive)) {
            return;
        }
        tickColorActive = tickColor;
        activeTicksPaint.setColor(getColorForState(tickColorActive));
        invalidate();
    }

    @NonNull
    public ColorStateList getTickInactiveTintList() {
        return tickColorInactive;
    }

    public void setTickInactiveTintList(@NonNull ColorStateList tickColor) {
        if (tickColor.equals(tickColorInactive)) {
            return;
        }
        tickColorInactive = tickColor;
        inactiveTicksPaint.setColor(getColorForState(tickColorInactive));
        invalidate();
    }

    public boolean isTickVisible() {
        return tickVisible;
    }

    public void setTickVisible(boolean tickVisible) {
        if (this.tickVisible != tickVisible) {
            this.tickVisible = tickVisible;
            postInvalidate();
        }
    }

    @NonNull
    public ColorStateList getTrackTintList() {
        if (!trackColorInactive.equals(trackColorActive)) {
            throw new IllegalStateException(
                    "The inactive and active parts of the track are different colors. Use the"
                            + " getInactiveTrackColor() and getActiveTrackColor() methods instead.");
        }
        return trackColorActive;
    }

    public void setTrackTintList(@NonNull ColorStateList trackColor) {
        setTrackInactiveTintList(trackColor);
        setTrackActiveTintList(trackColor);
    }

    @NonNull
    public ColorStateList getTrackActiveTintList() {
        return trackColorActive;
    }

    public void setTrackActiveTintList(@NonNull ColorStateList trackColor) {
        if (trackColor.equals(trackColorActive)) {
            return;
        }
        trackColorActive = trackColor;
        activeTrackPaint.setColor(getColorForState(trackColorActive));
        invalidate();
    }

    @NonNull
    public ColorStateList getTrackInactiveTintList() {
        return trackColorInactive;
    }

    public void setTrackInactiveTintList(@NonNull ColorStateList trackColor) {
        if (trackColor.equals(trackColorInactive)) {
            return;
        }
        trackColorInactive = trackColor;
        inactiveTrackPaint.setColor(getColorForState(trackColorInactive));
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);


        setLayerType(enabled ? LAYER_TYPE_NONE : LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }


    @Override
    protected void onDetachedFromWindow() {
        if (accessibilityEventSender != null) {
            removeCallbacks(accessibilityEventSender);
        }

        super.onDetachedFromWindow();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(widgetHeight, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateTrackWidth(w);
        updateHaloHotspot();
    }

    private void maybeCalculateTicksCoordinates() {
        if (stepSize <= 0.0f) {
            return;
        }

        validateConfigurationIfDirty();

        int tickCount = (int) ((valueTo - valueFrom) / stepSize + 1);

        tickCount = Math.min(tickCount, trackWidth / (trackHeight * 2) + 1);
        if (ticksCoordinates == null || ticksCoordinates.length != tickCount * 2) {
            ticksCoordinates = new float[tickCount * 2];
        }

        float interval = trackWidth / (float) (tickCount - 1);
        for (int i = 0; i < tickCount * 2; i += 2) {
            ticksCoordinates[i] = trackSidePadding + i / 2 * interval;
            ticksCoordinates[i + 1] = calculateTop();
        }
    }

    private void updateTrackWidth(int width) {

        trackWidth = Math.max(width - trackSidePadding * 2, 0);


        maybeCalculateTicksCoordinates();
    }

    private void updateHaloHotspot() {

        if (!shouldDrawCompatHalo() && getMeasuredWidth() > 0) {
            final Drawable background = getBackground();
            if (background instanceof RippleDrawable) {
                int x = (int) (normalizeValue(values.get(focusedThumbIdx)) * trackWidth + trackSidePadding);
                int y = calculateTop();
                DrawableCompat.setHotspotBounds(
                        background, x - haloRadius, y - haloRadius, x + haloRadius, y + haloRadius);
            }
        }
    }

    private int calculateTop() {
        return trackTop;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (dirtyConfig) {
            validateConfigurationIfDirty();


            maybeCalculateTicksCoordinates();
        }

        super.onDraw(canvas);

        int top = calculateTop();

        drawInactiveTrack(canvas, trackWidth, top);
        if (Collections.max(getValues()) > valueFrom) {
            drawActiveTrack(canvas, trackWidth, top);
        }

        maybeDrawTicks(canvas);

        if ((thumbIsPressed || isFocused()) && isEnabled()) {
            maybeDrawHalo(canvas, trackWidth, top);
        }

        drawThumbs(canvas, trackWidth, top);
    }

    private float[] getActiveRange() {
        float max = Collections.max(getValues());
        float min = Collections.min(getValues());
        float left = normalizeValue(values.size() == 1 ? valueFrom : min);
        float right = normalizeValue(max);


        return isRtl() ? new float[]{right, left} : new float[]{left, right};
    }

    private void drawInactiveTrack(@NonNull Canvas canvas, int width, int top) {
        float[] activeRange = getActiveRange();
        float right = trackSidePadding + activeRange[1] * width;
        if (right < trackSidePadding + width) {
            canvas.drawLine(right, top, trackSidePadding + width, top, inactiveTrackPaint);
        }


        float left = trackSidePadding + activeRange[0] * width;
        if (left > trackSidePadding) {
            canvas.drawLine(trackSidePadding, top, left, top, inactiveTrackPaint);
        }
    }

    private float normalizeValue(float value) {
        float normalized = (value - valueFrom) / (valueTo - valueFrom);
        if (isRtl()) {
            return 1 - normalized;
        }
        return normalized;
    }

    private void drawActiveTrack(@NonNull Canvas canvas, int width, int top) {
        float[] activeRange = getActiveRange();
        float right = trackSidePadding + activeRange[1] * width;
        float left = trackSidePadding + activeRange[0] * width;
        canvas.drawLine(left, top, right, top, activeTrackPaint);
    }

    private void maybeDrawTicks(@NonNull Canvas canvas) {
        if (!tickVisible || stepSize <= 0.0f) {
            return;
        }

        float[] activeRange = getActiveRange();
        int leftPivotIndex = pivotIndex(ticksCoordinates, activeRange[0]);
        int rightPivotIndex = pivotIndex(ticksCoordinates, activeRange[1]);


        canvas.drawPoints(ticksCoordinates, 0, leftPivotIndex * 2, inactiveTicksPaint);


        canvas.drawPoints(
                ticksCoordinates,
                leftPivotIndex * 2,
                rightPivotIndex * 2 - leftPivotIndex * 2,
                activeTicksPaint);


        canvas.drawPoints(
                ticksCoordinates,
                rightPivotIndex * 2,
                ticksCoordinates.length - rightPivotIndex * 2,
                inactiveTicksPaint);
    }

    private void drawThumbs(@NonNull Canvas canvas, int width, int top) {


        if (!isEnabled()) {
            for (Float value : values) {
                canvas.drawCircle(
                        trackSidePadding + normalizeValue(value) * width, top, thumbRadius, thumbPaint);
            }
        }

        for (Float value : values) {
            canvas.save();
            canvas.translate(
                    trackSidePadding + (int) (normalizeValue(value) * width) - thumbRadius,
                    top - thumbRadius);
            thumbDrawable.draw(canvas);
            canvas.restore();
        }
    }

    private void maybeDrawHalo(@NonNull Canvas canvas, int width, int top) {

        if (shouldDrawCompatHalo()) {
            int centerX = (int) (trackSidePadding + normalizeValue(values.get(focusedThumbIdx)) * width);
            if (VERSION.SDK_INT < VERSION_CODES.P) {

                canvas.clipRect(
                        centerX - haloRadius,
                        top - haloRadius,
                        centerX + haloRadius,
                        top + haloRadius,
                        Op.UNION);
            }
            canvas.drawCircle(centerX, top, haloRadius, haloPaint);
        }
    }

    private boolean shouldDrawCompatHalo() {
        return forceDrawCompatHalo
                || VERSION.SDK_INT < VERSION_CODES.LOLLIPOP
                || !(getBackground() instanceof RippleDrawable);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        float x = event.getX();
        touchPosition = (x - trackSidePadding) / trackWidth;
        touchPosition = Math.max(0, touchPosition);
        touchPosition = Math.min(1, touchPosition);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = x;


                if (isInScrollingContainer()) {
                    break;
                }

                getParent().requestDisallowInterceptTouchEvent(true);

                if (!pickActiveThumb()) {

                    break;
                }

                requestFocus();
                thumbIsPressed = true;
                snapTouchPosition();
                updateHaloHotspot();
                invalidate();
                onStartTrackingTouch();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!thumbIsPressed) {

                    if (abs(x - touchDownX) < scaledTouchSlop) {
                        return false;
                    }
                    getParent().requestDisallowInterceptTouchEvent(true);
                    onStartTrackingTouch();
                }

                if (!pickActiveThumb()) {

                    break;
                }

                thumbIsPressed = true;
                snapTouchPosition();
                updateHaloHotspot();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                thumbIsPressed = false;

                if (lastEvent != null
                        && lastEvent.getActionMasked() == MotionEvent.ACTION_DOWN
                        && abs(lastEvent.getX() - event.getX()) <= scaledTouchSlop
                        && abs(lastEvent.getY() - event.getY()) <= scaledTouchSlop) {
                    pickActiveThumb();
                }

                if (activeThumbIdx != -1) {
                    snapTouchPosition();
                    activeThumbIdx = -1;
                }
                onStopTrackingTouch();
                invalidate();
                break;
            default:

        }


        setPressed(thumbIsPressed);

        lastEvent = MotionEvent.obtain(event);
        return true;
    }

    private static int pivotIndex(float[] coordinates, float position) {
        return Math.round(position * (coordinates.length / 2 - 1));
    }

    private double snapPosition(float position) {
        if (stepSize > 0.0f) {
            int stepCount = (int) ((valueTo - valueFrom) / stepSize);
            return Math.round(position * stepCount) / (double) stepCount;
        }

        return position;
    }


    protected boolean pickActiveThumb() {
        if (activeThumbIdx != -1) {
            return true;
        }

        float touchValue = getValueOfTouchPositionAbsolute();
        float touchX = valueToX(touchValue);
        activeThumbIdx = 0;
        float activeThumbDiff = abs(values.get(activeThumbIdx) - touchValue);
        for (int i = 1; i < values.size(); i++) {
            float valueDiff = abs(values.get(i) - touchValue);
            float valueX = valueToX(values.get(i));
            if (compare(valueDiff, activeThumbDiff) > 1) {
                break;
            }

            boolean movingForward = isRtl() ? (valueX - touchX) > 0 : (valueX - touchX) < 0;


            if (compare(valueDiff, activeThumbDiff) < 0) {
                activeThumbDiff = valueDiff;
                activeThumbIdx = i;
                continue;
            }

            if (compare(valueDiff, activeThumbDiff) == 0) {

                if (abs(valueX - touchX) < scaledTouchSlop) {
                    activeThumbIdx = -1;
                    return false;
                }

                if (movingForward) {
                    activeThumbDiff = valueDiff;
                    activeThumbIdx = i;
                }
            }
        }

        return activeThumbIdx != -1;
    }

    private float getValueOfTouchPositionAbsolute() {
        float position = touchPosition;
        if (isRtl()) {
            position = 1 - position;
        }
        return (position * (valueTo - valueFrom) + valueFrom);
    }

    private boolean snapTouchPosition() {
        return snapActiveThumbToValue(getValueOfTouchPosition());
    }

    private boolean snapActiveThumbToValue(float value) {
        return snapThumbToValue(activeThumbIdx, value);
    }

    private boolean snapThumbToValue(int idx, float value) {

        if (abs(value - values.get(idx)) < THRESHOLD) {
            return false;
        }

        float newValue = getClampedValue(idx, value);

        values.set(idx, newValue);
        focusedThumbIdx = idx;

        dispatchOnChangedFromUser(idx);
        return true;
    }

    private float getClampedValue(int idx, float value) {
        float minSeparation = stepSize == 0 ? getMinSeparation() : 0;
        minSeparation = separationUnit == UNIT_PX ? dimenToValue(minSeparation) : minSeparation;
        if (isRtl()) {
            minSeparation = -minSeparation;
        }

        float upperBound = idx + 1 >= values.size() ? valueTo : values.get(idx + 1) - minSeparation;
        float lowerBound = idx - 1 < 0 ? valueFrom : values.get(idx - 1) + minSeparation;
        return clamp(value, lowerBound, upperBound);
    }

    private float dimenToValue(float dimen) {
        if (dimen == 0) {
            return 0;
        }
        return ((dimen - trackSidePadding) / trackWidth) * (valueFrom - valueTo) + valueFrom;
    }

    protected void setSeparationUnit(int separationUnit) {
        this.separationUnit = separationUnit;
    }

    protected float getMinSeparation() {
        return 0;
    }

    private float getValueOfTouchPosition() {
        double position = snapPosition(touchPosition);


        if (isRtl()) {
            position = 1 - position;
        }
        return (float) (position * (valueTo - valueFrom) + valueFrom);
    }

    private float valueToX(float value) {
        return normalizeValue(value) * trackWidth + trackSidePadding;
    }

    private void invalidateTrack() {
        inactiveTrackPaint.setStrokeWidth(trackHeight);
        activeTrackPaint.setStrokeWidth(trackHeight);
        inactiveTicksPaint.setStrokeWidth(trackHeight / 2.0f);
        activeTicksPaint.setStrokeWidth(trackHeight / 2.0f);
    }

    private boolean isInScrollingContainer() {
        ViewParent p = getParent();
        while (p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void dispatchOnChangedProgramatically() {
        for (L listener : changeListeners) {
            for (Float value : values) {
                listener.onValueChange((S) this, value, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchOnChangedFromUser(int idx) {
        for (L listener : changeListeners) {
            listener.onValueChange((S) this, values.get(idx), true);
        }
        if (accessibilityManager != null && accessibilityManager.isEnabled()) {
            scheduleAccessibilityEventSender(idx);
        }
    }

    @SuppressWarnings("unchecked")
    private void onStartTrackingTouch() {
        for (T listener : touchListeners) {
            listener.onStartTrackingTouch((S) this);
        }
    }

    @SuppressWarnings("unchecked")
    private void onStopTrackingTouch() {
        for (T listener : touchListeners) {
            listener.onStopTrackingTouch((S) this);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        inactiveTrackPaint.setColor(getColorForState(trackColorInactive));
        activeTrackPaint.setColor(getColorForState(trackColorActive));
        inactiveTicksPaint.setColor(getColorForState(tickColorInactive));
        activeTicksPaint.setColor(getColorForState(tickColorActive));
        if (thumbDrawable.isStateful()) {
            thumbDrawable.setState(getDrawableState());
        }
        haloPaint.setColor(getColorForState(haloColor));
        haloPaint.setAlpha(HALO_ALPHA);
    }

    @ColorInt
    private int getColorForState(@NonNull ColorStateList colorStateList) {
        return colorStateList.getColorForState(getDrawableState(), colorStateList.getDefaultColor());
    }

    @VisibleForTesting
    void forceDrawCompatHalo(boolean force) {
        forceDrawCompatHalo = force;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (!isEnabled()) {
            return super.onKeyDown(keyCode, event);
        }


        if (values.size() == 1) {
            activeThumbIdx = 0;
        }


        if (activeThumbIdx == -1) {
            Boolean handled = onKeyDownNoActiveThumb(keyCode, event);
            return handled != null ? handled : super.onKeyDown(keyCode, event);
        }

        isLongPress |= event.isLongPress();
        Float increment = calculateIncrementForKey(keyCode);
        if (increment != null) {
            if (snapActiveThumbToValue(values.get(activeThumbIdx) + increment)) {
                updateHaloHotspot();
                postInvalidate();
            }
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_TAB:
                if (event.hasNoModifiers()) {
                    return moveFocus(1);
                }

                if (event.isShiftPressed()) {
                    return moveFocus(-1);
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                activeThumbIdx = -1;
                postInvalidate();
                return true;
            default:

        }

        return super.onKeyDown(keyCode, event);
    }

    private Boolean onKeyDownNoActiveThumb(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_TAB:
                if (event.hasNoModifiers()) {
                    return moveFocus(1);
                }

                if (event.isShiftPressed()) {
                    return moveFocus(-1);
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                moveFocusInAbsoluteDirection(-1);
                return true;
            case KeyEvent.KEYCODE_MINUS:
                moveFocus(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                moveFocusInAbsoluteDirection(1);
                return true;
            case KeyEvent.KEYCODE_EQUALS:

            case KeyEvent.KEYCODE_PLUS:
                moveFocus(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                activeThumbIdx = focusedThumbIdx;
                postInvalidate();
                return true;
            default:

        }

        return null;
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        isLongPress = false;
        return super.onKeyUp(keyCode, event);
    }

    final boolean isRtl() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    private boolean moveFocus(int direction) {
        int oldFocusedThumbIdx = focusedThumbIdx;

        final long newFocusedThumbIdx = (long) oldFocusedThumbIdx + direction;
        focusedThumbIdx = (int) clamp(newFocusedThumbIdx, 0, values.size() - 1);
        if (focusedThumbIdx == oldFocusedThumbIdx) {

            return false;
        }
        if (activeThumbIdx != -1) {
            activeThumbIdx = focusedThumbIdx;
        }
        updateHaloHotspot();
        postInvalidate();
        return true;
    }

    private boolean moveFocusInAbsoluteDirection(int direction) {
        if (isRtl()) {

            direction = direction == Integer.MIN_VALUE ? Integer.MAX_VALUE : -direction;
        }
        return moveFocus(direction);
    }

    private Float calculateIncrementForKey(int keyCode) {


        float increment = isLongPress ? calculateStepIncrement(20) : calculateStepIncrement();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return isRtl() ? increment : -increment;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return isRtl() ? -increment : increment;
            case KeyEvent.KEYCODE_MINUS:
                return -increment;
            case KeyEvent.KEYCODE_EQUALS:

            case KeyEvent.KEYCODE_PLUS:
                return increment;
            default:
                return null;
        }
    }

    private float calculateStepIncrement() {
        return stepSize == 0 ? 1 : stepSize;
    }

    private float calculateStepIncrement(int stepFactor) {
        float increment = calculateStepIncrement();
        float numSteps = (valueTo - valueFrom) / increment;
        if (numSteps <= stepFactor) {
            return increment;
        }

        return Math.round((numSteps / stepFactor)) * increment;
    }

    @Override
    protected void onFocusChanged(
            boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            activeThumbIdx = -1;
            accessibilityHelper.clearKeyboardFocusForVirtualView(focusedThumbIdx);
        } else {
            focusThumbOnFocusGained(direction);
            accessibilityHelper.requestKeyboardFocusForVirtualView(focusedThumbIdx);
        }
    }

    private void focusThumbOnFocusGained(int direction) {
        switch (direction) {
            case FOCUS_BACKWARD:
                moveFocus(Integer.MAX_VALUE);
                break;
            case FOCUS_LEFT:
                moveFocusInAbsoluteDirection(Integer.MAX_VALUE);
                break;
            case FOCUS_FORWARD:
                moveFocus(Integer.MIN_VALUE);
                break;
            case FOCUS_RIGHT:
                moveFocusInAbsoluteDirection(Integer.MIN_VALUE);
                break;
            case FOCUS_UP:
            case FOCUS_DOWN:
            default:

        }
    }

    @VisibleForTesting
    final int getAccessibilityFocusedVirtualViewId() {
        return accessibilityHelper.getAccessibilityFocusedVirtualViewId();
    }

    @NonNull
    @Override
    public CharSequence getAccessibilityClassName() {
        return SeekBar.class.getName();
    }

    @Override
    public boolean dispatchHoverEvent(@NonNull MotionEvent event) {
        return accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {


        return super.dispatchKeyEvent(event);
    }

    private void scheduleAccessibilityEventSender(int idx) {
        if (accessibilityEventSender == null) {
            accessibilityEventSender = new AccessibilityEventSender();
        } else {
            removeCallbacks(accessibilityEventSender);
        }
        accessibilityEventSender.setVirtualViewId(idx);
        postDelayed(accessibilityEventSender, TIMEOUT_SEND_ACCESSIBILITY_EVENT);
    }

    private class AccessibilityEventSender implements Runnable {
        int virtualViewId = -1;

        void setVirtualViewId(int virtualViewId) {
            this.virtualViewId = virtualViewId;
        }

        @Override
        public void run() {
            accessibilityHelper.sendEventForVirtualView(
                    virtualViewId, AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SliderState sliderState = new SliderState(superState);
        sliderState.valueFrom = valueFrom;
        sliderState.valueTo = valueTo;
        sliderState.values = new ArrayList<>(values);
        sliderState.stepSize = stepSize;
        sliderState.hasFocus = hasFocus();
        return sliderState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SliderState sliderState = (SliderState) state;
        super.onRestoreInstanceState(sliderState.getSuperState());

        valueFrom = sliderState.valueFrom;
        valueTo = sliderState.valueTo;
        setValuesInternal(sliderState.values);
        stepSize = sliderState.stepSize;
        if (sliderState.hasFocus) {
            requestFocus();
        }
        dispatchOnChangedProgramatically();
    }

    static class SliderState extends BaseSavedState {

        float valueFrom;
        float valueTo;
        ArrayList<Float> values;
        float stepSize;
        boolean hasFocus;

        public static final Creator<SliderState> CREATOR =
                new Creator<SliderState>() {

                    @NonNull
                    @Override
                    public SliderState createFromParcel(@NonNull Parcel source) {
                        return new SliderState(source);
                    }

                    @NonNull
                    @Override
                    public SliderState[] newArray(int size) {
                        return new SliderState[size];
                    }
                };

        SliderState(Parcelable superState) {
            super(superState);
        }

        private SliderState(@NonNull Parcel source) {
            super(source);
            valueFrom = source.readFloat();
            valueTo = source.readFloat();
            values = new ArrayList<>();
            source.readList(values, Float.class.getClassLoader());
            stepSize = source.readFloat();
            hasFocus = source.createBooleanArray()[0];
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloat(valueFrom);
            dest.writeFloat(valueTo);
            dest.writeList(values);
            dest.writeFloat(stepSize);
            boolean[] booleans = new boolean[1];
            booleans[0] = hasFocus;
            dest.writeBooleanArray(booleans);
        }
    }

    void updateBoundsForVirturalViewId(int virtualViewId, Rect virtualViewBounds) {
        int x = trackSidePadding + (int) (normalizeValue(getValues().get(virtualViewId)) * trackWidth);
        int y = calculateTop();

        virtualViewBounds.set(x - thumbRadius, y - thumbRadius, x + thumbRadius, y + thumbRadius);
    }

    private static class AccessibilityHelper extends ExploreByTouchHelper {

        private final BaseSlider<?, ?, ?> slider;
        Rect virtualViewBounds = new Rect();

        AccessibilityHelper(BaseSlider<?, ?, ?> slider) {
            super(slider);
            this.slider = slider;
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            for (int i = 0; i < slider.getValues().size(); i++) {
                slider.updateBoundsForVirturalViewId(i, virtualViewBounds);
                if (virtualViewBounds.contains((int) x, (int) y)) {
                    return i;
                }
            }

            return ExploreByTouchHelper.HOST_ID;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int i = 0; i < slider.getValues().size(); i++) {
                virtualViewIds.add(i);
            }
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId, AccessibilityNodeInfoCompat info) {

            info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SET_PROGRESS);

            List<Float> values = slider.getValues();
            final float value = values.get(virtualViewId);
            float valueFrom = slider.getValueFrom();
            float valueTo = slider.getValueTo();

            if (slider.isEnabled()) {
                if (value > valueFrom) {
                    info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
                }
                if (value < valueTo) {
                    info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                }
            }

            info.setRangeInfo(RangeInfoCompat.obtain(RANGE_TYPE_FLOAT, valueFrom, valueTo, value));
            info.setClassName(SeekBar.class.getName());

            slider.updateBoundsForVirturalViewId(virtualViewId, virtualViewBounds);
            info.setBoundsInParent(virtualViewBounds);
        }


        @Override
        protected boolean onPerformActionForVirtualView(
                int virtualViewId, int action, Bundle arguments) {
            if (!slider.isEnabled()) {
                return false;
            }

            switch (action) {
                case android.R.id.accessibilityActionSetProgress: {
                    if (arguments == null
                            || !arguments.containsKey(
                            AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE)) {
                        return false;
                    }
                    float value =
                            arguments.getFloat(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE);
                    if (slider.snapThumbToValue(virtualViewId, value)) {
                        slider.updateHaloHotspot();
                        slider.postInvalidate();
                        invalidateVirtualView(virtualViewId);
                        return true;
                    }
                    return false;
                }
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD: {
                    float increment = slider.calculateStepIncrement(20);
                    if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
                        increment = -increment;
                    }


                    if (slider.isRtl()) {
                        increment = -increment;
                    }

                    List<Float> values = slider.getValues();
                    float clamped =
                            clamp(
                                    values.get(virtualViewId) + increment,
                                    slider.getValueFrom(),
                                    slider.getValueTo());
                    if (slider.snapThumbToValue(virtualViewId, clamped)) {
                        slider.updateHaloHotspot();
                        slider.postInvalidate();
                        invalidateVirtualView(virtualViewId);
                        return true;
                    }
                    return false;
                }
                default:
                    return false;
            }
        }
    }
}
