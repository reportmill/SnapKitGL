package snapgl;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This class renders OpenGL to image or Graphics using JOGL.
 */
public class JoglTest implements GLEventListener {

    // The window size
    protected int  _width = 500, _height = 500;

    // GLAutoDrawable to do real work of setting render environment up
    private GLAutoDrawable  _drawable;

    // Used to read rendered GL to BufferedImage
    private AWTGLReadBufferUtil  _glReadUtil;

    // Panel to paint rendered image
    private JPanel  _contentPane;

    /**
     * Constructor.
     */
    public JoglTest()
    {
        // Create panel to paint 3D
        _contentPane = new JPanel() {
            protected void paintComponent(Graphics aGfx) {
                paint3DToGraphics2D((Graphics2D) aGfx); } };
        _contentPane.setPreferredSize(new Dimension(_width, _height));

        // Create JFrame and show
        JFrame frame = new JFrame("Simple JOGL Test");
        frame.setResizable(false);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(_contentPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Start render loop
        SwingUtilities.invokeLater(() -> renderLoop());
    }

    /**
     * Returns offscreen GLAutoDrawable.
     */
    public GLAutoDrawable getDrawable()
    {
        // If already set, just return
        if (_drawable != null) return _drawable;

        // Create Drawable
        GLAutoDrawable drawable = DrawableUtils.createOffScreenDrawableDefault(null, _width, _height);

        // Add GLEventListener, set and return
        drawable.addGLEventListener(this);
        return _drawable = drawable;
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
     * Renders the RenderJOGL, then triggers repaint to get it in Panel.
     */
    void renderLoop()
    {
        // Get drawable (if not valid, complain and return)
        GLAutoDrawable drawable = getDrawable();

        // Tell drawable to display/render
        drawable.display();

        // Repaint content pane
        _contentPane.repaint();

        // Come back again
        SwingUtilities.invokeLater(() -> renderLoop());
    }

    /**
     * Override to do custom init.
     */
    @Override
    public void init(GLAutoDrawable drawable)
    {
        GL2 gl2 = getGL2();
        gl2.glClearColor(1f, 1f, 1f, 1f);
        gl2.glEnable(GL.GL_DEPTH_TEST);
        gl2.glEnable(GL.GL_CULL_FACE);
    }

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

    /**
     * Override to do custom rendering.
     */
    @Override
    public void display(GLAutoDrawable drawable)
    {
        // Get width based on current time
        double now = System.currentTimeMillis() * 0.001;
        float width = (float) Math.abs(Math.sin(now * 0.3));
        float aspect = (float) _width / _height;

        // Get GL and clear
        GL2 gl = getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Get/set Viewport size
        int viewW = drawable.getSurfaceWidth();
        int viewH = drawable.getSurfaceHeight();
        gl.glViewport(0, 0, viewW, viewH);

        // Custom OpenGL for diamond
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor3f(0.4f, 0.6f, 0.8f);
        gl.glVertex2f(-0.75f * width / aspect, 0.0f);
        gl.glVertex2f(0, -0.75f);
        gl.glVertex2f(0.75f * width / aspect, 0);
        gl.glVertex2f(0, +0.75f);
        gl.glEnd();
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
    public void paint3DToGraphics2D(Graphics2D aGfx)
    {
        // Get image for 3D and paint to graphics
        BufferedImage img = getImage();
        if (img != null)
            aGfx.drawImage(img, 0, 0, _width, _height, null);
        else System.err.println("JoglTest.paint3DToGraphics2D: Image is null");
    }

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        new JoglTest();
    }
}
