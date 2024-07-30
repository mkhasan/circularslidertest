package com.lightweight.lightweightrangeslider.Shapes;

import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.lightweight.lightweightrangeslider.R;
import com.lightweight.lightweightrangeslider.Utils.RawResourceReader;
import com.lightweight.lightweightrangeslider.Utils.ShaderHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

/**
 * Created by Hasan on 24. 7. 28.
 * Email: hasan@kaist.ac.kr
 * Desc:
 */
public class CircularSlider {
    final String TAG = this.getClass().getSimpleName();

    private final int[] glShaderBuffer;

    private float EPSILON = 0;
    private int BYTES_PER_FLOAT = 4;
    private int POSITION_DATA_SIZE = 3;

    private final int SLICE_COUNT = 1000;
    final float START_ANGLE = -80.0f;
    //final float END_ANGLE = 80.0f;

    final float BORDER_THICKNESS = 0.08f;

    final float ARC_THICKNESS = 0.08f;

    enum ModelType {
        BORDER,
        ARC,
        LINE_CAP,
        NO_OF_MODEL_TYPES,
    }

    private final int shaderProgram;

    private int[] vertexCount = new int[ModelType.NO_OF_MODEL_TYPES.ordinal()];

    private int aBorderPositionsBufferIdx;
    private int aArcPositionsBufferIdx;
    private int aLineCapPositionsBufferIdx;

    FloatBuffer greenColor, redColor, greyColor;
    private int aProjectionMatrixHandle;
    private int aViewMatrixHandle;
    private int aModelMatrixHandle;
    private int aPositionHandle;
    private int aColorHandle;
    private int count;
    private int stripIndex = SLICE_COUNT/2+1;

    private Object stripIndexMutex = new Object();




