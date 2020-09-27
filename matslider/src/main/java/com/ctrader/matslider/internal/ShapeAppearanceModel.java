package com.ctrader.matslider.internal;

import android.graphics.RectF;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;

public class ShapeAppearanceModel {

    public static final class Builder {

        @NonNull
        private RoundedCornerTreatment topLeftCorner = new RoundedCornerTreatment();

        @NonNull
        private RoundedCornerTreatment topRightCorner = new RoundedCornerTreatment();

        @NonNull
        private RoundedCornerTreatment bottomRightCorner = new RoundedCornerTreatment();

        @NonNull
        private RoundedCornerTreatment bottomLeftCorner = new RoundedCornerTreatment();

        @NonNull
        private CornerSize topLeftCornerSize = new AbsoluteCornerSize(0);
        @NonNull
        private CornerSize topRightCornerSize = new AbsoluteCornerSize(0);
        @NonNull
        private CornerSize bottomRightCornerSize = new AbsoluteCornerSize(0);
        @NonNull
        private CornerSize bottomLeftCornerSize = new AbsoluteCornerSize(0);

        @NonNull
        private EdgeTreatment topEdge = new EdgeTreatment();
        @NonNull
        private EdgeTreatment rightEdge = new EdgeTreatment();
        @NonNull
        private EdgeTreatment bottomEdge = new EdgeTreatment();
        @NonNull
        private EdgeTreatment leftEdge = new EdgeTreatment();

        public Builder() {
        }

        public Builder(@NonNull ShapeAppearanceModel other) {
            topLeftCorner = other.topLeftCorner;
            topRightCorner = other.topRightCorner;
            bottomRightCorner = other.bottomRightCorner;
            bottomLeftCorner = other.bottomLeftCorner;

            topLeftCornerSize = other.topLeftCornerSize;
            topRightCornerSize = other.topRightCornerSize;
            bottomRightCornerSize = other.bottomRightCornerSize;
            bottomLeftCornerSize = other.bottomLeftCornerSize;

            topEdge = other.topEdge;
            rightEdge = other.rightEdge;
            bottomEdge = other.bottomEdge;
            leftEdge = other.leftEdge;
        }

        @NonNull
        public Builder setAllCorners(@Dimension float cornerSize) {

            return setAllCorners(new RoundedCornerTreatment())
                    .setAllCornerSizes(cornerSize);
        }

        @NonNull
        public Builder setAllCorners(@NonNull RoundedCornerTreatment cornerTreatment) {
            return setTopLeftCorner(cornerTreatment)
                    .setTopRightCorner(cornerTreatment)
                    .setBottomRightCorner(cornerTreatment)
                    .setBottomLeftCorner(cornerTreatment);
        }

        @NonNull
        public Builder setAllCornerSizes(@Dimension float cornerSize) {
            return setTopLeftCornerSize(cornerSize)
                    .setTopRightCornerSize(cornerSize)
                    .setBottomRightCornerSize(cornerSize)
                    .setBottomLeftCornerSize(cornerSize);
        }

        @NonNull
        public Builder setTopLeftCornerSize(@Dimension float cornerSize) {
            topLeftCornerSize = new AbsoluteCornerSize(cornerSize);
            return this;
        }

        @NonNull
        public Builder setTopLeftCornerSize(@NonNull CornerSize cornerSize) {
            topLeftCornerSize = cornerSize;
            return this;
        }

        @NonNull
        public Builder setTopRightCornerSize(@Dimension float cornerSize) {
            topRightCornerSize = new AbsoluteCornerSize(cornerSize);
            return this;
        }

        @NonNull
        public Builder setTopRightCornerSize(@NonNull CornerSize cornerSize) {
            topRightCornerSize = cornerSize;
            return this;
        }

        @NonNull
        public Builder setBottomRightCornerSize(@Dimension float cornerSize) {
            bottomRightCornerSize = new AbsoluteCornerSize(cornerSize);
            return this;
        }

        @NonNull
        public Builder setBottomRightCornerSize(@NonNull CornerSize cornerSize) {
            bottomRightCornerSize = cornerSize;
            return this;
        }

        @NonNull
        public Builder setBottomLeftCornerSize(@Dimension float cornerSize) {
            bottomLeftCornerSize = new AbsoluteCornerSize(cornerSize);
            return this;
        }

        @NonNull
        public Builder setBottomLeftCornerSize(@NonNull CornerSize cornerSize) {
            bottomLeftCornerSize = cornerSize;
            return this;
        }

        @NonNull
        public Builder setTopLeftCorner(@NonNull RoundedCornerTreatment topLeftCorner) {
            this.topLeftCorner = topLeftCorner;
            float size = compatCornerTreatmentSize(topLeftCorner);
            if (size != -1) {
                setTopLeftCornerSize(size);
            }
            return this;
        }

        @NonNull
        public Builder setTopRightCorner(@NonNull RoundedCornerTreatment topRightCorner) {
            this.topRightCorner = topRightCorner;
            float size = compatCornerTreatmentSize(topRightCorner);
            if (size != -1) {
                setTopRightCornerSize(size);
            }
            return this;
        }

        @NonNull
        public Builder setBottomRightCorner(@NonNull RoundedCornerTreatment bottomRightCorner) {
            this.bottomRightCorner = bottomRightCorner;
            float size = compatCornerTreatmentSize(bottomRightCorner);
            if (size != -1) {
                setBottomRightCornerSize(size);
            }
            return this;
        }

