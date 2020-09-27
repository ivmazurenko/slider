package com.ctrader.matslider.internal;

import android.graphics.RectF;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;

public class ShapeAppearanceModel {

    public static final class Builder {

        @NonNull
        private CornerTreatment topLeftCorner = MaterialShapeUtils.createDefaultCornerTreatment();

        @NonNull
        private CornerTreatment topRightCorner = MaterialShapeUtils.createDefaultCornerTreatment();

        @NonNull
        private CornerTreatment bottomRightCorner = MaterialShapeUtils.createDefaultCornerTreatment();

        @NonNull
        private CornerTreatment bottomLeftCorner = MaterialShapeUtils.createDefaultCornerTreatment();

        @NonNull
        private CornerSize topLeftCornerSize = new AbsoluteCornerSize(0);
        @NonNull
        private CornerSize topRightCornerSize = new AbsoluteCornerSize(0);
        @NonNull
        private CornerSize bottomRightCornerSize = new AbsoluteCornerSize(0);
        @NonNull
        private CornerSize bottomLeftCornerSize = new AbsoluteCornerSize(0);

        @NonNull
        private EdgeTreatment topEdge = MaterialShapeUtils.createDefaultEdgeTreatment();
        @NonNull
        private EdgeTreatment rightEdge = MaterialShapeUtils.createDefaultEdgeTreatment();
        @NonNull
        private EdgeTreatment bottomEdge = MaterialShapeUtils.createDefaultEdgeTreatment();
        @NonNull
        private EdgeTreatment leftEdge = MaterialShapeUtils.createDefaultEdgeTreatment();

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
        public Builder setAllCorners(@CornerFamily int cornerFamily, @Dimension float cornerSize) {
            return setAllCorners(MaterialShapeUtils.createCornerTreatment(cornerFamily))
                    .setAllCornerSizes(cornerSize);
        }

        @NonNull
        public Builder setAllCorners(@NonNull CornerTreatment cornerTreatment) {
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
        public Builder setTopLeftCorner(@NonNull CornerTreatment topLeftCorner) {
            this.topLeftCorner = topLeftCorner;
            // For backwards compatibility, set the size from the treatment if it exists
            float size = compatCornerTreatmentSize(topLeftCorner);
            if (size != -1) {
                setTopLeftCornerSize(size);
            }
            return this;
        }

        @NonNull
        public Builder setTopRightCorner(@NonNull CornerTreatment topRightCorner) {
            this.topRightCorner = topRightCorner;
            // For backwards compatibility, set the size from the treatment if it exists
            float size = compatCornerTreatmentSize(topRightCorner);
            if (size != -1) {
                setTopRightCornerSize(size);
            }
            return this;
        }

        @NonNull
        public Builder setBottomRightCorner(@NonNull CornerTreatment bottomRightCorner) {
            this.bottomRightCorner = bottomRightCorner;
            // For backwards compatibility, set the size from the treatment if it exists
            float size = compatCornerTreatmentSize(bottomRightCorner);
            if (size != -1) {
                setBottomRightCornerSize(size);
            }
            return this;
        }

        @NonNull
        public Builder setBottomLeftCorner(@NonNull CornerTreatment bottomLeftCorner) {
            this.bottomLeftCorner = bottomLeftCorner;
            // For backwards compatibility, set the size from the treatment if it exists
            float size = compatCornerTreatmentSize(bottomLeftCorner);
            if (size != -1) {
                setBottomLeftCornerSize(size);
            }
            return this;
        }

        private static float compatCornerTreatmentSize(CornerTreatment treatment) {
            if (treatment instanceof RoundedCornerTreatment) {
                return ((RoundedCornerTreatment) treatment).radius;
            } else if (treatment instanceof CutCornerTreatment) {
                return ((CutCornerTreatment) treatment).size;
            }
            return -1;
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

    CornerTreatment topLeftCorner;
    CornerTreatment topRightCorner;
    CornerTreatment bottomRightCorner;
    CornerTreatment bottomLeftCorner;
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
        topLeftCorner = MaterialShapeUtils.createDefaultCornerTreatment();
        topRightCorner = MaterialShapeUtils.createDefaultCornerTreatment();
        bottomRightCorner = MaterialShapeUtils.createDefaultCornerTreatment();
        bottomLeftCorner = MaterialShapeUtils.createDefaultCornerTreatment();

        topLeftCornerSize = new AbsoluteCornerSize(0);
        topRightCornerSize = new AbsoluteCornerSize(0);
        bottomRightCornerSize = new AbsoluteCornerSize(0);
        bottomLeftCornerSize = new AbsoluteCornerSize(0);

        topEdge = MaterialShapeUtils.createDefaultEdgeTreatment();
        rightEdge = MaterialShapeUtils.createDefaultEdgeTreatment();
        bottomEdge = MaterialShapeUtils.createDefaultEdgeTreatment();
        leftEdge = MaterialShapeUtils.createDefaultEdgeTreatment();
    }

    @NonNull
    public CornerTreatment getTopLeftCorner() {
        return topLeftCorner;
    }

    @NonNull
    public CornerTreatment getTopRightCorner() {
        return topRightCorner;
    }

    @NonNull
    public CornerTreatment getBottomRightCorner() {
        return bottomRightCorner;
    }

    @NonNull
    public CornerTreatment getBottomLeftCorner() {
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

        boolean hasRoundedCorners =
                topRightCorner instanceof RoundedCornerTreatment
                        && topLeftCorner instanceof RoundedCornerTreatment
                        && bottomRightCorner instanceof RoundedCornerTreatment
                        && bottomLeftCorner instanceof RoundedCornerTreatment;

        return hasDefaultEdges && cornersHaveSameSize && hasRoundedCorners;
    }
}
