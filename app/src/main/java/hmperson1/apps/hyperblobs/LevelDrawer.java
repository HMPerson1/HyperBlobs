package hmperson1.apps.hyperblobs;

import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import org.jetbrains.annotations.NonNls;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static hmperson1.apps.hyperblobs.LevelUpdater.BLOB_RADIUS;
import static hmperson1.apps.hyperblobs.LevelUpdater.SPIKE_RADIUS;

/**
 * @author HMPerson1
 */
class LevelDrawer implements GLSurfaceView.Renderer {

    /**
     * Vertex Shader.
     */
    @NonNls
    private static final String VERTEX_SHADER_CODE = ""
            + "uniform mat4 uMVPMatrix;"
            + "attribute vec4 aPosition;"
            + "attribute vec2 aTexCoord;"
            + "varying vec2 vTexCoord;"
            + "void main() {"
            + "  vTexCoord = aTexCoord;"
            + "  gl_Position = uMVPMatrix * aPosition;"
            + "}";
    /**
     * Fragment Shader.
     */
    @NonNls
    private static final String FRAGMENT_SHADER_CODE = ""
            + "precision mediump float;"
            + "uniform sampler2D uTexture;"
            + "uniform vec4 uColor;"
            + "varying vec2 vTexCoord;"
            + "void main() {"
            + "  gl_FragColor = uColor * texture2D(uTexture, vTexCoord);"
            + "}";
    /**
     * Lock for changing {@code mNextState}.
     */
    private final Object mNextStateLock = new Object();
    /**
     * Model View Projection Matrix.
     */
    private final float[] mMVPMatrix = new float[16];
    /**
     * Model Matrix.
     */
    private final float[] mMMatrix = new float[16];
    /**
     * View Projection Matrix.
     */
    private final float[] mVPMatrix = new float[16];
    /**
     * Projection Matrix.
     */
    private final float[] mPMatrix = new float[16];
    /**
     * View Matrix.
     */
    private final float[] mVMatrix = new float[16];
    /**
     * Cached float array for colors.
     */
    private final float[] mColor = new float[4];
    /**
     * Data that is currently being drawn onto the screen.
     */
    private LevelState mDrawingState;
    /**
     * Data that will be drawn on the next {@code onDrawFrame()}.
     */
    private LevelState mNextState;
    /**
     * Limits of the camera range.
     */
    private float mCamLeft, mCamRight;
    /**
     * Used to pass in the transformation matrix. (Uniform)
     */
    private int muMVPMatrixHandle;
    /**
     * Used to pass in model position information. (Attribute)
     */
    private int maPositionHandle;
    /**
     * Used to pass in model color information. (Uniform)
     */
    private int muColorHandle;
    /**
     * Used to pass in texture information. (Uniform)
     */
    private int muTexHandle;
    /**
     * Used to pass in texture coordinate information. (Attribute)
     */
    private int maTexCoordHandle;
    /**
     * Handle to the blob texture data.
     */
    private int mTexBlobHandle;
    /**
     * Handle to the spike texture data.
     */
    private int mTexSpikeHandle;
    /**
     * Handle to the wall texture data.
     */
    private int mTexWallHandle;

    /**
     * Creates a program with both shaders.
     *
     * @return the program handle
     */
    private static int createProgram() {
        // Load vertex shader
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, VERTEX_SHADER_CODE);
        GLES20.glCompileShader(vShader);