        @NonNull
        public Builder setBottomLeftCorner(@NonNull RoundedCornerTreatment bottomLeftCorner) {
            this.bottomLeftCorner = bottomLeftCorner;
            float size = compatCornerTreatmentSize(bottomLeftCorner);
            if (size != -1) {
                setBottomLeftCornerSize(size);
            }
            return this;
        }

        private static float compatCornerTreatmentSize(RoundedCornerTreatment treatment) {
            return treatment.radius;

        }

        @NonNull
        public ShapeAppearanceModel build() {
            return new ShapeAppearanceModel(this);
        }
    }

    @NonNull
    public static Builder builder() {
        return new ShapeAppearanceModel.Builder();
    }

    RoundedCornerTreatment topLeftCorner;
    RoundedCornerTreatment topRightCorner;
    RoundedCornerTreatment bottomRightCorner;
    RoundedCornerTreatment bottomLeftCorner;
    CornerSize topLeftCornerSize;
    CornerSize topRightCornerSize;
    CornerSize bottomRightCornerSize;
    CornerSize bottomLeftCornerSize;
    EdgeTreatment topEdge;
    EdgeTreatment rightEdge;
    EdgeTreatment bottomEdge;
    EdgeTreatment leftEdge;

    private ShapeAppearanceModel(@NonNull ShapeAppearanceModel.Builder builder) {
        topLeftCorner = builder.topLeftCorner;
        topRightCorner = builder.topRightCorner;
        bottomRightCorner = builder.bottomRightCorner;
        bottomLeftCorner = builder.bottomLeftCorner;

        topLeftCornerSize = builder.topLeftCornerSize;
        topRightCornerSize = builder.topRightCornerSize;
        bottomRightCornerSize = builder.bottomRightCornerSize;
        bottomLeftCornerSize = builder.bottomLeftCornerSize;

        topEdge = builder.topEdge;
        rightEdge = builder.rightEdge;
        bottomEdge = builder.bottomEdge;
        leftEdge = builder.leftEdge;
    }

    public ShapeAppearanceModel() {

        topLeftCorner = new RoundedCornerTreatment();

        topRightCorner = new RoundedCornerTreatment();

        bottomRightCorner = new RoundedCornerTreatment();

        bottomLeftCorner = new RoundedCornerTreatment();

        topLeftCornerSize = new AbsoluteCornerSize(0);
        topRightCornerSize = new AbsoluteCornerSize(0);
        bottomRightCornerSize = new AbsoluteCornerSize(0);
        bottomLeftCornerSize = new AbsoluteCornerSize(0);

        topEdge = new EdgeTreatment();
        rightEdge = new EdgeTreatment();
        bottomEdge = new EdgeTreatment();
        leftEdge = new EdgeTreatment();
    }

    @NonNull
    public RoundedCornerTreatment getTopLeftCorner() {
        return topLeftCorner;
    }

    @NonNull
    public RoundedCornerTreatment getTopRightCorner() {
        return topRightCorner;
    }

    @NonNull
    public RoundedCornerTreatment getBottomRightCorner() {
        return bottomRightCorner;
    }

    @NonNull
    public RoundedCornerTreatment getBottomLeftCorner() {
        return bottomLeftCorner;
    }

    @NonNull
    public CornerSize getTopLeftCornerSize() {
        return topLeftCornerSize;
    }

    @NonNull
    public CornerSize getTopRightCornerSize() {
        return topRightCornerSize;
    }

    @NonNull
    public CornerSize getBottomRightCornerSize() {
        return bottomRightCornerSize;
    }

    @NonNull
    public CornerSize getBottomLeftCornerSize() {
        return bottomLeftCornerSize;
    }

    @NonNull
    public EdgeTreatment getLeftEdge() {
        return leftEdge;
    }

    @NonNull
    public EdgeTreatment getTopEdge() {
        return topEdge;
    }

    @NonNull
    public EdgeTreatment getRightEdge() {
        return rightEdge;
    }

    @NonNull
    public EdgeTreatment getBottomEdge() {
        return bottomEdge;
    }

    @NonNull
    public Builder toBuilder() {
        return new Builder(this);
    }

    public interface CornerSizeUnaryOperator {
        @NonNull
        CornerSize apply(@NonNull CornerSize cornerSize);
    }

    @NonNull
    public ShapeAppearanceModel withTransformedCornerSizes(@NonNull CornerSizeUnaryOperator op) {
        return toBuilder()
                .setTopLeftCornerSize(op.apply(getTopLeftCornerSize()))
                .setTopRightCornerSize(op.apply(getTopRightCornerSize()))
                .setBottomLeftCornerSize(op.apply(getBottomLeftCornerSize()))
                .setBottomRightCornerSize(op.apply(getBottomRightCornerSize()))
                .build();
    }

    public boolean isRoundRect(@NonNull RectF bounds) {
        boolean hasDefaultEdges =
                leftEdge.getClass().equals(EdgeTreatment.class)
                        && rightEdge.getClass().equals(EdgeTreatment.class)
                        && topEdge.getClass().equals(EdgeTreatment.class)
                        && bottomEdge.getClass().equals(EdgeTreatment.class);

        float cornerSize = topLeftCornerSize.getCornerSize(bounds);

        boolean cornersHaveSameSize =
                topRightCornerSize.getCornerSize(bounds) == cornerSize
                        && bottomLeftCornerSize.getCornerSize(bounds) == cornerSize
                        && bottomRightCornerSize.getCornerSize(bounds) == cornerSize;

        return hasDefaultEdges && cornersHaveSameSize;
    }
}
