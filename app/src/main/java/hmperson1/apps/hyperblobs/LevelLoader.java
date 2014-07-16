package hmperson1.apps.hyperblobs;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

/**
 * Loads levels from XML into {@link LevelState}s.
 *
 * @author HMPerson1
 */
class LevelLoader implements Callable<LevelState> {

    //<editor-fold defaultstate="collapsed" desc="State Machine Constants">
    private static final int BLOB_T = 10;
    private static final int BLOB_X = 11;
    private static final int BLOB_Y = 12;
    //
    private static final int WALL_T = 20;
    private static final int WALL_X = 21;
    private static final int WALL_Y = 22;
    private static final int WALL_W = 23;
    private static final int WALL_H = 24;
    //
    private static final int SPKE_T = 30;
    private static final int SPKE_X = 31;
    private static final int SPKE_Y = 32;
    /**
     * {@link Resources} to load XML from.
     */
    private static Resources res;
    /**
     * Name of the resource. Exposed for logging purposes.
     */
    public final String resName;
    /**
     * XML input.
     */
    private final XmlResourceParser input;

    /**
     * Creates a new {@code LevelLoader} with the given level ID.
     *
     * @param major major level ID
     * @param minor minor level ID
     */
    public LevelLoader(int major, int minor) {
        resName = String.format("level_%02d%02d", major, minor);
        int resId = 0;
        // Ugly Reflection Stuff
        try {
            resId = (Integer) R.xml.class.getField(resName).get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Unable to load: " + resName, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to load: " + resName, e);
        } finally {
            if (resId == 0) {
                //noinspection ThrowFromFinallyBlock
                throw new IllegalArgumentException("Unable to load: " + resName);
                // TODO: Handle more gracefully...?
            }
        }
        input = res.getXml(resId);
    }
    //</editor-fold>

    /**
     * Initialize with the Resources for XML loading.
     *
     * @param r the Resources from our package
     */
    public static void init(Resources r) {
        res = r;
    }

    /**
     * Loads a {@link LevelState} from the xml resource
     *
     * @return the {@code LevelState} represented by the resource
     * @throws Exception
     */
    @Override
    public LevelState call() throws Exception {
        LevelState.Builder builder = new LevelState.Builder();
        int state = 0; // State machine 'cause why not
        int[] cumulData = new int[4];
        Arrays.fill(cumulData, 0); // Initialize
        for (int type = input.getEventType();
             type != XmlResourceParser.END_DOCUMENT;
             type = input.next()) {
            String tag = input.getName();
            switch (type) {
                case START_TAG:
                    // Update state for the major tag
                    if ("blob".equals(tag)) {
                        state = BLOB_T;
                    } else if ("wall".equals(tag)) {
                        state = WALL_T;
                    } else if ("spke".equals(tag)) {
                        state = SPKE_T;
                    }
                    // Continue updating state for the inner tags
                    if ("x".equals(tag)) {
                        state += 1;
                    } else if ("y".equals(tag)) {
                        state += 2;
                    } else if ("hWidth".equals(tag)) {
                        state += 3;
                    } else if ("hHeight".equals(tag)) {
                        state += 4;
                    }
                    break;
                case TEXT:
                    // Text will always be numbers
                    int i = Integer.parseInt(input.getText());
                    // Cache the data
                    switch (state) {
                        case BLOB_X:
                        case WALL_X:
                        case SPKE_X:
                            cumulData[0] = i;
                            state -= 1; // Reset state
                            break;
                        case BLOB_Y:
                        case WALL_Y:
                        case SPKE_Y:
                            cumulData[1] = i;
                            state -= 2; // Reset state
                            break;
                        case WALL_W:
                            cumulData[2] = i;
                            state -= 3; // Reset state
                            break;
                        case WALL_H:
                            cumulData[3] = i;
                            state -= 4; // Reset state
                            break;
                    }
                    break;
                case END_TAG:
                    // The tag determines what kind of data we've been storing
                    if ("blob".equals(tag)) {
                        builder.setBlob(cumulData[0], cumulData[1]);
                        Arrays.fill(cumulData, 0); // Reset the data
                    } else if ("wall".equals(tag)) {
                        builder.addWall(cumulData[0], cumulData[1], cumulData[2], cumulData[3]);
                        Arrays.fill(cumulData, 0); // Reset the data
                    } else if ("spke".equals(tag)) {
                        builder.addSpike(cumulData[0], cumulData[1]);
                        Arrays.fill(cumulData, 0); // Reset the data
                    }
                    break;

            }
        }
//        Thread.sleep(3000);
        return builder.build();
    }
}
