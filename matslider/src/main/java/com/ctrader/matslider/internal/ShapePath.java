package com.ctrader.matslider.internal;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


public class ShapePath {

    private static final float ANGLE_UP = 270;
    protected static final float ANGLE_LEFT = 180;

    @Deprecated
    public float startX;
    @Deprecated
    public float startY;
    @Deprecated
    public float endX;
    @Deprecated
    public float endY;
    @Deprecated
    public float currentShadowAngle;
    @Deprecated
    public float endShadowAngle;

    private final List<PathOperation> operations = new ArrayList<>();
    private final List<ShadowCompatOperation> shadowCompatOperations = new ArrayList<>();
    private boolean containsIncompatibleShadowOp;

    public ShapePath() {
        reset(0, 0);
    }


    public void reset(float startX, float startY) {
        reset(startX, startY, ANGLE_UP, 0);
    }

    public void reset(float startX, float startY, float shadowStartAngle, float shadowSweepAngle) {
        setStartX(startX);
        setStartY(startY);
        setEndX(startX);
        setEndY(startY);
        setCurrentShadowAngle(shadowStartAngle);
        setEndShadowAngle((shadowStartAngle + shadowSweepAngle) % 360);
        this.operations.clear();
        this.shadowCompatOperations.clear();
        this.containsIncompatibleShadowOp = false;
    }

    public void lineTo(float x, float y) {
        PathLineOperation operation = new PathLineOperation();
        operation.x = x;
        operation.y = y;
        operations.add(operation);

        LineShadowOperation shadowOperation = new LineShadowOperation(operation, getEndX(), getEndY());

        // The previous endX and endY is the starting point for this shadow operation.
        addShadowCompatOperation(
                shadowOperation,
                ANGLE_UP + shadowOperation.getAngle(),
                ANGLE_UP + shadowOperation.getAngle());

        setEndX(x);
        setEndY(y);
    }





    public void addArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        PathArcOperation operation = new PathArcOperation(left, top, right, bottom);
        operation.setStartAngle(startAngle);
        operation.setSweepAngle(sweepAngle);
        operations.add(operation);

        ArcShadowOperation arcShadowOperation = new ArcShadowOperation(operation);
        float endAngle = startAngle + sweepAngle;
        // Flip the startAngle and endAngle when drawing the shadow inside the bounds. They represent
        // the angles from the center of the circle to the start or end of the arc, respectively. When
        // the shadow is drawn inside the arc, it is going the opposite direction.
        boolean drawShadowInsideBounds = sweepAngle < 0;
        addShadowCompatOperation(
                arcShadowOperation,
                drawShadowInsideBounds ? (180 + startAngle) % 360 : startAngle,
                drawShadowInsideBounds ? (180 + endAngle) % 360 : endAngle);

