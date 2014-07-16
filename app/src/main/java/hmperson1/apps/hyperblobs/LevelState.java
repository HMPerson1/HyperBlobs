package hmperson1.apps.hyperblobs;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable.
 *
 * @author HMPerson1
 */
public class LevelState {

    /**
     * List of walls. Each wall is a list of two pairs of integers: its center
     * point and it's half-dimensions.
     */
    private final List<List<Point>> mWalls;
    /**
     * List of spikes.
     */
    private final List<Point> mSpikes;
    /**
     * Position of the blob.
     */
    private final Point mBlob;

    /**
     * Creates and populates a state
     *
     * @param walls  list of walls
     * @param spikes list of spikes
     * @param blob   position of blob
     */
    @SuppressWarnings({"WeakerAccess", "AssignmentToCollectionOrArrayFieldFromParameter"})
    protected LevelState(List<List<Point>> walls,
                         List<Point> spikes,
                         Point blob) {
        mWalls = walls;
        mSpikes = spikes;
        mBlob = blob;
    }

    /**
     * Returns the position of the blob.
     *
     * @return a copy of the position of the blob
     */
    public Point getBlob() {
        return new Point(mBlob);
    }

    /**
     * Returns the list of spikes.
     *
     * @return an unmodifiable list of spikes
     */
    public List<Point> getSpikes() {
        return Collections.unmodifiableList(mSpikes); // Extra redundancy just in case
    }

    /**
     * Returns the list of walls.
     *
     * @return an unmodifiable list of walls
     */
    public List<List<Point>> getWalls() {
        return Collections.unmodifiableList(mWalls);
    }

    /**
     * Builds a {@link LevelState} part by part.
     */
    @SuppressWarnings({"PublicInnerClass", "ClassNamingConvention"})
    public static class Builder {

        /**
         * Cached Comparator to avoid unnecessary object creation. Sorts points
         * by x, then by y.
         */
        @SuppressWarnings("OverlyComplexAnonymousInnerClass")
        private static final Comparator<Point> comp = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                if (lhs.x < rhs.x) {
                    return -1;
                } else if (lhs.x > rhs.x) {
                    return 1;
                } else if (lhs.y < rhs.y) {
                    return -1;
                } else if (lhs.y > rhs.y) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        /**
         * Current list of walls.
         */
        private final List<List<Point>> mWalls = new ArrayList<List<Point>>();
        /**
         * Current list of spikes.
         */
        private final List<Point> mSpikes = new ArrayList<Point>();
        /**
         * Position of blob.
         */
        private final Point mBlob = new Point();
        //<editor-fold defaultstate="collapsed" desc="Cached Objects">
        /**
         * Cached array of points.
         */
        private final Point[] points = new Point[4];

        { // Initialize the array
            for (int i = 0; i < 4; i++) {
                points[i] = new Point();
            }
        }

        /**
         * Sets the position of the blob.
         *
         * @param x center x coordinate
         * @param y center y coordinate
         * @return this object for convenience
         */
        public Builder setBlob(int x, int y) {
            mBlob.set(x, y);
            return this;
        }
        //</editor-fold>

        /**
         * Adds a wall given 4 points.
         *
         * @param x1 first point
         * @param y1 first point
         * @param x2 second point
         * @param y2 second point
         * @param x3 third point
         * @param y3 third point
         * @param x4 fourth point
         * @param y4 fourth point
         * @return this object for convenience
         */
        public Builder addWall(int x1, int y1,
                               int x2, int y2,
                               int x3, int y3,
                               int x4, int y4) {
            points[0].set(x1, y1);
            points[1].set(x2, y2);
            points[2].set(x3, y3);
            points[3].set(x4, y4);

            // Orders the points from bottom-left to top-right; down-up
            Arrays.sort(points, comp);

            int height = points[1].y - points[0].y;
            int width = points[3].x - points[1].x;

            // Ensure that it is actually a rectangle
            if (((points[3].y - points[2].y) != height) || ((points[2].x - points[0].x) != width)) {
                throw new IllegalArgumentException("Not a rectangle!");
            }

            // Actually add the wall now
            List<Point> rect = new ArrayList<Point>(2);

            int hw = width / 2;
            int hh = height / 2;

            rect.add(new Point(points[0].x + hw, points[0].y + hh)); // center point
            rect.add(new Point(hw, hh)); // half-width and half-height

            mWalls.add(Collections.unmodifiableList(rect));

            return this;
        }

        /**
         * Adds a wall.
         *
         * @param x       center x coordinate
         * @param y       center y coordinate
         * @param hWidth  half-width
         * @param hHeight half-height
         * @return this object for convenience
         */
        public Builder addWall(int x, int y, int hWidth, int hHeight) {
            List<Point> rect = new ArrayList<Point>(2);

            rect.add(new Point(x, y)); // center point
            rect.add(new Point(hWidth, hHeight)); // half-width and half-height

            mWalls.add(Collections.unmodifiableList(rect));
            return this;
        }

        /**
         * Adds a spike.
         *
         * @param x center x coordinate
         * @param y center y coordinate
         * @return this object for convenience
         */
        public Builder addSpike(int x, int y) {
            mSpikes.add(new Point(x, y));
            return this;
        }

        /**
         * Builds the {@link LevelState} with the given information.
         *
         * @return the completed object.
         */
        public LevelState build() {
            return new LevelState(Collections.unmodifiableList(mWalls),
                    Collections.unmodifiableList(mSpikes), mBlob);
        }
    }
}
