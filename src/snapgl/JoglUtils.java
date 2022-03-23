package snapgl;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;
import jogamp.opengl.GLDrawableHelper;
import snap.gfx.GFXEnv;
import java.nio.DoubleBuffer;

/**
 * Handy JOGL utility methods.
 */
public class JoglUtils {

    // The default Screen
    private static Screen  _screen0;

    /**
     * Creates default capabilities.
     */
    public static GLCapabilities getDefaultOffscreenCapabilities()
    {
        // Configure Debug/trace
        System.setProperty("jogl.debug.DebugGL", "true");
        //System.setProperty("jogl.debug.TraceGL", "true");

        // Get default profile
        GLProfile glProfile = GLProfile.getDefault();

        // Get/configure caps
        GLCapabilities glCaps = new GLCapabilities(glProfile);

        // No need to double-buffer offscreen drawable
        glCaps.setDoubleBuffered(false);

        // Configure basic multi-sampling
        glCaps.setSampleBuffers(true);
        glCaps.setNumSamples(8);

        // Don't know why we do this
        glCaps.setAlphaBits(8);

        // Print capabilities
        System.out.println("JOGL Caps: " + glCaps);

        // Return
        return glCaps;
    }

    /**
     * Returns an offscreen GLAutoDrawable. But this freezes on Mac.
     */
    public static GLAutoDrawable createOffScreenDrawableDefault(GLCapabilities glCaps, int aW, int aH)
    {
        return createOffScreenGLWindow(glCaps, aW, aH);
        //return createOffScreenDrawable(glCaps, aW, aH);
    }

    /**
     * Returns an offscreen GLWindow as work around for multi-sampling on mac.
     */
    public static GLWindow createOffScreenGLWindow(GLCapabilities glCaps, int aW, int aH)
    {
        // Make sure capabilities are set
        if (glCaps == null)
            glCaps = getDefaultOffscreenCapabilities();

        // Get screen height
        Screen screen = getScreen();

        // Create/configure window and return
        GLWindow glWindow = GLWindow.create(screen, glCaps);
        glWindow.setUndecorated(true);
        glWindow.setSize(aW, aH);

        // Need window visible to work, but try to keep it hidden
        int screenH = getScreenHeight();
        glWindow.setPosition(0, screenH - 1);
        glWindow.setVisible(true);
        glWindow.setPosition(0, screenH + 1);

        // Return window
        return glWindow;
    }

    /**
     * Resizes drawable.
     */
    public static void resizeDrawable(GLAutoDrawable drawable, int aWidth, int aHeight)
    {
        // Handle GLWindow
        if (drawable instanceof GLWindow) {
            GLWindow glWindow = (GLWindow) drawable;
            new GLDrawableHelper().reshape(glWindow, 0, 0, aWidth, aHeight);
        }

        // Handle GLOffscreenAutoDrawable
        else if (drawable instanceof GLOffscreenAutoDrawable) {
            double screenScale = GFXEnv.getEnv().getScreenScale();
            int pixW = (int) Math.round(aWidth * screenScale);
            int pixH = (int) Math.round(aHeight * screenScale);
            GLOffscreenAutoDrawable offscreenAutoDrawable = (GLOffscreenAutoDrawable) drawable;
            offscreenAutoDrawable.setSurfaceSize(pixW, pixH);
        }

        // Handle unknown
        else System.out.println("RenderJOGL.resizeDrawable: Unknown drawable class: " + drawable.getClass());
    }

    /**
     * Returns the screen.
     */
    private static Screen getScreen()
    {
        // If already set, just return
        if (_screen0 != null) return _screen0;

        //Collection<Screen> screens = Screen.getAllScreens();
        //Screen screen = screens.stream().findFirst().get();
        Display display = NewtFactory.createDisplay(null);
        display.addReference();
        Screen screen = NewtFactory.createScreen(display, 0);
        screen.addReference();

        // Set/return screen
        return _screen0 = screen;
    }

    /**
     * Returns the screen height.
     */
    private static int getScreenHeight()
    {
        Screen screen = getScreen();
        return screen.getViewportInWindowUnits().getHeight();
    }

    /**
     * Returns the screen scale.
     */
    public static double getScreenScale()
    {
        double screenScale = GFXEnv.getEnv().getScreenScale();
        return screenScale;
    }

