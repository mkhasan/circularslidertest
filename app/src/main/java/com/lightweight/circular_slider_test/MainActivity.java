package com.lightweight.circular_slider_test;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.lightweight.circularbarrange.CircularBarRange;
import com.lightweight.circularbarrange.CircularBarRangeOriginal;
import com.lightweight.lightweightrangeslider.GLSurface.ShapeGLSurfaceView;
import com.lightweight.lightweightrangeslider.GLSurface.ShapeRenderer;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    final String TAG = this.getClass().getSimpleName();
    static final int ratioX = 1;
    static final int ratioY = 1;

    static double startAngle;
    static double endAngle;

    public final int minValue = 0;

    public final int maxValue = 20000;

    public static DisplayMetrics metrics;

    Timer workoutChecker = null;

    int stepSize = 1000;

    int currEnc = 0;

    int dir = 1;

    private ShapeGLSurfaceView aGLSurfaceView;

    private ShapeRenderer aRenderer;
    CircularBarRangeOriginal circularBarRangeOriginal;

    public static final long WORKOUT_CHECKER_UPDATE_PERIOD = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startAngle = (double) this.getResources().getInteger(R.integer.start_angle);
        endAngle = 360.0f- startAngle;

        metrics = getResources().getDisplayMetrics();


        aGLSurfaceView = findViewById(R.id.circular_bar_range);
        initSliderView();

        circularBarRangeOriginal = findViewById(R.id.circular_bar_range_ori);

        ResizeView(aGLSurfaceView);
        ResizeView(circularBarRangeOriginal);

        Log.e(TAG, "angle: " + ComputeAngleFromValue(10000));

        workoutChecker = new Timer();

        workoutChecker.scheduleAtFixedRate(

            new TimerTask() {


                public void run() {


                    double angle = ComputeAngleFromValue(currEnc);
                    double oglAngle = ComputeOGLAngleFromValue(currEnc);
                    //Log.e(TAG, "running " + currEnc + " angle: " + angle);
                    currEnc += dir*stepSize;
                    if (currEnc >= maxValue) {
                        currEnc = maxValue;
                        //Log.e(TAG, "angle at maxValue: " + angle);
                        dir = -1;
                    }
                    if (currEnc <= minValue) {
                        currEnc = minValue;
                        Log.e(TAG, "angle at minValue: " + angle);
                        dir = 1;
                    }

                    if (currEnc > maxValue/2 && currEnc < maxValue/2+4000) {
                        Log.e(TAG, "angle at mid: " + angle);
                    }



                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //updateConnectionStatusText(findViewById(R.id.comm_status_text_view), isOkay);
                            //circularBarRange.setEndAngle(angle);
                            //circularBarRange.invalidate();

                            aGLSurfaceView.setEndAngle(oglAngle);



                            circularBarRangeOriginal.setEndAngle(angle);
                            circularBarRangeOriginal.invalidate();



                        }
                    });




                };



            }, WORKOUT_CHECKER_UPDATE_PERIOD, WORKOUT_CHECKER_UPDATE_PERIOD
        );
    }

    public static float pxToDp(float px) {

        return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static void ResizeView(View view) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        int width = params.width == MATCH_PARENT || params.width == WRAP_CONTENT ? params.width : Math.round(ratioX * pxToDp(params.width));
        int height = params.height == MATCH_PARENT || params.height == WRAP_CONTENT ? params.height : Math.round(ratioY * pxToDp(params.height));
        int leftMargin = Math.round(ratioX * pxToDp(params.leftMargin));
        int rightMargin = Math.round(ratioX * pxToDp(params.rightMargin));
        int topMargin = Math.round(ratioY * pxToDp(params.topMargin));
        int bottomMargin = Math.round(ratioY * pxToDp(params.bottomMargin));
        int leftPadding = Math.round(ratioX * pxToDp(view.getPaddingLeft()));
        int rightPadding = Math.round(ratioX * pxToDp(view.getPaddingRight()));
        int topPadding = Math.round(ratioX * pxToDp(view.getPaddingTop()));
        int bottomPadding = Math.round(ratioX * pxToDp(view.getPaddingBottom()));

        params.width = width;
        params.height = height;
        params.leftMargin = leftMargin;
        params.rightMargin = rightMargin;
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        view.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);





    }

    double ComputeAngleFromValue(int value) {
        double finalAngle = endAngle;
        while (finalAngle < startAngle)
            finalAngle += 360.0f;

        double targetAngle = startAngle + ((double) (value-minValue) / (double) (maxValue-minValue)) * (double) (finalAngle-startAngle);

        if (targetAngle > 180f)
            targetAngle -= 360.0f;
        else if (targetAngle < -180.f)
            targetAngle += 360.0f;

        return targetAngle;
    }

    double ComputeOGLAngleFromValue(int value) {
        double startAngle = -90.0+18.0;
        double finalAngle = 90.0-18.0;
        double targetAngle = startAngle + ((double) (value-minValue) / (double) (maxValue-minValue)) * (double) (finalAngle-startAngle);
        return targetAngle;
    }

    void initGL() {
        aGLSurfaceView.setEGLContextClientVersion(2);

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Set the renderer to our demo renderer, defined below.
        aRenderer = new ShapeRenderer(this, aGLSurfaceView);

        aGLSurfaceView.setZOrderOnTop(true);
        aGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        aGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);

        aGLSurfaceView.setRenderer(aRenderer, displayMetrics.density);


    }
    void initSliderView() {
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            /*

            aGLSurfaceView.setEGLContextClientVersion(2);

            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            // Set the renderer to our demo renderer, defined below.
            aRenderer = new ShapeRenderer(this, aGLSurfaceView, 5);
            aGLSurfaceView.setRenderer(aRenderer, displayMetrics.density, 5);
            */

            initGL();
            Log.e(TAG, "initGL done");
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            Log.e(TAG, "initGL not done");
        }



    }



}