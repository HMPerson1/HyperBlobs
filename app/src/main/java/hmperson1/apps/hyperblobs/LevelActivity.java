package hmperson1.apps.hyperblobs;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * @author HMPerson1
 */
public class LevelActivity extends Activity implements SensorEventListener {

    /**
     * Name for logging.
     */
    static final String NAME_LOGGING = "HyperBlobs";
    /**
     * Our updater thread.
     */
    LevelUpdater mUpdater;
    /**
     * ID of the level.
     */
    int major, minor;
    /**
     * The {@link SensorManager}.
     */
    private SensorManager mSensorManager;
    /**
     * The sensor we will be using.
     */
    private Sensor mSensor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        getActionBar().hide();
        setContentView(R.layout.activity_level);
        LevelLoader.init(getResources());
        GLPolyTools.init(getResources());

        int[] levelId = getIntent().getIntArrayExtra("hmperson1.apps.hyperblobs.LevelId");
        major = levelId[0];
        minor = levelId[1];

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        } else {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        new InitTask().execute((Void) null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mUpdater != null) {
            mUpdater.input.injectSensorData(event, getWindowManager().getDefaultDisplay().getRotation());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((mSensorManager != null) && (mSensor != null)) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (mUpdater != null) {
            mUpdater.setRunning(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mUpdater != null) {
            mUpdater.setRunning(false);
        }
        if ((mSensorManager != null) && (mSensor != null)) {
            mSensorManager.unregisterListener(this, mSensor);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUpdater.setRunning(false);
        mUpdater.interrupt();
    }

    /**
     * Loads a level and creates our {@link LevelUpdater}.
     */
    private class InitTask extends AsyncTask<Void, Void, LevelUpdater> {

        InitTask() {
        }

        @Override
        protected LevelUpdater doInBackground(Void... params) {
            LevelPhysicsUpdater.loadClasses();

            LevelLoader loader = new LevelLoader(major, minor);
            LevelState level;
            try {
                level = loader.call();
            } catch (Exception e) {
                Log.i(NAME_LOGGING, "Failed to load level: " + loader.resName, e);
                return null;
            }

            return new LevelUpdater(level);
        }

        @SuppressWarnings("RefusedBequest")
        @Override
        protected void onPostExecute(LevelUpdater result) {
            // Fill in fields
            mUpdater = result;

            // Get rid of the progress view
            findViewById(R.id.progress).setVisibility(View.GONE);

            // Get this show on the road
            mUpdater.start();
            setContentView(new LevelView(LevelActivity.this, result.getRenderer()));
        }
    }
}
