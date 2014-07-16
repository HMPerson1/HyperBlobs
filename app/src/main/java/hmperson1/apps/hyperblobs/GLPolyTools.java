package hmperson1.apps.hyperblobs;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Utility Class for polygons in OpenGL, as well as poly OpenGL tools.
 *
 * @author HMPerson1
 */
@SuppressWarnings("PointlessArithmeticExpression")
final class GLPolyTools {

    /**
     * Radians in a full circle.
     */
    private static final float FULL_CIRCLE = (float) (2 * Math.PI);
    /**
     * Number of bytes needed to store a float.
     */
    private static final int BYTES_PER_FLOAT = Float.SIZE / Byte.SIZE;
    /**
     * Maps a regular polygon to its vertices.
     */
    private static final SparseArray<FloatBuffer> POLYGONS = new SparseArray<FloatBuffer>();
    /**
     * Maps an offset regular polygon to its vertices.
     */
    private static final SparseArray<FloatBuffer> POLYGONS_OFF = new SparseArray<FloatBuffer>();
    /**
     * Maps a regular polygon to its texture coordinates.
     */
    private static final SparseArray<FloatBuffer> TEX_COORDS = new SparseArray<FloatBuffer>();
    /**
     * Maps an offset regular polygon to its texture coordinates.
     */
    private static final SparseArray<FloatBuffer> TEX_COORDS_OFF = new SparseArray<FloatBuffer>();
    /**
     * Maps a regular polygon to the ordering in which its vertices should be
     * drawn.
     */
    private static final SparseArray<ShortBuffer> DRAW_ORDERS = new SparseArray<ShortBuffer>();
    /**
     * Converts a integer color to an OpenGL color.
     */
    private static final float COLOR_FACTOR = 1.0f / 0xFF;
    /**
     * Options for loading {@link Bitmap}s into textures.
     */
    private static final BitmapFactory.Options OPTIONS = new BitmapFactory.Options();

    static {
        OPTIONS.inScaled = false;
    }

