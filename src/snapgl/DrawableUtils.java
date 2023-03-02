/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapgl;
import com.jogamp.opengl.*;
import jogamp.opengl.GLDrawableHelper;
import snap.gfx.GFXEnv;

/**
 * Handy JOGL utility methods.
 */
public class DrawableUtils {

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
        glCaps.setNumSamples(4);

        // Increase depth buffer
        glCaps.setDepthBits(24);

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
        //return createOffScreenGLWindow(glCaps, aW, aH); // Once needed for MacOS problems
        return createOffScreenDrawable(glCaps, aW, aH);
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

        // Get size for screen scale
        double screenScale = GFXEnv.getEnv().getScreenScale();
        int winW = (int) Math.round(aW * screenScale);
        int winH = (int) Math.round(aH * screenScale);

        // Create drawable
        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
        GLAutoDrawable drawable = factory.createOffscreenAutoDrawable(null, glCaps, null, winW, winH);

        // Return
        return drawable;
    }

    /**
     * Resizes drawable.
     */
    public static void resizeDrawable(GLAutoDrawable drawable, int aWidth, int aHeight)
    {
        // Make sure drawable context is set
        GL gl = drawable.getGL();
        GLContext glc = gl.getContext();
        glc.makeCurrent();

        // Reshape
        new GLDrawableHelper().reshape(drawable, 0, 0, aWidth, aHeight);
    }

//    /**
//     * Returns offscreen GLWindow as work around for multi-sampling on mac. Otherwise, try glCaps.setSampleBuffers(false);
//     */
//    public static GLWindow createOffScreenGLWindow(GLCapabilities glCaps, int aW, int aH)
//    {
//        // Make sure capabilities are set
//        if (glCaps == null)
//            glCaps = getDefaultOffscreenCapabilities();
//
//        // Create/configure window and return
//        GLWindow glWindow = GLWindow.create(glCaps);
//        glWindow.setUndecorated(true);
//        glWindow.setSize(aW, aH);
//
//        // Need window visible to work, but try to keep it hidden
//        int screenH = Toolkit.getDefaultToolkit().getScreenSize().height;
//        glWindow.setPosition(0, screenH - 1);
//        glWindow.setVisible(true);
//        glWindow.setPosition(0, screenH + 1);
//
//        // Return
//        return glWindow;
//    }
//
//    /**
//     * Resize GLOffscreenAutoDrawable.
//     */
//    public static void resizeGLOffscreenAutoDrawable(GLOffscreenAutoDrawable offscreenDrawable, int aWidth, int aHeight)
//    {
//        double screenScale = GFXEnv.getEnv().getScreenScale();
//        int pixW = (int) Math.round(aWidth * screenScale);
//        int pixH = (int) Math.round(aHeight * screenScale);
//        offscreenDrawable.setSurfaceSize(pixW, pixH);
//    }
}
