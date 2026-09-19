package com.example.rungirlrun.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SpeedometerView extends View {

    public static final double DEFAULT_MAX_SPEED = 100.0;
    public static final double DEFAULT_MAJOR_TICK_STEP = 20.0;
    public static final int DEFAULT_MINOR_TICKS = 1;

    private double maxSpeed = DEFAULT_MAX_SPEED;
    private double speed = 0;

    private double majorTickStep =
            DEFAULT_MAJOR_TICK_STEP;

    private int minorTicks =
            DEFAULT_MINOR_TICKS;

    private LabelConverter labelConverter;

    private final List<ColoredRange> ranges =
            new ArrayList<>();

    private Paint backgroundPaint;
    private Paint innerPaint;
    private Paint ticksPaint;
    private Paint textPaint;
    private Paint rangePaint;
    private Paint needlePaint;

    public SpeedometerView(Context context) {
        super(context);
        init();
    }

    public SpeedometerView(
            Context context,
            AttributeSet attrs
    ) {
        super(context, attrs);
        init();
    }

    public SpeedometerView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        backgroundPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        backgroundPaint.setColor(
                Color.rgb(239, 229, 249)
        );

        backgroundPaint.setStyle(
                Paint.Style.STROKE
        );

        backgroundPaint.setStrokeWidth(22f);

        innerPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        innerPaint.setColor(
                Color.rgb(218, 196, 242)
        );

        innerPaint.setStyle(
                Paint.Style.STROKE
        );

        innerPaint.setStrokeWidth(5f);

        ticksPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        ticksPaint.setColor(
                Color.rgb(105, 75, 145)
        );

        ticksPaint.setStrokeWidth(3f);

        textPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint.setColor(
                Color.rgb(82, 42, 102)
        );

        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rangePaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        rangePaint.setStyle(Paint.Style.STROKE);
        rangePaint.setStrokeWidth(12f);

        needlePaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        needlePaint.setColor(
                Color.rgb(235, 76, 110)
        );

        needlePaint.setStrokeWidth(8f);
        needlePaint.setStrokeCap(
                Paint.Cap.ROUND
        );
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
        invalidate();
    }

    public void setSpeed(double speed) {

        if (speed < 0) {
            speed = 0;
        }

        if (speed > maxSpeed) {
            speed = maxSpeed;
        }

        this.speed = speed;
        invalidate();
    }

    public void setMajorTickStep(
            double majorTickStep
    ) {
        this.majorTickStep = majorTickStep;
        invalidate();
    }

    public void setMinorTicks(int minorTicks) {
        this.minorTicks = minorTicks;
        invalidate();
    }

    public void setLabelConverter(
            LabelConverter converter
    ) {
        this.labelConverter = converter;
        invalidate();
    }

    public void addColoredRange(
            double begin,
            double end,
            int color
    ) {
        ranges.add(
                new ColoredRange(
                        color,
                        begin,
                        end
                )
        );

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float padding = 45f;

        RectF oval = new RectF(
                padding,
                padding,
                width - padding,
                height * 2 - padding
        );

        canvas.drawArc(
                oval,
                180,
                180,
                false,
                backgroundPaint
        );

        RectF innerOval = new RectF(
                padding + 15,
                padding + 15,
                width - padding - 15,
                height * 2 - padding - 15
        );

        canvas.drawArc(
                innerOval,
                180,
                180,
                false,
                innerPaint
        );

        drawColoredRanges(canvas, oval);
        drawTicks(canvas, oval);
        drawNeedle(canvas, oval);
    }

    private void drawColoredRanges(
            Canvas canvas,
            RectF oval
    ) {

        for (ColoredRange range : ranges) {

            rangePaint.setColor(range.color);

            float startAngle =
                    180 +
                            (float)
                                    (range.begin /
                                            maxSpeed *
                                            180);

            float sweepAngle =
                    (float)
                            ((range.end - range.begin)
                                    / maxSpeed
                                    * 180);

            canvas.drawArc(
                    oval,
                    startAngle,
                    sweepAngle,
                    false,
                    rangePaint
            );
        }
    }

    private void drawTicks(
            Canvas canvas,
            RectF oval
    ) {

        if (majorTickStep <= 0) {
            return;
        }

        float centerX = oval.centerX();
        float centerY = oval.centerY();

        float radius =
                oval.width() / 2f;

        for (
                double value = 0;
                value <= maxSpeed;
                value += majorTickStep
        ) {

            double angle =
                    Math.toRadians(
                            180 +
                                    value /
                                            maxSpeed *
                                            180
                    );

            float outerX =
                    centerX +
                            (float) Math.cos(angle)
                                    * radius;

            float outerY =
                    centerY +
                            (float) Math.sin(angle)
                                    * radius;

            float innerX =
                    centerX +
                            (float) Math.cos(angle)
                                    * (radius - 22);

            float innerY =
                    centerY +
                            (float) Math.sin(angle)
                                    * (radius - 22);

            canvas.drawLine(
                    innerX,
                    innerY,
                    outerX,
                    outerY,
                    ticksPaint
            );

            if (labelConverter != null) {

                String label =
                        labelConverter
                                .getLabelFor(
                                        value,
                                        maxSpeed
                                );

                float labelX =
                        centerX +
                                (float) Math.cos(angle)
                                        * (radius - 52);

                float labelY =
                        centerY +
                                (float) Math.sin(angle)
                                        * (radius - 52)
                                + 10;

                canvas.drawText(
                        label,
                        labelX,
                        labelY,
                        textPaint
                );
            }
        }
    }

    private void drawNeedle(
            Canvas canvas,
            RectF oval
    ) {

        float centerX = oval.centerX();
        float centerY = oval.centerY();

        float radius =
                oval.width() / 2f - 65;

        double angle =
                Math.toRadians(
                        180 +
                                speed /
                                        maxSpeed *
                                        180
                );

        float needleX =
                centerX +
                        (float) Math.cos(angle)
                                * radius;

        float needleY =
                centerY +
                        (float) Math.sin(angle)
                                * radius;

        canvas.drawLine(
                centerX,
                centerY,
                needleX,
                needleY,
                needlePaint
        );

        canvas.drawCircle(
                centerX,
                centerY,
                13,
                needlePaint
        );
    }

    public interface LabelConverter {

        String getLabelFor(
                double progress,
                double maxProgress
        );
    }

    private static class ColoredRange {

        private final int color;
        private final double begin;
        private final double end;

        ColoredRange(
                int color,
                double begin,
                double end
        ) {
            this.color = color;
            this.begin = begin;
            this.end = end;
        }
    }
}