        // Load vertex shader
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, FRAGMENT_SHADER_CODE);
        GLES20.glCompileShader(fShader);

        // Create program
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vShader);
        GLES20.glAttachShader(program, fShader);
        GLES20.glLinkProgram(program);

        return program;
    }

    /**
     * Sets the data to be drawn on the next {@code onDrawFrame()}.
     *
     * @param state the state from {@code LevelUpdater}
     */
    void setState(LevelState state) {
        synchronized (mNextStateLock) {
            mNextState = state;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set OpenGL settings
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLPolyTools.toGlColor(mColor, Color.LTGRAY);
        GLES20.glClearColor(mColor[0], mColor[1], mColor[2], mColor[3]);

        // Create the view matrix
        Matrix.setLookAtM(mVMatrix, 0,
                +0.0f, +0.0f, +1.5f, // eye point
                +0.0f, +0.0f, -5.0f, // center point
                +0.0f, +1.0f, +0.0f) // up vector
        ;

        // Initialize program
        int program = createProgram();
        // Get handles
        muMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        muColorHandle = GLES20.glGetUniformLocation(program, "uColor");
        muTexHandle = GLES20.glGetUniformLocation(program, "uTexture");
        maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        maTexCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");

        GLES20.glUseProgram(program);

        // Load textures
        int[] tmp = new int[3];
        GLES20.glGenTextures(tmp.length, tmp, 0);

        mTexBlobHandle = tmp[0];
        mTexSpikeHandle = tmp[1];
        mTexWallHandle = tmp[2];

        GLPolyTools.loadTexture(mTexBlobHandle, R.drawable.blob);
        GLPolyTools.loadTexture(mTexSpikeHandle, R.drawable.spike);
        GLPolyTools.loadTexture(mTexWallHandle, R.drawable.wall);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // Construct the projection matrix
        float hWidth = ((LevelUpdater.HEIGHT / height) * width) / 2;
        Matrix.orthoM(mPMatrix, 0, -hWidth, hWidth, 0, LevelUpdater.HEIGHT, 1, 10);
        mCamLeft = hWidth;
        mCamRight = 960 - hWidth;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Update the drawing state and reset the next state
        synchronized (mNextStateLock) {
            if (mNextState != null) {
                mDrawingState = mNextState;
                mNextState = null;
            }
        }
        // Don't draw if there's nothing to draw
        if (mDrawingState == null) {
            return;
        }

        // Clear the screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Clear ALL the matrices!
        Matrix.setIdentityM(mMMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);

        // Construct the view-projection matrix
        Matrix.multiplyMM(mVPMatrix, 0, mPMatrix, 0, mVMatrix, 0);
        Matrix.translateM(mVPMatrix, 0, -getCameraTranslation(mDrawingState.getBlob().x), 0, 0);

        long time = System.nanoTime();

        // Walls
        for (List<Point> wall : mDrawingState.getWalls()) {
            Matrix.setIdentityM(mMMatrix, 0);
            Point center = wall.get(0);
            Point dims = wall.get(1);
            // Move and scale
            Matrix.translateM(mMMatrix, 0, center.x, center.y, 0);
            Matrix.scaleM(mMMatrix, 0, dims.x, dims.y, 0);
            // Draw
            Matrix.multiplyMM(mMVPMatrix, 0, mVPMatrix, 0, mMMatrix, 0);
            drawShape(GLPolyTools.box(), GLPolyTools.order(4),
                    GLPolyTools.boxTex(), mTexWallHandle, Color.WHITE);
        }

        // Spikes
        float angle = GLPolyTools.rotation(1, time);
        for (Point spike : mDrawingState.getSpikes()) {
            Matrix.setIdentityM(mMMatrix, 0);
            // Move and scale
            Matrix.translateM(mMMatrix, 0, spike.x, spike.y, 0);
            Matrix.scaleM(mMMatrix, 0, SPIKE_RADIUS, SPIKE_RADIUS, 0);
            Matrix.rotateM(mMMatrix, 0, angle, 0, 0, 1);
            // Draw
            Matrix.multiplyMM(mMVPMatrix, 0, mVPMatrix, 0, mMMatrix, 0);
            drawShape(GLPolyTools.polygon(3), GLPolyTools.order(3),
                    GLPolyTools.texCoords(3), mTexSpikeHandle, Color.LTGRAY);
            drawShape(GLPolyTools.polygonOff(3), GLPolyTools.order(3),
                    GLPolyTools.texCoordsOff(3), mTexSpikeHandle, Color.LTGRAY);
        }

        // Blob
        Point blob = mDrawingState.getBlob();
        Matrix.setIdentityM(mMMatrix, 0);
        // Move and scale
        Matrix.translateM(mMMatrix, 0, blob.x, blob.y, 0);
        Matrix.scaleM(mMMatrix, 0, BLOB_RADIUS, BLOB_RADIUS, 0);
        Matrix.rotateM(mMMatrix, 0, GLPolyTools.rotation(10, time), 0, 0, 1);
        // Draw
        Matrix.multiplyMM(mMVPMatrix, 0, mVPMatrix, 0, mMMatrix, 0);
        drawShape(GLPolyTools.polygon(8), GLPolyTools.order(8),
                GLPolyTools.texCoords(8), mTexBlobHandle, Color.WHITE);
    }

    private float getCameraTranslation(float blob) {
        float ret = blob;
        if (ret > mCamRight) ret = mCamRight;
        if (ret < mCamLeft) ret = mCamLeft;
        return ret;
    }

    /**
     * Draws a shape onto the screen with the given information.
     *
     * @param vertices      vertices
     * @param order         ordering
     * @param texCoord      coordinates of each vertex in texels
     * @param textureHandle texture to be used
     * @param color         color of the shape
     */
    private void drawShape(FloatBuffer vertices, ShortBuffer order,
                           FloatBuffer texCoord, int textureHandle, int color) {
        // Pass in vertex info
        vertices.position(0);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT,
                false, 12, vertices);

        // Pass in color info
        GLPolyTools.toGlColor(mColor, color);
        GLES20.glUniform4fv(muColorHandle, 1, mColor, 0);

        // Pass in texture coordinate info
        texCoord.position(0);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);
        GLES20.glVertexAttribPointer(maTexCoordHandle, 2, GLES20.GL_FLOAT,
                false, 8, texCoord);

        // Pass in texture info
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLES20.glUniform1i(muTexHandle, 0);

        // Draw
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, order.capacity(),
                GLES20.GL_UNSIGNED_SHORT, order);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
    }
}
