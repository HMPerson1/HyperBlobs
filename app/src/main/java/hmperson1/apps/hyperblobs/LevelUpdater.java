package hmperson1.apps.hyperblobs;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.view.Surface;

/**
 * Continually does physics simulation and game-state updating.
 *
 * @author HMPerson1
 */
public class LevelUpdater extends Thread {

    /**
     * Radius of a blob.
     */
    public static final int BLOB_RADIUS = 30;
    /**
     * Radius of a spike.
     */
    public static final int SPIKE_RADIUS = 20;
    /**
     * Height of the emulated screen.
     */
    public static final float HEIGHT = 360;
    /**
     * Scaling factor from Box2d to screen.
     */
    public static final float SCALE = 10;
    /**
     * Scaling factor from screen to Box2d.
     */
    public static final float INV_SCALE = 1 / SCALE;
    /**
     * Used to input data from sensors.
     */
    public final UserInput input = new UserInput();
    /**
     * Rendering thread to which {@link LevelState}s must be given.
     */
    private final LevelDrawer mDrawer;
    /**
     * Physics updater.
     */
    private final LevelPhysicsUpdater mPhysics;
    /**
     * The next velocity the blob should have.
     */
    private final float[] mNextVel = new float[2];
    /**
     * Whether or not we should be running.
     */
    private volatile boolean mRun = true;

    /**
     * Creates a new {@code LevelUpdater} with the given state.
     *
     * @param state the initial state of the world
     */
    public LevelUpdater(LevelState state) {
        mDrawer = new LevelDrawer();
        mPhysics = new LevelPhysicsUpdater(state, 960);

    }

    /**
     * Loops through physics updates.
     */
    @SuppressWarnings("RefusedBequest")
    @Override
    public void run() {
        boolean cont = true;
        while (cont) {
            synchronized (this) {
                while (!mRun) try {
                    wait();
                } catch (InterruptedException ignored) {
                    cont = false;
                }
            }
            input.run();
            synchronized (mNextVel) {
                mPhysics.setBlobVelocity(mNextVel[0], mNextVel[1]);
            }
            setMotion(0, 0);
            mPhysics.run();
            mDrawer.setState(mPhysics.getState());
        }
    }

    /**
     * Sets the motion of the blob.
     *
     * @param x velocity
     * @param y velocity
     */
    void setMotion(float x, float y) {
        synchronized (mNextVel) {
            mNextVel[0] = x;
            mNextVel[1] = y;
        }
    }

    /**
     * Used for stopping the main loop.
     *
     * @param running whether or not to continue to run
     */
    public synchronized void setRunning(boolean running) {
        mRun = running;
        notifyAll();
    }

    /**
     * Returns the {@link LevelDrawer} being used.
     *
     * @return the LevelDrawer
     */
    Renderer getRenderer() {
        return mDrawer;
    }

    /**
     * Interprets user inputs.
     */
    @SuppressWarnings("PublicInnerClass")
    public class UserInput implements Runnable {

        /**
         * Constant for {@code dataType} representing touch data.
         */
        private static final int TYPE_TOUCH = 2;
        /**
         * Constant for {@code dataType} representing sensor data from a gravity
         * sensor.
         */
        private static final int TYPE_SENSOR_GRAVITY = 0;
        /**
         * Constant for {@code dataType} representing sensor data from an
         * accelerometer.
         */
        private static final int TYPE_SENSOR_ACCELEROMETER = 1;
        /**
         * Data that was last passed in from
         * {@link UserInput#injectSensorData}.
         */
        private final float[] sensorData = new float[3];
        /**
         * Scratch array.
         */
        private final float[] mDataVec2, mOutVec3, mOutVec4, mValuesVec4, mTfMat4, mIdentityMat4;
        //<editor-fold defaultstate="collapsed" desc="Scratch Arrays">
        /**
         * Which data should be processed.
         */
        private volatile int dataType = -1;

        {
            mDataVec2 = new float[2];
            mOutVec3 = new float[3];
            mOutVec4 = new float[4];
            mValuesVec4 = new float[4];
            mTfMat4 = new float[16];
            mIdentityMat4 = new float[16];
            Matrix.setIdentityM(mIdentityMat4, 0);
        }
        //</editor-fold>

        /**
         * Processes {@code sensorData} and sends the result to its
         * {@link LevelUpdater}.
         */
        @Override
        public void run() {
            synchronized (sensorData) {
                System.arraycopy(sensorData, 0, mDataVec2, 0, mDataVec2.length);
            }
            switch (dataType) {
                case TYPE_SENSOR_ACCELEROMETER:
                    // TODO: Low-pass filter
                case TYPE_SENSOR_GRAVITY:
                    setMotion(mDataVec2[0], mDataVec2[1]);
                    break;
                case TYPE_TOUCH:
                    // Maybe some sensitivity settings...?
                    setMotion(mDataVec2[0], mDataVec2[1]);
            }
        }

        /**
         * Converts an array of a given size to another size by padding with zeros
         * or clipping off the extra values.
         *
         * @param in  input array
         * @param out array to be filled with {@code in}
         */
        private void convertArray(float[] in, float[] out) {
            for (int i = 0; i < out.length; i++) {
                out[i] = (i < in.length ? in[i] : 0);
            }
        }

        /**
         * Inject data from the sensors to process. Also re-maps the coordinate
         * system based on the current orientation.
         *
         * @param event       the {@link SensorEvent} to process
         * @param orientation the current orientation of the device
         */
        @SuppressWarnings("SuspiciousNameCombination")
        public void injectSensorData(SensorEvent event, int orientation) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_GRAVITY:
                    dataType = TYPE_SENSOR_GRAVITY;
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    dataType = TYPE_SENSOR_ACCELEROMETER;
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("Invalid SensorEvent: %s", event.sensor.toString()));
            }
            Matrix.setIdentityM(mTfMat4, 0);
            switch (orientation) {
                case Surface.ROTATION_0:
                    SensorManager.remapCoordinateSystem(mIdentityMat4, SensorManager.AXIS_X, SensorManager.AXIS_Y, mTfMat4);
                    break;
                case Surface.ROTATION_90:
                    SensorManager.remapCoordinateSystem(mIdentityMat4, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, mTfMat4);
                    break;
                case Surface.ROTATION_180:
                    SensorManager.remapCoordinateSystem(mIdentityMat4, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, mTfMat4);
                    break;
                case Surface.ROTATION_270:
                    SensorManager.remapCoordinateSystem(mIdentityMat4, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mTfMat4);
            }

            convertArray(event.values, mValuesVec4);
            Matrix.multiplyMV(mOutVec4, 0, mTfMat4, 0, mValuesVec4, 0);
            convertArray(mOutVec4, mOutVec3);
//            System.out.println(Arrays.toString(event.values) + " --(" + orientation + ")-> " + Arrays.toString(mScratchVec3));

            synchronized (sensorData) {
                System.arraycopy(mOutVec3, 0, sensorData, 0, sensorData.length);
            }
        }
    }
}
