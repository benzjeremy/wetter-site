package com.benzjeremy.wetter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import java.util.Collections;
import java.util.List;

/** A touch- and keyboard-operated time axis, backed by Android's native slider. */
public class HourlyChart extends SeekBar {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path curve = new Path();
    private List<WeatherData.HourlyItem> hours = Collections.emptyList();
    private double low;
    private double high;

    public HourlyChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        setPadding(dp(12), 0, dp(12), 0);
        setEnabled(false);
    }

    public void showHours(List<WeatherData.HourlyItem> forecast) {
        hours = forecast == null ? Collections.emptyList() : forecast;
        low = Double.POSITIVE_INFINITY;
        high = Double.NEGATIVE_INFINITY;
        for (WeatherData.HourlyItem hour : hours) {
            low = Math.min(low, hour.temp);
            high = Math.max(high, hour.temp);
        }
        setMax(Math.max(0, hours.size() - 1));
        setKeyProgressIncrement(1);
        setEnabled(hours.size() > 1);
        invalidate();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private float x(int i) {
        return getPaddingLeft() + i * (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) Math.max(1, hours.size() - 1);
    }

    private float y(int i) {
        double span = Math.max(2, high - low);
        double position = .5 + (hours.get(i).temp - (high + low) / 2) / span;
        return dp(12) + (float) ((1 - position) * (getHeight() - dp(48)));
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (hours.isEmpty()) return;
        int last = hours.size() - 1;
        int selected = Math.min(getProgress(), last);
        int ink = getResources().getColor(R.color.accent_sky);
        float floor = getHeight() - dp(24);
        curve.reset();
        curve.moveTo(x(0), y(0));
        for (int i = 1; i <= last; i++) curve.lineTo(x(i), y(i));
        paint.setColor(ink);
        paint.setStrokeWidth(dp(3));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(curve, paint);
        curve.lineTo(x(last), floor);
        curve.lineTo(x(0), floor);
        curve.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(28);
        canvas.drawPath(curve, paint);
        paint.setAlpha(255);
        paint.setStrokeWidth(dp(1));
        canvas.drawLine(x(selected), y(selected), x(selected), floor, paint);
        canvas.drawCircle(x(selected), y(selected), dp(5), paint);
        paint.setColor(getResources().getColor(R.color.text_secondary));
        paint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        float baseline = getHeight() - dp(4);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(hours.get(0).time, x(0), baseline, paint);
        if (last > 2) {
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(hours.get(last / 2).time, x(last / 2), baseline, paint);
        }
        if (last > 0) {
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(hours.get(last).time, x(last), baseline, paint);
        }
    }
}
