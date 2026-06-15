package org.kilohertz.owrxplus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TuningKnobView extends View {
    private static final float DEGREES_PER_TICK = 14f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private Listener listener;
    private float lastAngle;
    private float accumulator;
    private float touchDownX;
    private float touchDownY;
    private long touchDownTime;

    public interface Listener {
        void onTick(int direction);

        void onCenterTap();
    }

    public TuningKnobView(Context context) {
        super(context);
        init();
    }

    public TuningKnobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void init() {
        setFocusable(true);
        setClickable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desired = dp(164);
        int size = Math.min(width, desired);
        if (size <= 0) {
            size = desired;
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(width, height) * 0.44f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xEE101820);
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0xFF2D5360);
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setStrokeWidth(dp(7));
        paint.setColor(0xFF31D27C);
        arc.set(cx - radius + dp(12), cy - radius + dp(12), cx + radius - dp(12), cy + radius - dp(12));
        canvas.drawArc(arc, 215, 110, false, paint);

        paint.setStrokeWidth(dp(2));
        paint.setColor(0x99FFFFFF);
        for (int i = -5; i <= 5; i++) {
            double angle = Math.toRadians(-90 + i * 18);
            float outer = radius - dp(4);
            float inner = radius - (i == 0 ? dp(24) : dp(16));
            canvas.drawLine(
                    cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer,
                    paint
            );
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0A1217);
        canvas.drawCircle(cx, cy, radius * 0.46f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xAA31D27C);
        canvas.drawCircle(cx, cy, radius * 0.46f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(dp(15));
        canvas.drawText("TUNE", cx, cy - dp(4), paint);
        paint.setColor(0xFFB7E4CE);
        paint.setTextSize(dp(10));
        canvas.drawText("tap step", cx, cy + dp(14), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = x;
                touchDownY = y;
                touchDownTime = SystemClock.uptimeMillis();
                lastAngle = angleFor(x, y);
                accumulator = 0;
                return true;
            case MotionEvent.ACTION_MOVE:
                float angle = angleFor(x, y);
                float delta = normalizeDelta(angle - lastAngle);
                lastAngle = angle;
                accumulator += delta;
                while (accumulator >= DEGREES_PER_TICK) {
                    accumulator -= DEGREES_PER_TICK;
                    tick(1);
                }
                while (accumulator <= -DEGREES_PER_TICK) {
                    accumulator += DEGREES_PER_TICK;
                    tick(-1);
                }
                return true;
            case MotionEvent.ACTION_UP:
                float dx = x - touchDownX;
                float dy = y - touchDownY;
                boolean shortTap = SystemClock.uptimeMillis() - touchDownTime < 350;
                boolean smallMove = Math.sqrt(dx * dx + dy * dy) < dp(14);
                boolean center = distanceFromCenter(x, y) < Math.min(getWidth(), getHeight()) * 0.26f;
                if (shortTap && smallMove && center && listener != null) {
                    performClick();
                    listener.onCenterTap();
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void tick(int direction) {
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        if (listener != null) {
            listener.onTick(direction);
        }
    }

    private float angleFor(float x, float y) {
        return (float) Math.toDegrees(Math.atan2(y - getHeight() / 2f, x - getWidth() / 2f));
    }

    private float normalizeDelta(float delta) {
        while (delta > 180) {
            delta -= 360;
        }
        while (delta < -180) {
            delta += 360;
        }
        return delta;
    }

    private float distanceFromCenter(float x, float y) {
        float dx = x - getWidth() / 2f;
        float dy = y - getHeight() / 2f;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
