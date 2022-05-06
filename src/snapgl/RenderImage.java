/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapgl;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This class renders OpenGL to image using JOGL.
 */
public class RenderImage implements GLEventListener {

    // The image size
    protected int  _width, _height;

    // GLAutoDrawable to do real work of setting render environment up
    private GLAutoDrawable  _drawable;

    // Used to read rendered GL to BufferedImage
    private AWTGLReadBufferUtil  _glReadUtil;

    /**
     * Constructor.
     */
    public RenderImage(int aWidth, int aHeight)
    {
        _width = aWidth;
        _height = aHeight;
    }

    /**
     * Return image width.
     */
    public int getWidth()  { return _width; }

    /**
     * Return image height.
     */
    public int getHeight()  { return _height; }

    /**
     * Resets the size.
     */
    public void setSize(int aWidth, int aHeight)
    {
        // If already at size, just return
        if (aWidth == _width && aHeight == _height) return;

        // Handle GLWindow
        GLAutoDrawable drawable = getDrawable();
        DrawableUtils.resizeDrawable(drawable, aWidth, aHeight);

        // Reset sizes and clear ReadUtil
        _width = aWidth;
        _height = aHeight;
        _glReadUtil = null;
    }

    /**
     * Returns image for current rendering.
     */
    public BufferedImage getImage()
    {
        // Get GL and context
        GL gl = getGL2(); if (gl == null) return null;
        GLContext glc = gl.getContext(); if (glc == null) return null;
        glc.makeCurrent();

        // Get glReadUtil
        if (_glReadUtil == null)
            _glReadUtil = new AWTGLReadBufferUtil(gl.getGLProfile(), true);

        // Read to image
        BufferedImage img = _glReadUtil.readPixelsToBufferedImage(gl, true);
        return img;
    }

    /**
     * Paints 3D to Graphics2D.
     */
    public void renderAndPaintToGraphics2D(Graphics2D aGfx)
    {
        renderAll();
        paintToGraphics2D(aGfx);
    }

    /**
     * Triggers render.
     */
    public void renderAll()
    {
        GLAutoDrawable drawable = getDrawable();
        drawable.display();
    }

    /**
     * Paints 3D to Graphics2D.
     */
    public void paintToGraphics2D(Graphics2D aGfx)
    {
        // Get image for 3D and paint to graphics
        BufferedImage img = getImage();
        if (img == null) {
            System.err.println("RenderImage.paint3DToGraphics2D: Image is null"); return; }

        // Paint image
        aGfx.drawImage(img, 0, 0, _width, _height, null);
    }

    /**
     * Returns offscreen GLAutoDrawable.
     */
    public GLAutoDrawable getDrawable()
    {
        // If already set, just return
        if (_drawable != null) return _drawable;

        // Create Drawable, add GLEventListener, set and return
        GLAutoDrawable drawable = createDrawable();
        drawable.addGLEventListener(this);
        return _drawable = drawable;
    }

    /**
     * Creates the drawable.
     */
    protected GLAutoDrawable createDrawable()
    {
        GLAutoDrawable drawable = DrawableUtils.createOffScreenDrawableDefault(null, _width, _height);
        return drawable;
    }

    /**
     * Returns the GL2.
     */
    public GL2 getGL2()
    {
        GLAutoDrawable drawable = getDrawable();
        GL gl = drawable.getGL();
        GL2 gl2 = gl.getGL2();
        return gl2;
    }

    /**
     * Override to do custom init.
     */
    @Override
    public void init(GLAutoDrawable drawable)
    {
        GL2 gl2 = getGL2();
        gl2.glClearColor(0f, 0f, 0f, 0f);
        gl2.glEnable(GL.GL_DEPTH_TEST);
        gl2.glEnable(GL.GL_CULL_FACE);
    }

    /**
     * Override to do custom rendering.
     */
    @Override
    public void display(GLAutoDrawable drawable)  { }

    /**
     * Override for custom resize code.
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)  { }

    /**
     * Override for custom cleanup code.
     */
    @Override
    public void dispose(GLAutoDrawable drawable)  { }
}