        setEndX(
                (left + right) * 0.5f
                        + (right - left) / 2 * (float) Math.cos(Math.toRadians(startAngle + sweepAngle)));
        setEndY(
                (top + bottom) * 0.5f
                        + (bottom - top) / 2 * (float) Math.sin(Math.toRadians(startAngle + sweepAngle)));
    }

    public void applyToPath(Matrix transform, Path path) {
        for (int i = 0, size = operations.size(); i < size; i++) {
            PathOperation operation = operations.get(i);
            operation.applyToPath(transform, path);
        }
    }

    @NonNull
    ShadowCompatOperation createShadowCompatOperation(final Matrix transform) {
        // If the shadowCompatOperations don't end on the desired endShadowAngle, add an arc to do so.
        addConnectingShadowIfNecessary(getEndShadowAngle());
        final Matrix transformCopy = new Matrix(transform);
        final List<ShadowCompatOperation> operations = new ArrayList<>(shadowCompatOperations);
        return new ShadowCompatOperation() {
            @Override
            public void draw(
                    Matrix matrix, ShadowRenderer shadowRenderer, int shadowElevation, Canvas canvas) {
                for (ShadowCompatOperation op : operations) {
                    op.draw(transformCopy, shadowRenderer, shadowElevation, canvas);
                }
            }
        };
    }

    private void addShadowCompatOperation(
            ShadowCompatOperation shadowOperation, float startShadowAngle, float endShadowAngle) {
        addConnectingShadowIfNecessary(startShadowAngle);
        shadowCompatOperations.add(shadowOperation);
        setCurrentShadowAngle(endShadowAngle);
    }

    boolean containsIncompatibleShadowOp() {
        return containsIncompatibleShadowOp;
    }

    private void addConnectingShadowIfNecessary(float nextShadowAngle) {
        if (getCurrentShadowAngle() == nextShadowAngle) {
            // Previously drawn shadow lines up with the next shadow, so don't draw anything.
            return;
        }
        float shadowSweep = (nextShadowAngle - getCurrentShadowAngle() + 360) % 360;
        if (shadowSweep > 180) {
            // Shadows are actually overlapping, so don't draw anything.
            return;
        }
        PathArcOperation pathArcOperation =
                new PathArcOperation(getEndX(), getEndY(), getEndX(), getEndY());
        pathArcOperation.setStartAngle(getCurrentShadowAngle());
        pathArcOperation.setSweepAngle(shadowSweep);
        shadowCompatOperations.add(new ArcShadowOperation(pathArcOperation));
        setCurrentShadowAngle(nextShadowAngle);
    }

    float getStartX() {
        return startX;
    }

    float getStartY() {
        return startY;
    }

    float getEndX() {
        return endX;
    }

    float getEndY() {
        return endY;
    }

    private float getCurrentShadowAngle() {
        return currentShadowAngle;
    }

    private float getEndShadowAngle() {
        return endShadowAngle;
    }

    private void setStartX(float startX) {
        this.startX = startX;
    }

    private void setStartY(float startY) {
        this.startY = startY;
    }

    private void setEndX(float endX) {
        this.endX = endX;
    }

    private void setEndY(float endY) {
        this.endY = endY;
    }

    private void setCurrentShadowAngle(float currentShadowAngle) {
        this.currentShadowAngle = currentShadowAngle;
    }

    private void setEndShadowAngle(float endShadowAngle) {
        this.endShadowAngle = endShadowAngle;
    }

    abstract static class ShadowCompatOperation {

        static final Matrix IDENTITY_MATRIX = new Matrix();


        public final void draw(ShadowRenderer shadowRenderer, int shadowElevation, Canvas canvas) {
            draw(IDENTITY_MATRIX, shadowRenderer, shadowElevation, canvas);
        }


        public abstract void draw(
                Matrix transform, ShadowRenderer shadowRenderer, int shadowElevation, Canvas canvas);
    }


    static class LineShadowOperation extends ShadowCompatOperation {

        private final PathLineOperation operation;
        private final float startX;
        private final float startY;

        public LineShadowOperation(PathLineOperation operation, float startX, float startY) {
            this.operation = operation;
            this.startX = startX;
            this.startY = startY;
        }

        @Override
        public void draw(
                Matrix transform,
                @NonNull ShadowRenderer shadowRenderer,
                int shadowElevation,
                @NonNull Canvas canvas) {
            final float height = operation.y - startY;
            final float width = operation.x - startX;
            final RectF rect = new RectF(0, 0, (float) Math.hypot(height, width), 0);
            final Matrix edgeTransform = new Matrix(transform);
            edgeTransform.preTranslate(startX, startY);
            edgeTransform.preRotate(getAngle());
            shadowRenderer.drawEdgeShadow(canvas, edgeTransform, rect, shadowElevation);
        }

        float getAngle() {
            return (float) Math.toDegrees(Math.atan((operation.y - startY) / (operation.x - startX)));
        }
    }


    static class ArcShadowOperation extends ShadowCompatOperation {

        private final PathArcOperation operation;

        public ArcShadowOperation(PathArcOperation operation) {
            this.operation = operation;
        }

        @Override
        public void draw(
                Matrix transform,
                @NonNull ShadowRenderer shadowRenderer,
                int shadowElevation,
                @NonNull Canvas canvas) {
            float startAngle = operation.getStartAngle();
            float sweepAngle = operation.getSweepAngle();
            RectF rect =
                    new RectF(
                            operation.getLeft(), operation.getTop(), operation.getRight(), operation.getBottom());
            shadowRenderer.drawCornerShadow(
                    canvas, transform, rect, shadowElevation, startAngle, sweepAngle);
        }
    }


    public abstract static class PathOperation {

        protected final Matrix matrix = new Matrix();

        public abstract void applyToPath(Matrix transform, Path path);
    }


    public static class PathLineOperation extends PathOperation {
        private float x;
        private float y;

        @Override
        public void applyToPath(@NonNull Matrix transform, @NonNull Path path) {
            Matrix inverse = matrix;
            transform.invert(inverse);
            path.transform(inverse);
            path.lineTo(x, y);
            path.transform(transform);
        }
    }


    public static class PathQuadOperation extends PathOperation {
        @Deprecated
        public float controlX;
        @Deprecated
        public float controlY;
        @Deprecated
        public float endX;
        @Deprecated
        public float endY;

        @Override
        public void applyToPath(@NonNull Matrix transform, @NonNull Path path) {
            Matrix inverse = matrix;
            transform.invert(inverse);
            path.transform(inverse);
            path.quadTo(getControlX(), getControlY(), getEndX(), getEndY());
            path.transform(transform);
        }

        private float getEndX() {
            return endX;
        }


        private float getControlY() {
            return controlY;
        }


        private float getEndY() {
            return endY;
        }


        private float getControlX() {
            return controlX;
        }

    }

    public static class PathArcOperation extends PathOperation {
        private static final RectF rectF = new RectF();

        @Deprecated
        public float left;
        @Deprecated
        public float top;
        @Deprecated
        public float right;
        @Deprecated
        public float bottom;
        @Deprecated
        public float startAngle;
        @Deprecated
        public float sweepAngle;

        public PathArcOperation(float left, float top, float right, float bottom) {
            setLeft(left);
            setTop(top);
            setRight(right);
            setBottom(bottom);
        }

        @Override
        public void applyToPath(@NonNull Matrix transform, @NonNull Path path) {
            Matrix inverse = matrix;
            transform.invert(inverse);
            path.transform(inverse);
            rectF.set(getLeft(), getTop(), getRight(), getBottom());
            path.arcTo(rectF, getStartAngle(), getSweepAngle(), false);
            path.transform(transform);
        }

        private float getLeft() {
            return left;
        }

        private float getTop() {
            return top;
        }

        private float getRight() {
            return right;
        }

        private float getBottom() {
            return bottom;
        }

        private void setLeft(float left) {
            this.left = left;
        }

        private void setTop(float top) {
            this.top = top;
        }

        private void setRight(float right) {
            this.right = right;
        }

        private void setBottom(float bottom) {
            this.bottom = bottom;
        }

        private float getStartAngle() {
            return startAngle;
        }

        private float getSweepAngle() {
            return sweepAngle;
        }

        private void setStartAngle(float startAngle) {
            this.startAngle = startAngle;
        }

        private void setSweepAngle(float sweepAngle) {
            this.sweepAngle = sweepAngle;
        }
    }
}