    /**
     * Buffer for a box.
     */
    private static final FloatBuffer BOX = ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder()).asFloatBuffer();
    /**
     * Buffer for the texture coordinates of a box.
     */
    private static final FloatBuffer BOX_TEX = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer();

    static {
        BOX.put(new float[]{
                +1, +1, 0,
                +1, -1, 0,
                -1, -1, 0,
                -1, +1, 0});
        BOX_TEX.put(new float[]{
                +1, +0,
                +1, +1,
                +0, +1,
                +0, +0});
    }

    /**
     * {@link Resources} to load textures from.
     */
    private static Resources res;

    /**
     * Utility class -- private constructor.
     */
    private GLPolyTools() {
    }

    /**
     * Creates a vertex array for a regular polygon centered at the origin. If
     * the polygon is not offset, the top center point will be a vertex. If the
     * polygon is offset, there will be a segment instead of a vertex.
     *
     * @param radius   distance each point is from the center
     * @param vertices number of vertices
     * @param offset   whether or not the polygon is offset
     * @return an array of vertices
     */
    private static float[] createPolygon(int radius, int vertices, boolean offset) {
        float[] coords = new float[vertices * 3]; // 3 coords per vertex

        float step = FULL_CIRCLE / vertices;
        float angle = 0;
        if (offset) {
            angle = step / 2;
        }

        // Calculate the positions for every vertex
        for (int i = 0; i < vertices; i++) {
            int idx = i * 3;
            coords[idx + 0] = (float) (Math.cos(angle) * radius); // X
            coords[idx + 1] = (float) (Math.sin(angle) * radius); // Y
            coords[idx + 2] = 0;                   // Z -- since this is 2d, z will always be 0

            angle += step;
        }

        return coords;
    }

    /**
     * Lists the order of which vertices should be connected.
     *
     * @param vertices number of vertices of the polygon
     * @return the ordering
     */
    private static short[] drawOrder(int vertices) {
        int triangles = vertices - 2;
        short[] order = new short[3 * triangles];

        // Start at 0, then the previous vertex, then the next vertex.
        for (int i = 0; i < triangles; i++) {
            int idx = i * 3;
            order[idx + 0] = (short) 0;
            order[idx + 1] = (short) (i + 1);
            order[idx + 2] = (short) (i + 2);
        }

        return order;
    }

    /**
     * Converts the vertices of a polygon to texture coordinates.
     *
     * @param coords the vertices
     * @return the texture coordinates
     * @see #createPolygon
     */
    private static float[] createTexCoords(float[] coords) {
        int vertices = coords.length / 3; // 3 coords per vertex

        // Create the texture coordinates for each vertex
        float[] tCoords = new float[vertices * 2]; // 2 coords per vertex
        for (int i = 0; i < vertices; i++) {
            int xIdx = (2 * i) + 0;
            int yIdx = (2 * i) + 1;

            // Copy respective coords
            tCoords[xIdx] = coords[((3 * i) + 0)];
            tCoords[yIdx] = coords[((3 * i) + 1)];

            // Transform
            tCoords[xIdx] += 1;
            tCoords[xIdx] *= 0.5f;
            tCoords[yIdx] += 1;
            tCoords[yIdx] *= 0.5f;

            // Flip vertically
            tCoords[yIdx] = 1 - tCoords[yIdx];
        }

        return tCoords;
    }

    /**
     * Stores a float array as a {@link FloatBuffer} in native ordering.
     *
     * @param vertices the float array
     * @return a buffer containing the array
     */
    private static FloatBuffer toBuffer(float[] vertices) {
        FloatBuffer buffer =
                ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        buffer.put(vertices).position(0);
        return buffer;
    }

    /**
     * Stores a short array as a {@link ShortBuffer} in native ordering.
     *
     * @param vertices the short array
     * @return a buffer containing the array
     */
    private static ShortBuffer toBuffer(short[] vertices) {
        ShortBuffer buffer =
                ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                        .order(ByteOrder.nativeOrder())
                        .asShortBuffer();
        buffer.put(vertices).position(0);
        return buffer;
    }

    /**
     * Returns a {@link FloatBuffer} with the vertices of the specified regular
     * polygon.
     *
     * @param vertices number of vertices
     * @return the vertices
     */
    public static FloatBuffer polygon(int vertices) {
        if (POLYGONS.indexOfKey(vertices) < 0) {
            POLYGONS.put(vertices, toBuffer(createPolygon(1, vertices, false)));
        }
        return POLYGONS.get(vertices);
    }

    /**
     * Returns a {@link FloatBuffer} with the vertices of the specified offset
     * regular polygon.
     *
     * @param vertices number of vertices
     * @return the vertices
     */
    public static FloatBuffer polygonOff(int vertices) {
        if (POLYGONS_OFF.indexOfKey(vertices) < 0) {
            POLYGONS_OFF.put(vertices, toBuffer(createPolygon(1, vertices, true)));
        }
        return POLYGONS_OFF.get(vertices);
    }

    /**
     * Returns a box.
     *
     * @return a box.
     */
    public static FloatBuffer box() {
        return BOX;
    }

    /**
     * Returns the texture coordinates of a box.
     *
     * @return the texture coordinates of a box.
     */
    public static FloatBuffer boxTex() {
        return BOX_TEX;
    }

    /**
     * Returns a {@link ShortBuffer} with the order in which the vertices of the
     * specified polygon should be drawn.
     *
     * @param vertices number of vertices
     * @return the ordering
     */
    public static ShortBuffer order(int vertices) {
        if (DRAW_ORDERS.indexOfKey(vertices) < 0) {
            DRAW_ORDERS.put(vertices, toBuffer(drawOrder(vertices)));
        }
        return DRAW_ORDERS.get(vertices);
    }

    /**
     * Returns the texture coordinates for each vertex of the polygon.
     *
     * @param vertices number of vertices
     * @return the texture coordinates
     */
    public static FloatBuffer texCoords(int vertices) {
        if (TEX_COORDS.indexOfKey(vertices) < 0) {
            TEX_COORDS.put(vertices, toBuffer(createTexCoords(
                    createPolygon(1, vertices, false))));
        }
        return TEX_COORDS.get(vertices);
    }

    /**
     * Returns the texture coordinates for each vertex of the offset polygon.
     *
     * @param vertices number of vertices
     * @return the texture coordinates
     */
    public static FloatBuffer texCoordsOff(int vertices) {
        if (TEX_COORDS_OFF.indexOfKey(vertices) < 0) {
            TEX_COORDS_OFF.put(vertices, toBuffer(createTexCoords(
                    createPolygon(1, vertices, true))));
        }
        return TEX_COORDS_OFF.get(vertices);
    }

    /**
     * Converts a color to an OpenGL color.
     *
     * @param colors an array to store the converted color
     * @param color  the color
     */
    public static void toGlColor(float[] colors, int color) {
        colors[0] = Color.red(color) * COLOR_FACTOR;
        colors[1] = Color.green(color) * COLOR_FACTOR;
        colors[2] = Color.blue(color) * COLOR_FACTOR;
        colors[3] = Color.alpha(color) * COLOR_FACTOR;
    }

    /**
     * Returns how much something has rotated.
     *
     * @param spr  seconds per revolution
     * @param time current time from {@link System#nanoTime}
     * @return the angle (in degrees)
     */
    public static float rotation(float spr, long time) {
        long nspR = (long) (spr * 1000000000);
        return ((float) (time % nspR) / nspR) * 360;
    }

    /**
     * Loads a texture into the given texture handle.
     *
     * @param handle texture handle
     * @param resId  resource id of the texture
     */
    public static void loadTexture(int handle, int resId) {
        // Prepare OpenGL stuff
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Actually load now
        Bitmap bitmap = BitmapFactory.decodeResource(res, resId, OPTIONS);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Unload the bitmap
        bitmap.recycle();
    }

    /**
     * Initialize with the {@link Resources} for texture loading.
     *
     * @param r the Resources from our package
     */
    public static void init(Resources r) {
        res = r;
    }
}
