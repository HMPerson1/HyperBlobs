package hmperson1.apps.hyperblobs;

import android.graphics.Point;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

import java.util.ArrayList;
import java.util.List;

import static hmperson1.apps.hyperblobs.LevelUpdater.BLOB_RADIUS;
import static hmperson1.apps.hyperblobs.LevelUpdater.HEIGHT;
import static hmperson1.apps.hyperblobs.LevelUpdater.INV_SCALE;
import static hmperson1.apps.hyperblobs.LevelUpdater.SCALE;
import static hmperson1.apps.hyperblobs.LevelUpdater.SPIKE_RADIUS;

/**
 * Updates physics. This is in a 36x96m world.
 *
 * @author HMPerson1
 */
class LevelPhysicsUpdater implements Runnable, ContactListener {

    /**
     * Amount of time to be emulated each run.
     */
    private static final float TIME_STEP = (float) 1 / 60;
    /**
     * World in which everything happens.
     */
    private final World mWorld;
    /**
     * Detects when the blob is off the screen.
     */
    private final Body mScreenDet;
    /**
     * List of all the walls.
     */
    private final List<Body> mWalls;
    /**
     * List of all the spikes.
     */
    private final List<Body> mSpikes;
    /**
     * Main Blob.
     */
    private final Body mBlob;
    /**
     * Current state of the world.
     */
    private LevelState mState;

    /**
     * Creates a new updater.
     *
     * @param state  the value of state
     * @param length the value of length
     */
    public LevelPhysicsUpdater(LevelState state, int length) {
        mWorld = new World(new Vec2(0, 0));

        // Off-screen detectors
        // Define
        BodyDef screenDetDef = new BodyDef();
        screenDetDef.position.set((length * INV_SCALE) / 2, (HEIGHT * INV_SCALE) / 2);
        PolygonShape screenDetShape = new PolygonShape();
        screenDetShape.setAsBox((length * INV_SCALE) / 2, (HEIGHT * INV_SCALE) / 2);
        // Create
        mScreenDet = mWorld.createBody(screenDetDef);
        mScreenDet.createFixture(screenDetShape, 0);
        mScreenDet.getFixtureList().setSensor(true);

        // Walls
        BodyDef wallDef = new BodyDef();
        PolygonShape wallShape = new PolygonShape();
        mWalls = new ArrayList<Body>(state.getWalls().size());
        for (List<Point> wall : state.getWalls()) {
            Point pos = wall.get(0);
            Point size = wall.get(1);
            // Define
            wallDef.position.set(pos.x * INV_SCALE, pos.y * INV_SCALE);
            wallShape.setAsBox(size.x * INV_SCALE, size.y * INV_SCALE);
            // Create
            Body wallBody = mWorld.createBody(wallDef);
            wallBody.createFixture(wallShape, 0);
            mWalls.add(wallBody);
        }

        // Spikes
        BodyDef spikeDef = new BodyDef();
        spikeDef.type = BodyType.DYNAMIC;
        CircleShape spikeShape = new CircleShape();
        spikeShape.m_radius = SPIKE_RADIUS * INV_SCALE;
        mSpikes = new ArrayList<Body>(state.getSpikes().size());
        for (Point spike : state.getSpikes()) {
            // Define
            spikeDef.position.set(spike.x * INV_SCALE, spike.y * INV_SCALE);
            // Create
            Body spikeBody = mWorld.createBody(spikeDef);
            spikeBody.createFixture(spikeShape, 1);
            mSpikes.add(spikeBody);
        }

        // Blob
        Point blob = state.getBlob();
        // Define
        BodyDef blobDef = new BodyDef();
        blobDef.allowSleep = false;
        blobDef.position.set(blob.x * INV_SCALE, blob.y * INV_SCALE);
        blobDef.type = BodyType.DYNAMIC;
        CircleShape blobShape = new CircleShape();
        blobShape.m_radius = BLOB_RADIUS * INV_SCALE;
        // Create
        mBlob = mWorld.createBody(blobDef);
        mBlob.createFixture(blobShape, 1);

        registerListeners();
    }

    /**
     * Forces the loading of jBox2D classes.
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void loadClasses() {
        MathUtils.sinLUT(0);
        new World(new Vec2());
    }

    /**
     * Sets the velocity of the blob. (m/s)
     *
     * @param x velocity
     * @param y velocity
     */
    public void setBlobVelocity(float x, float y) {
        mBlob.setLinearVelocity(new Vec2(x, y));
    }

    /**
     * Updates {@code mData} to reflect the current state of the world.
     */
    private void updateState() {
        LevelState.Builder builder = new LevelState.Builder();
        PolygonShape shape;
        Vec2[] wp = new Vec2[4];
        for (int i = 0; i < 4; i++) {
            wp[i] = new Vec2();
        }

        // Walls
        for (Body wall : mWalls) {
            shape = (PolygonShape) wall.getFixtureList().getShape();
            for (int i = 0; i < 4; i++) {
                Transform.mulToOut(wall.getTransform(), shape.getVertex(i), wp[i]);
            }
            builder.addWall((int) (wp[0].x * SCALE), (int) (wp[0].y * SCALE),
                    (int) (wp[1].x * SCALE), (int) (wp[1].y * SCALE),
                    (int) (wp[2].x * SCALE), (int) (wp[2].y * SCALE),
                    (int) (wp[3].x * SCALE), (int) (wp[3].y * SCALE));
        }

        // Spikes
        for (Body spike : mSpikes) {
            wp[0] = spike.getWorldCenter();
            builder.addSpike((int) (wp[0].x * SCALE), (int) (wp[0].y * SCALE));
        }

        // Blob
        wp[0] = mBlob.getWorldCenter();
        builder.setBlob((int) (wp[0].x * SCALE), (int) (wp[0].y * SCALE));

        mState = builder.build();
    }

    /**
     * Returns the current state of the world.
     *
     * @return the current state
     */
    public LevelState getState() {
        return mState;
    }

    /**
     * Updates physics and collisions.
     */
    @Override
    public void run() {
        mWorld.step(TIME_STEP, 8, 3);
        updateState();
    }

    /**
     * Registers listeners.
     */
    private void registerListeners() {
        mWorld.setContactListener(this);
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        Fixture screenDet = mScreenDet.getFixtureList();
        Fixture blob = mBlob.getFixtureList();
        if (((a == screenDet) && (b == blob)) || ((b == screenDet) && (a == blob))) {
            System.out.println("+++On--Screen!+++");
        }
    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        Fixture screenDet = mScreenDet.getFixtureList();
        Fixture blob = mBlob.getFixtureList();
        if (((a == screenDet) && (b == blob)) || ((b == screenDet) && (a == blob))) {
            System.out.println("~~~Off-Screen!~~~");
        }
    }

    @Override
    public void preSolve(Contact arg0, Manifold arg1) {
    }

    @Override
    public void postSolve(Contact arg0, ContactImpulse arg1) {
    }
}