    /**
     * Returns an offscreen GLAutoDrawable. But this freezes on Mac.
     */
    public static GLAutoDrawable createOffScreenDrawable(GLCapabilities glCaps, int aW, int aH)
    {
        // Make sure capabilities are set
        if (glCaps == null)
            glCaps = getDefaultOffscreenCapabilities();
        GLProfile profile = glCaps.getGLProfile();

        // Turn off multi-sampling, otherwise Mac will freeze painting
        glCaps.setSampleBuffers(false);

        // Get size for screen scale
        double screenScale = getScreenScale();
        int winW = (int) Math.round(aW * screenScale);
        int winH = (int) Math.round(aH * screenScale);

        // Create drawable
        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
        GLAutoDrawable drawable = factory.createOffscreenAutoDrawable(null, glCaps, null, winW, winH);

        // Add callbacks and return
        return drawable;
    }

    /**
     * Render axis.
     */
    public static void render3DAxisLines(GL2 gl, GLUT glut, double aRadius, double aHeight)
    {
        // Get info
        int slices = 32;
        int stacks = 32;

        // Make sure it draws in front
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

        // Save transform
        gl.glPushMatrix();

        // Render X axis
        gl.glColor3d(1d, 0d, 0d);
        gl.glRotated(-90, 1, 0, 0);
        glut.glutSolidCylinder(aRadius, aHeight, slices, stacks);
        gl.glRotated(90, 1, 0, 0);

        // Render Y axis
        gl.glColor3d(0d, 1d, 0);
        gl.glRotated(90, 0, 1, 0);
        glut.glutSolidCylinder(aRadius, aHeight, slices, stacks);
        gl.glRotated(-90, 0, 1, 0);

        // Render Z axis
        gl.glColor3d(0d, 0d, 1d);
        glut.glutSolidCylinder(aRadius, aHeight, slices, stacks);

        // Restore transform
        gl.glPopMatrix();
    }

    /**
     * Renders a line as a cylinder between points.
     */
    public static void renderLineAsCylinder(GL2 gl2, GLU glu,
            double x0, double y0, double z0, double x1, double y1, double z1, double aRadius)
    {
        double dir_x = x1-x0;
        double dir_y = y1-y0;
        double dir_z = z1-z0;
        double bone_length = Math.sqrt (dir_x * dir_x + dir_y * dir_y + dir_z * dir_z);
        GLUquadric quad_obj = glu.gluNewQuadric();

        glu.gluQuadricDrawStyle (quad_obj, GLU.GLU_FILL);
        glu.gluQuadricNormals (quad_obj, GLU.GLU_SMOOTH);
        gl2.glPushMatrix();
        //Pan to the starting point
        gl2.glTranslated (x0, y0, z0);
        //Calculate the length
        double length = Math.sqrt (dir_x * dir_x + dir_y * dir_y + dir_z * dir_z);
        if (length <0.0001) {
            dir_x = 0.0; dir_y = 0.0; dir_z = 1.0; length = 1.0;
        }
        dir_x/= length; dir_y/= length; dir_z/= length;
        double up_x, up_y, up_z;
        up_x = 0.0;
        up_y = 1.0;
        up_z = 0.0;
        double side_x, side_y, side_z;
        side_x = up_y * dir_z-up_z * dir_y;
        side_y = up_z * dir_x-up_x * dir_z;
        side_z = up_x * dir_y-up_y * dir_x;
        length = Math.sqrt (side_x * side_x + side_y * side_y + side_z * side_z);
        if (length <0.0001) {
            side_x = 1.0; side_y = 0.0; side_z = 0.0; length = 1.0;
        }
        side_x/= length; side_y/= length; side_z/= length;
        up_x = dir_y * side_z-dir_z * side_y;
        up_y = dir_z * side_x-dir_x * side_z;
        up_z = dir_x * side_y-dir_y * side_x;
        //Calculate the transformation matrix
        double m[] = { side_x, side_y, side_z, 0.0,
            up_x, up_y, up_z, 0.0,
            dir_x, dir_y, dir_z, 0.0,
            0.0, 0.0, 0.0, 1.0};
        DoubleBuffer mdb = DoubleBuffer.wrap(m);
        gl2.glMultMatrixd (mdb);
        //Cylinder parameters
        int slices = 8;//number of segments
        int stack = 3;//Recursion times
        glu.gluCylinder(quad_obj, aRadius, aRadius, bone_length, slices, stack);
        gl2.glPopMatrix();
    }
}
