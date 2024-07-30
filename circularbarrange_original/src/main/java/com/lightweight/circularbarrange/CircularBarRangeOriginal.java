package com.lightweight.circularbarrange;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Hasan on 24. 7. 25.
 * Email: hasan@kaist.ac.kr
 * Desc:
 */
public class CircularBarRangeOriginal extends View {
    private final String TAG = this.getClass().getSimpleName();
    private static final double EPSION = 0.01;

    private int mThumbStartX;
    private int mThumbStartY;

    private int mThumbEndX;
    private int mThumbEndY;

    private int mCircleCenterX;
    private int mCircleCenterY;
    private int mCircleRadius;

    private int mPadding;
    private int mBorderColor;
    private int mMarkColor;
    private int mBorderThickness;
    private int mArcDashSize;
    private int mArcColor;
    private LineCap mLineCap;
    private double mAngle;
    private double mAngleEnd;
    private int mTouchExtension;
    public enum LineCap {
        BUTT(0),
        ROUND(1),
        SQUARE(2);

        int id;

        LineCap(int id) {
            this.id = id;
        }

        static LineCap fromId(int id) {
            for (LineCap lc : values()) {
                if (lc.id == id) return lc;
            }
            throw new IllegalArgumentException();
        }

        public Paint.Cap getPaintCap() {
            switch (this) {
                case BUTT:
                default:
                    return Paint.Cap.BUTT;
                case ROUND:
                    return Paint.Cap.ROUND;
                case SQUARE:
                    return Paint.Cap.SQUARE;
            }
        }

    }

    private Paint mPaint = new Paint();
    private Paint mLinePaint = new Paint();

    private RectF arcRectF = new RectF();
    private Rect arcRect = new Rect();


    public CircularBarRangeOriginal(Context context) {
        this(context, null);
    }

    public CircularBarRangeOriginal(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularBarRangeOriginal(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircularBarRangeOriginal(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularBar, defStyleAttr, 0);

        // read all available attributes
        float startAngle = a.getFloat(R.styleable.CircularBar_start_angle, 90);
        float endAngle = a.getFloat(R.styleable.CircularBar_end_angle, 60);
        int borderThickness = a.getDimensionPixelSize(R.styleable.CircularBar_border_thickness, 20);
        int arcDashSize = a.getDimensionPixelSize(R.styleable.CircularBar_arc_dash_size, 60);
        int arcColor = a.getColor(R.styleable.CircularBar_arc_color, 0);
        int borderColor = a.getColor(R.styleable.CircularBar_border_color, Color.RED);
        int touchExtensionSize = a.getDimensionPixelSize(R.styleable.CircularBar_touch_extension, 0);
        LineCap lineCap = LineCap.fromId(a.getInt(R.styleable.CircularBar_line_cap, 0));

        // save those to fields (really, do we need setters here..?)
        setStartAngle(startAngle);
        setEndAngle(endAngle);
        setBorderThickness(borderThickness);
        setBorderColor(borderColor);
        setArcColor(arcColor);
        setArcDashSize(arcDashSize);
        setLineCap(lineCap);
        setTouchExtension(touchExtensionSize);

        Log.e(TAG, "Init done");

        // assign padding - check for version because of RTL layout compatibility
        int padding;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int all = getPaddingLeft() + getPaddingRight() + getPaddingBottom() + getPaddingTop() + getPaddingEnd() + getPaddingStart();
            padding = all / 6;
        } else {
            padding = (getPaddingLeft() + getPaddingRight() + getPaddingBottom() + getPaddingTop()) / 4;

        }
        setPadding(padding);
        a.recycle();


        if (isInEditMode())
            return;
    }

    public void setStartAngle(double startAngle) {
        mAngle = fromDrawingAngle(startAngle);
    }

    public void setEndAngle(double angle) {
        mAngleEnd = fromDrawingAngle(angle);
    }

    public void setBorderThickness(int circleBorderThickness) {
        mBorderThickness = circleBorderThickness;
    }

    public void setBorderColor(int color) {
        mBorderColor = color;
    }

    public void setArcColor(int color) {
        mArcColor = color;
    }

    public void setArcDashSize(int value) {
        mArcDashSize = value;
    }

    public void setLineCap(LineCap value) { mLineCap = value; }

    public void setTouchExtension(int size) {
        mTouchExtension = size;
    }

    public void setPadding(int padding) {
        mPadding = padding;
    }

    public double fromDrawingAngle(double angleInDe1grees) {
        double radians = Math.toRadians(angleInDe1grees);
        return -radians;
    }

    public float toDrawingAngle(double angleInRadians) {
        double fixedAngle = Math.toDegrees(angleInRadians);
        if (angleInRadians > 0)
            fixedAngle = 360 - fixedAngle;
        else
            fixedAngle = -fixedAngle;
        return (float) fixedAngle;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // use smaller dimension for calculations (depends on parent size)
        int smallerDim = w > h ? h : w;

        // find circle's rectangle points
        int largestCenteredSquareLeft = (w - smallerDim) / 2;
        int largestCenteredSquareTop = (h - smallerDim) / 2;
        int largestCenteredSquareRight = largestCenteredSquareLeft + smallerDim;
        int largestCenteredSquareBottom = largestCenteredSquareTop + smallerDim;

        // save circle coordinates and radius in fields
        mCircleCenterX = largestCenteredSquareRight / 2 + (w - largestCenteredSquareRight) / 2;
        mCircleCenterY = largestCenteredSquareBottom / 2 + (h - largestCenteredSquareBottom) / 2;
        mCircleRadius = smallerDim / 2 - mBorderThickness / 2 - mPadding;

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // outer circle (ring)
        mPaint.setColor(mBorderColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mBorderThickness);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(mLineCap.getPaintCap());


        mLinePaint.setColor(mArcColor == 0 ? Color.RED : mArcColor);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(mArcDashSize);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setTextSize(50);
        mLinePaint.setStrokeCap(mLineCap.getPaintCap());

        arcRect.set(mCircleCenterX - mCircleRadius, mCircleCenterY + mCircleRadius, mCircleCenterX + mCircleRadius, mCircleCenterY - mCircleRadius);
        arcRectF.set(arcRect);
        arcRectF.sort();

        final float drawStartLeft = toDrawingAngle(mAngle);     // start of both arc and border for left bars
        final float delta = drawStartLeft-90.0f;        // decides gap in the lower part between two bars
        final float theta = toDrawingAngle(mAngleEnd) - drawStartLeft;      // span angle
        final float drawStartRight = 90.0f-delta;       // start of both arc and border for right bars

        mPaint.setColor(mBorderColor);
        mPaint.setShader(null);

        Path p;
        p = new Path();
        p.arcTo(arcRectF, drawStartLeft, 180.0f-2*delta);
        canvas.drawPath(p,mPaint);

        p = new Path();
        p.arcTo(arcRectF, drawStartRight, -1*(180.0f-2*delta));
        canvas.drawPath(p,mPaint);

        p = new Path();
        p.arcTo(arcRectF, drawStartLeft, theta);
        canvas.drawPath(p,mLinePaint);

        p = new Path();
        p.arcTo(arcRectF, drawStartRight, -theta);
        canvas.drawPath(p,mLinePaint);



    }

}
