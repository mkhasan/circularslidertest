package com.lightweight.lightweightrangeslider.GLSurface;

/**
 * Created by usrc on 17. 12. 28.
 */

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class ShapeGLSurfaceView extends GLSurfaceView
{
    private ShapeRenderer mRenderer;

    // Offsets for touch events
    private float mPreviousX;
    private float mPreviousY;

    private float mDensity;

    public ShapeGLSurfaceView(Context context)
    {
        super(context);

    }

    public ShapeGLSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

    }

    // Hides superclass method.
    public void setRenderer(ShapeRenderer renderer, float density)
    {
        mRenderer = renderer;
        mDensity = density;
        super.setRenderer(renderer);
    }

    public void setEndAngle(double angle) {
        if (mRenderer != null)
            mRenderer.setEndAngle(angle);
    }
}
