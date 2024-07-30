package com.lightweight.lightweightrangeslider.GLSurface;

/**
 * Created by usrc on 17. 12. 28.
 */

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;



import com.lightweight.lightweightrangeslider.Shapes.CircularSlider;
import com.lightweight.lightweightrangeslider.Utils.TextureHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.opengles.GL10;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class ShapeRenderer implements GLSurfaceView.Renderer
{
    /** Used for debug logs. */
    private static final String TAG = "ShapeRenderer";

    static boolean first = true;

    private final Activity aShapeActivity;
    private final GLSurfaceView aGlSurfaceView;


    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] aProjectionMatrix = new float[16];


    // Shapes objects
    private CircularSlider aCircularSlider;


    /** Thread executor for generating data points in the background. */
    private final ExecutorService aSingleThreadedExecutor = Executors.newSingleThreadExecutor();

    public ShapeRenderer(final Activity shapeActivity, final GLSurfaceView glSurfaceView) {
        aShapeActivity = shapeActivity;
        aGlSurfaceView = glSurfaceView;

    }

    private void generatePlots() {
        aSingleThreadedExecutor.submit(new GenDataRunnable());
    }

    class GenDataRunnable implements Runnable {

        GenDataRunnable() {

        }

        @Override
        public void run() {
            try {

                // Run on the GL thread -- the same thread the other members of the renderer run in.
                aGlSurfaceView.queueEvent(new Runnable() {

                    @Override
                    public void run() {

                        // Not supposed to manually call this, but Dalvik sometimes needs some additional prodding to clean up the heap.
                        System.gc();

                        try {

                                aCircularSlider = new CircularSlider(aShapeActivity);



                        } catch (OutOfMemoryError err) {

                            // Not supposed to manually call this, but Dalvik sometimes needs some additional prodding to clean up the heap.
                            System.gc();

                            aShapeActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//									Toast.makeText(mLessonSevenActivity, "Out of memory; Dalvik takes a while to clean up the memory. Please try again.\nExternal bytes allocated=" + dalvik.system.VMRuntime.getRuntime().getExternalBytesAllocated(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
            } catch (OutOfMemoryError e) {
                // Not supposed to manually call this, but Dalvik sometimes needs some additional prodding to clean up the heap.
                System.gc();

                aShapeActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//						Toast.makeText(mLessonSevenActivity, "Out of memory; Dalvik takes a while to clean up the memory. Please try again.\nExternal bytes allocated=" + dalvik.system.VMRuntime.getRuntime().getExternalBytesAllocated(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, javax.microedition.khronos.egl.EGLConfig config)
    {

        generatePlots();

        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {

        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        final float ratio = (float) width / height;

        Matrix.orthoM(aProjectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);
    }

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


        if (aCircularSlider != null) {
            aCircularSlider.render(aProjectionMatrix);
        }

    }

    public void setEndAngle(double angle) {
        //double theta = -angle;
        if (aCircularSlider != null)
            aCircularSlider.setEndAngle(angle);
    }



}
