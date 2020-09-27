package com.ctrader.matslider.internal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;


public class ShadowRenderer {

    private static final int COLOR_ALPHA_START = 0x44;
    private static final int COLOR_ALPHA_MIDDLE = 0x14;

    private static final int COLOR_ALPHA_END = 0;
    private static final int[] edgeColors = new int[3];
    private static final float[] edgePositions = new float[]{0f, .5f, 1f};
    private static final int[] cornerColors = new int[4];
    private static final float[] cornerPositions = new float[]{0f, 0f, .5f, 1f};
    @NonNull
    private final Paint shadowPaint;
    @NonNull
    private final Paint cornerShadowPaint;
    @NonNull
    private final Paint edgeShadowPaint;
    private final Path scratch = new Path();
    private int shadowStartColor;
    private int shadowMiddleColor;
    private int shadowEndColor;
    private Paint transparentPaint = new Paint();

    public ShadowRenderer() {
        this(Color.BLACK);
    }

    public ShadowRenderer(int color) {
        shadowPaint = new Paint();
        setShadowColor(color);

        transparentPaint.setColor(Color.TRANSPARENT);
        cornerShadowPaint = new Paint(Paint.DITHER_FLAG);
        cornerShadowPaint.setStyle(Paint.Style.FILL);

        edgeShadowPaint = new Paint(cornerShadowPaint);
    }

    public void setShadowColor(int color) {
        shadowStartColor = ColorUtils.setAlphaComponent(color, COLOR_ALPHA_START);
        shadowMiddleColor = ColorUtils.setAlphaComponent(color, COLOR_ALPHA_MIDDLE);
        shadowEndColor = ColorUtils.setAlphaComponent(color, COLOR_ALPHA_END);
        shadowPaint.setColor(shadowStartColor);
    }

    public void drawEdgeShadow(
            @NonNull Canvas canvas, @Nullable Matrix transform, @NonNull RectF bounds, int elevation) {
        bounds.bottom += elevation;
        bounds.offset(0, -elevation);

        edgeColors[0] = shadowEndColor;
        edgeColors[1] = shadowMiddleColor;
        edgeColors[2] = shadowStartColor;

        edgeShadowPaint.setShader(
                new LinearGradient(
                        bounds.left,
                        bounds.top,
                        bounds.left,
                        bounds.bottom,
                        edgeColors,
                        edgePositions,
                        Shader.TileMode.CLAMP));

        canvas.save();
        canvas.concat(transform);
        canvas.drawRect(bounds, edgeShadowPaint);
        canvas.restore();
    }

    public void drawCornerShadow(
            @NonNull Canvas canvas,
            @Nullable Matrix matrix,
            @NonNull RectF bounds,
            int elevation,
            float startAngle,
            float sweepAngle) {

        boolean drawShadowInsideBounds = sweepAngle < 0;

        Path arcBounds = scratch;

        if (drawShadowInsideBounds) {
            cornerColors[0] = 0;
            cornerColors[1] = shadowEndColor;
            cornerColors[2] = shadowMiddleColor;
            cornerColors[3] = shadowStartColor;
        } else {
            arcBounds.rewind();
            arcBounds.moveTo(bounds.centerX(), bounds.centerY());
            arcBounds.arcTo(bounds, startAngle, sweepAngle);
            arcBounds.close();

            bounds.inset(-elevation, -elevation);
            cornerColors[0] = 0;
            cornerColors[1] = shadowStartColor;
            cornerColors[2] = shadowMiddleColor;
            cornerColors[3] = shadowEndColor;
        }

        float radius = bounds.width() / 2f;
        if (radius <= 0) {
            return;
        }

        float startRatio = 1f - (elevation / radius);
        float midRatio = startRatio + ((1f - startRatio) / 2f);
        cornerPositions[1] = startRatio;
        cornerPositions[2] = midRatio;
        cornerShadowPaint.setShader(
                new RadialGradient(
                        bounds.centerX(),
                        bounds.centerY(),
                        radius,
                        cornerColors,
                        cornerPositions,
                        Shader.TileMode.CLAMP));

        // TODO(b/117606382): handle oval bounds by scaling the canvas.
        canvas.save();
        canvas.concat(matrix);

        if (!drawShadowInsideBounds) {
            canvas.clipPath(arcBounds, Op.DIFFERENCE);
            // This line is required for the next drawArc to work correctly, I think.
            canvas.drawPath(arcBounds, transparentPaint);
        }

        canvas.drawArc(bounds, startAngle, sweepAngle, true, cornerShadowPaint);
        canvas.restore();
    }

    @NonNull
    public Paint getShadowPaint() {
        return shadowPaint;
    }
}