    public CircularSlider(Context activity){

        /** initialize the sphere program */
        final String sphereVS = RawResourceReader.readTextFileFromRawResource(activity, R.raw.circular_slider_vertex_shader);
        final String sphereFS = RawResourceReader.readTextFileFromRawResource(activity, R.raw.circular_slider_fragment_shader);


        final int sphereVSHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, sphereVS);
        final int sphereFSHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, sphereFS);



        shaderProgram = ShaderHelper.createAndLinkProgram(sphereVSHandle, sphereFSHandle,
                new String[]{"a_Position"});

        // Second, copy these buffers into OpenGL's memory. After, we don't need to keep the client-side buffers around.
        glShaderBuffer = new int[ModelType.NO_OF_MODEL_TYPES.ordinal()];
        GLES20.glGenBuffers(glShaderBuffer.length, glShaderBuffer, 0);

        createBuffers();

        greenColor = getColor(new float[]{0.73f, 0.99f, 0.31f, 1.0f});
        redColor = getColor(new float[]{0.9f, 0.2f, 0.2f, 1.0f});
        greyColor = getColor(new float[]{0.17f, 0.17f, 0.19f, 1.0f});

    }


    public void createBuffers(){
        FloatBuffer aBorderVerticesBuffer, aArcVerticesBuffer, aLineCapVerticesBuffer;


        float[] borderVertices = createArc(BORDER_THICKNESS);
        vertexCount[ModelType.BORDER.ordinal()] = borderVertices.length/POSITION_DATA_SIZE;

        // Initialize the buffers.
        aBorderVerticesBuffer = ByteBuffer.allocateDirect(borderVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        aBorderVerticesBuffer.put(borderVertices).position(0);



        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glShaderBuffer[ModelType.BORDER.ordinal()]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, aBorderVerticesBuffer.capacity() * BYTES_PER_FLOAT, aBorderVerticesBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        float[] arcVertices = createArc(ARC_THICKNESS, 1.0f-(BORDER_THICKNESS-ARC_THICKNESS)/2.0f);
        vertexCount[ModelType.ARC.ordinal()] = arcVertices.length/POSITION_DATA_SIZE;

        // Initialize the buffers.
        aArcVerticesBuffer = ByteBuffer.allocateDirect(arcVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        aArcVerticesBuffer.put(arcVertices).position(0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glShaderBuffer[ModelType.ARC.ordinal()]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, aArcVerticesBuffer.capacity() * BYTES_PER_FLOAT, aArcVerticesBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        float[] lineCapVertices = createSector(ARC_THICKNESS/2.0f);
        vertexCount[ModelType.LINE_CAP.ordinal()] = lineCapVertices.length/POSITION_DATA_SIZE;

        aLineCapVerticesBuffer = ByteBuffer.allocateDirect(lineCapVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        aLineCapVerticesBuffer.put(lineCapVertices).position(0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glShaderBuffer[ModelType.LINE_CAP.ordinal()]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, aLineCapVerticesBuffer.capacity() * BYTES_PER_FLOAT, aLineCapVerticesBuffer, GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);



        aBorderPositionsBufferIdx = glShaderBuffer[ModelType.BORDER.ordinal()];
        aArcPositionsBufferIdx = glShaderBuffer[ModelType.ARC.ordinal()];
        aLineCapPositionsBufferIdx = glShaderBuffer[ModelType.LINE_CAP.ordinal()];



        aBorderVerticesBuffer.limit(0);
        aBorderVerticesBuffer = null;
        aArcVerticesBuffer.limit(0);
        aArcVerticesBuffer = null;
        aLineCapVerticesBuffer.limit(0);
        aLineCapVerticesBuffer = null;


    }

    private void singleSideRender(boolean isLefSide) {
        singleSideRender(isLefSide, null);
    }
    private void singleSideRender(boolean isLeftSide, float[] aModelMatrix) {


        float[] initialModelMatrix = new float[16];
        Matrix.setIdentityM(initialModelMatrix, 0);
        if (isLeftSide) {
            Matrix.rotateM(initialModelMatrix, 0, 180.0f, 0.0f, 1.0f, 0.0f);

        }

        // in case model matrix is provide we keep it to get rotation using finger touch
        if (aModelMatrix == null) {
            aModelMatrix = new float[16];
            System.arraycopy(initialModelMatrix, 0, aModelMatrix, 0, 16);

        }

        ///////////////////////// border //////////////////////////////////

        System.arraycopy(initialModelMatrix, 0, aModelMatrix, 0, 16);

        GLES20.glUniformMatrix4fv(aModelMatrixHandle, 1, false, aModelMatrix, 0);

        GLES20.glUniform4fv(aColorHandle, 1, greyColor);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, aBorderPositionsBufferIdx);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount[ModelType.BORDER.ordinal()]);

        ////////////////////////////////////////////////////////////////////////

        ///////////////////////// border lower cap /////////////////////////////

        System.arraycopy(initialModelMatrix, 0, aModelMatrix, 0, 16);
        float borderCapRadius = BORDER_THICKNESS/2.0f;
        float arcCapRadius = ARC_THICKNESS/2.0f;
        float d = 1.0f - borderCapRadius;
        float alpha = START_ANGLE;//radians(START_ANGLE);
        float theta = START_ANGLE+180.0f;//radians(START_ANGLE+180.0f);
        float factor = borderCapRadius/arcCapRadius;



        Matrix.translateM(aModelMatrix, 0, d*(float) cos(radians(alpha)), d*(float) sin(radians(alpha)), 0);
        Matrix.rotateM(aModelMatrix, 0, theta, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(aModelMatrix, 0, factor, factor, 1.0f);

        //Matrix.scaleM(aModelMatrix, 0, factor, factor, 1.0f);


        GLES20.glUniformMatrix4fv(aModelMatrixHandle, 1, false, aModelMatrix, 0);

        GLES20.glUniform4fv(aColorHandle, 1, greyColor);



        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, aLineCapPositionsBufferIdx);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount[ModelType.LINE_CAP.ordinal()]);

        //////////////////////////////////////////////////////////////////

        //////////////////// border upper cap /////////////////////////////////

        System.arraycopy(initialModelMatrix, 0, aModelMatrix, 0, 16);

        d = 1.0f - borderCapRadius;
        theta = -START_ANGLE;
        factor = borderCapRadius/arcCapRadius;
        Matrix.translateM(aModelMatrix, 0, d*(float) cos(radians(theta)), d*(float) sin(radians(theta)), 0.0f);
        Matrix.rotateM(aModelMatrix, 0, theta, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(aModelMatrix, 0, factor, factor, 1.0f);


        GLES20.glUniformMatrix4fv(aModelMatrixHandle, 1, false, aModelMatrix, 0);

        GLES20.glUniform4fv(aColorHandle, 1, greyColor);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, aLineCapPositionsBufferIdx);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount[ModelType.LINE_CAP.ordinal()]);

        ///////////////////////////////////////////////////////////////////////

        ////////////////////////// main arc ///////////////////////////        


        System.arraycopy(initialModelMatrix, 0, aModelMatrix, 0, 16);

        // make it be closer to the viewer to avoid conflict
        // but when the whole thinks is rotate 180 around Y axis then this arc
        // goes to behind the border...so sing adjustment is necessary

        Matrix.translateM(aModelMatrix, 0, 0, 0, isLeftSide ? -EPSILON : EPSILON);

        GLES20.glUniformMatrix4fv(aModelMatrixHandle, 1, false, aModelMatrix, 0);
        GLES20.glUniform4fv(aColorHandle, 1, greenColor);
        /*
        if (count % 50 == 0) {

            stripIndex = new Random().nextInt(SLICE_COUNT-1);
            //my_index = 0;

        }



         */

        int myIndex = 0;
        synchronized (stripIndexMutex) {
            myIndex = stripIndex;
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, aArcPositionsBufferIdx);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, (2*myIndex*3)/(3));


        /////////////////////////////////////////////////////////////////////////////

        ////////////////////////// main arc bottom cap /////////////////////////////

        if (myIndex > 0) {
            System.arraycopy(initialModelMatrix, 0, aModelMatrix, 0, 16);
            d = 1.0f - borderCapRadius;
            alpha = START_ANGLE;
            theta = START_ANGLE+180;
            Matrix.translateM(aModelMatrix, 0, d*(float) cos(radians(alpha)), d*(float) sin(radians(alpha)), isLeftSide ? -EPSILON : EPSILON);
            Matrix.rotateM(aModelMatrix, 0, theta, 0, 0, 1.0f);

            GLES20.glUniformMatrix4fv(aModelMatrixHandle, 1, false, aModelMatrix, 0);
            GLES20.glUniform4fv(aColorHandle, 1, greenColor);


            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, aLineCapPositionsBufferIdx);
            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);


            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount[ModelType.LINE_CAP.ordinal()]);

            /////////////////////////////////////////////////////////////////////////////


            ////////////////////////// main arc tpp cap /////////////////////////////

            System.arraycopy(initialModelMatrix, 0, aModelMatrix, 0, 16);


            float percent = ((myIndex-1) / (float) (SLICE_COUNT));
            //float rad = glm::radians(START_ANGLE) + percent * (glm::radians(END_ANGLE)-glm::radians(START_ANGLE));
            float deg = START_ANGLE + percent * (-START_ANGLE-START_ANGLE);
            float capRadius = ARC_THICKNESS/2;
            d = 1.0f - (BORDER_THICKNESS-ARC_THICKNESS)/2 - capRadius;
            Matrix.translateM(aModelMatrix, 0, d*(float) cos(radians(deg)), d*(float) sin(radians(deg)), isLeftSide ? -EPSILON : EPSILON );
            Matrix.rotateM(aModelMatrix, 0, deg, 0, 0, 1.0f);

            GLES20.glUniformMatrix4fv(aModelMatrixHandle, 1, false, aModelMatrix, 0);
            GLES20.glUniform4fv(aColorHandle, 1, greenColor);


            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, aLineCapPositionsBufferIdx);
            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, 0);


            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount[ModelType.LINE_CAP.ordinal()]);

            /////////////////////////////////////////////////////////////////////////////

        }

        count ++;

    }

    public void render(float[] aProjectionMatrix){

        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(shaderProgram);

        // Set program handles. These will later be used to pass in values to the program.
        aProjectionMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_projection");
        aViewMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_view");
        aModelMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_model");
        aColorHandle = GLES20.glGetUniformLocation(shaderProgram, "u_color");
        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        //aColorHandle = GLES20.glGetAttribLocation(aSphereProgramHandle, "a_Color");

        float[] aViewMatrix = new float[16];
        Matrix.setIdentityM(aViewMatrix, 0);
        Matrix.translateM(aViewMatrix, 0, 0, 0, -3);

        GLES20.glUniformMatrix4fv(aProjectionMatrixHandle, 1, false, aProjectionMatrix, 0);
        GLES20.glUniformMatrix4fv(aViewMatrixHandle, 1, false, aViewMatrix, 0);

        singleSideRender(false);
        singleSideRender(true);

    }

    public void release() {
        // Delete buffers from OpenGL's memory
        final int[] buffersToDelete = new int[] { aBorderPositionsBufferIdx, aArcPositionsBufferIdx, aLineCapPositionsBufferIdx};
        GLES20.glDeleteBuffers(buffersToDelete.length, buffersToDelete, 0);
    }


    static float radians(float deg) {
        return deg*(float) Math.PI/180.0f;
    };
    float[] createArc(float thickness) {
        return createArc(thickness, 1.0f);
    }


    float[] createArc(float thickness, float radius) {
        int slice_count = SLICE_COUNT;// thickness > 0.4f+0.1 > SLICE_COUNT ? SLICE_COUNT : temp;
        float center_x = 0.0f;
        float center_y = 0.0f;
        float start_angle = radians(START_ANGLE);
        float end_angle = -start_angle;

        float center_z = 0.0f;

        int no_of_coordinates = ((slice_count + 1) * 2) * 3;
        // Create a buffer for vertex data
        float[] vertices = new float[no_of_coordinates]; // (x,y) for each vertex

        int idx = 0;

        // Center vertex for triangle fan

        for (int i = 0; i < slice_count + 1; ++i) {

            float percent = (i / (float) (slice_count));
            float rad = start_angle + percent * (end_angle - start_angle);

            vertices[idx++] = center_x + (radius - thickness) * (float) cos(rad);
            vertices[idx++] = center_y + (radius - thickness) * (float) sin(rad);
            vertices[idx++] = center_z;

            vertices[idx++] = center_x + (radius) * (float) cos(rad);
            vertices[idx++] = center_y + (radius) * (float) sin(rad);
            vertices[idx++] = center_z;

        }
        return vertices;
    }

    float[] createSector(float radius) {
        int vertexCount = SLICE_COUNT/2;
        float center_x = 0.0f;
        float center_y = 0.0f;

        float center_z = 0.0f;

        int no_of_coordinates = vertexCount*3;
        // Create a buffer for vertex data
        float[] vertices = new float[no_of_coordinates]; // (x,y) for each vertex

        int idx = 0;

        // Center vertex for triangle fan
        vertices[idx++] = center_x;
        vertices[idx++] = center_y;
        vertices[idx++] = center_z;


        // Outer vertices of the circle
        int outerVertexCount = vertexCount-1;

        for (int i = 0; i < outerVertexCount; ++i){
            float percent = (i / (float) (outerVertexCount-1));
            float rad = percent * (float) Math.PI;

            //Vertex position
            float outer_x = center_x + radius * (float) cos(rad);
            float outer_y = center_y + radius * (float) sin(rad);

            vertices[idx++] = outer_x;
            vertices[idx++] = outer_y;
            vertices[idx++] = center_z;
        }


        return vertices;
    }


    FloatBuffer getColor(float[] colorArray) {
        FloatBuffer color = ByteBuffer.allocateDirect(colorArray.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        color.put(colorArray).position(0);
        return color;
    }


    public void setEndAngle(double angle) {

        float percent =  ((float) angle - START_ANGLE)/(-START_ANGLE-START_ANGLE);

        if (percent < 0.f || percent > 1.0f) {
            Log.e(TAG, "percent: " + percent + " angle: " + angle);
            return;
        }
        int myIndex = (int) (percent*(float) SLICE_COUNT);

        if (myIndex < 0 || myIndex >= SLICE_COUNT+1) {
            Log.e(TAG, "index: " + myIndex + " angle: " + angle);
            return;
        }

        synchronized (stripIndexMutex) {
            this.stripIndex = myIndex;
        }

    }
}

