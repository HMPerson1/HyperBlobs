package hmperson1.apps.hyperblobs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 * @author HMPerson1
 */
@SuppressLint("ViewConstructor")
class LevelView extends GLSurfaceView {

    public LevelView(Context context, Renderer renderer) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }
}
