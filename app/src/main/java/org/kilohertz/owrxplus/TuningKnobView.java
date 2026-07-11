package org.kilohertz.owrxplus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
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
    private float indicatorAngle = -90f;
    private String stepLabel = "step";

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

    public void setStepLabel(String stepLabel) {
        if (stepLabel == null || stepLabel.trim().length() == 0) {
            this.stepLabel = "step";
        } else {
            this.stepLabel = stepLabel.trim();
        }
        invalidate();
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
        paint.setShader(new RadialGradient(
                cx,
                cy,
                radius * 1.18f,
                new int[]{0xFF232832, 0xFF0C1118, 0xFF05070A},
                new float[]{0f, 0.72f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius + dp(5), paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xFF3E4953);
        for (int i = 0; i < 96; i++) {
            double angle = Math.toRadians(i * 3.75);
            float outer = radius + dp(5);
            float inner = radius - (i % 2 == 0 ? dp(7) : dp(4));
            canvas.drawLine(
                    cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer,
                    paint
            );
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new SweepGradient(
                cx,
                cy,
                new int[]{
                        0xFF0B0D10,
                        0xFF565D63,
                        0xFF15181D,
                        0xFF8A9094,
                        0xFF101318,
                        0xFF30363D,
                        0xFF0B0D10
                },
                new float[]{0f, .14f, .31f, .48f, .64f, .82f, 1f}
        ));
        canvas.drawCircle(cx, cy, radius - dp(7), paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(
                cx - radius * .22f,
                cy - radius * .28f,
                radius * .95f,
                new int[]{0x99FFFFFF, 0x332B3238, 0x002B3238},
                new float[]{0f, .38f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius - dp(8), paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0x333D444B);
        for (int i = 0; i < 72; i++) {
            double angle = Math.toRadians(i * 5);
            canvas.drawLine(
                    cx,
                    cy,
                    cx + (float) Math.cos(angle) * (radius - dp(11)),
                    cy + (float) Math.sin(angle) * (radius - dp(11)),
                    paint
            );
        }

        paint.setStrokeWidth(dp(8));
        paint.setColor(0xFFBDEFFF);
        arc.set(cx - radius + dp(13), cy - radius + dp(13), cx + radius - dp(13), cy + radius - dp(13));
        canvas.drawArc(arc, 218, 104, false, paint);

        double indicatorRadians = Math.toRadians(indicatorAngle);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(
                cx + (float) Math.cos(indicatorRadians) * (radius - dp(18)),
                cy + (float) Math.sin(indicatorRadians) * (radius - dp(18)),
                dp(4),
                paint
        );

        paint.setStrokeWidth(dp(2));
        paint.setColor(0xB6D6E8EF);
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
        paint.setShader(new RadialGradient(
                cx - dp(5),
                cy - dp(6),
                radius * .36f,
                new int[]{0xFF444C54, 0xFF11161C, 0xFF030405},
                new float[]{0f, .62f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius * 0.42f, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xCC9FEAFF);
        canvas.drawCircle(cx, cy, radius * 0.42f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(
                cx - dp(18),
                cy - dp(18),
                cx + dp(18),
                cy + dp(18),
                0xFF111418,
                0xFF050608,
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius * 0.18f, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(dp(14));
        canvas.drawText("TUNE", cx, cy - dp(9), paint);
        paint.setColor(0xFFBDEFFF);
        paint.setTextSize(dp(12));
        canvas.drawText(stepLabel, cx, cy + dp(10), paint);
        paint.setTextSize(dp(9));
        canvas.drawText("tap", cx, cy + dp(25), paint);
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
        indicatorAngle = normalizeIndicator(indicatorAngle + direction * 14f);
        invalidate();
        performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        if (listener != null) {
            listener.onTick(direction);
        }
    }

    private float normalizeIndicator(float angle) {
        while (angle > 180) {
            angle -= 360;
        }
        while (angle < -180) {
            angle += 360;
        }
        return angle;
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
