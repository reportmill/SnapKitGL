package snapgl;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4bc;
import com.jogamp.opengl.GLAutoDrawable;
import javax.swing.*;
import java.awt.*;

/**
 * A class to test RenderJOGL.
 */
public class TestRenderer extends RenderImage {

    // Panel
    private JPanel  _contentPane;

    // Constants
    private static final int BOX_WIDTH = 500;
    private static final int BOX_HEIGHT = 500;

    /**
     * Constructor.
     */
    public TestRenderer()
    {
        // Normal version
        super(BOX_WIDTH, BOX_HEIGHT);

        // Create panel to paint 3D
        _contentPane = new JPanel() {
            protected void paintComponent(Graphics aGfx) {
                paintToGraphics2D((Graphics2D) aGfx); } };
        _contentPane.setPreferredSize(new Dimension(BOX_WIDTH, BOX_HEIGHT));

        // Create JFrame and show
        JFrame frame = new JFrame("Simple LWJGL Test");
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
     * Renders the RenderJOGL, then triggers repaint to get it in Panel.
     */
    void renderLoop()
    {
        renderAll();
        _contentPane.repaint();
        SwingUtilities.invokeLater(() -> renderLoop());
    }

    public void display(GLAutoDrawable drawable)
    {
        render3D();
    }

    /**
     * Called to execute custom OpenGL in given RenderJOGL.
     */
    protected void render3D()
    {
        GL2 gl = getGL2();

        // Get view size and aspect
        int scale = 2;
        int viewW = getWidth() * scale;
        int viewH = getHeight() * scale;
        float aspect = (float) viewW / viewH;

        // Get width based on current time
        double now = System.currentTimeMillis() * 0.001;
        float width = (float) Math.abs(Math.sin(now * 0.3));

        // OpenGL to reset viewport
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0, 0, viewW, viewH);

        // Custom OpenGL for diamond
        gl.glBegin(GL4bc.GL_QUADS);
        gl.glColor3f(0.4f, 0.6f, 0.8f);
        gl.glVertex2f(-0.75f * width / aspect, 0.0f);
        gl.glVertex2f(0, -0.75f);
        gl.glVertex2f(0.75f * width / aspect, 0);
        gl.glVertex2f(0, +0.75f);
        gl.glEnd();
    }

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        new TestRenderer();
    }
}
