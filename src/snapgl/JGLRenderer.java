/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapgl;
import snap.gfx.Color;
import snap.gfx.Painter;
import snap.gfx3d.*;
import java.awt.*;
import com.jogamp.opengl.*;
import snap.util.PropChange;

/**
 * A Renderer implementation for OpenGL with JOGL.
 */
public class JGLRenderer extends Renderer {

    // A RenderImage
    private RenderImage  _renderImage;

    // Constant for name
    private static final String RENDERER_NAME = "JOGL";

    /**
     * Constructor.
     */
    public JGLRenderer(Camera3D aCamera)
    {
        super(aCamera);
    }

    /**
     * Returns the name.
     */
    @Override
    public String getName()  { return RENDERER_NAME; }

    /**
     * Returns the RenderImage.
     */
    public RenderImage getRenderImage()
    {
        // If already set, just return
        if (_renderImage != null) return _renderImage;

        // Get camera view size
        Camera3D camera = getCamera();
        int viewW = (int) Math.round(camera.getViewWidth());
        int viewH = (int) Math.round(camera.getViewHeight());

        // Create RenderImage for size
        RenderImage renderImage = new RenderImage(viewW, viewH) {
            public void display(GLAutoDrawable drawable) {
                render3D();
            }
        };

        // Set, return
        return _renderImage = renderImage;
    }

    /**
     * Returns offscreen GLAutoDrawable.
     */
    public GLAutoDrawable getDrawable()
    {
        RenderImage renderJOGL = getRenderImage();
        return renderJOGL.getDrawable();
    }

    /**
     * Returns the GL2.
     */
    public GL2 getGL2()
    {
        RenderImage renderImage = getRenderImage();
        return renderImage.getGL2();
    }

    /**
     * Override to render scene.
     */
    @Override
    public void renderAll(Painter aPainter)
    {
        RenderImage renderImage = getRenderImage();
        Graphics2D gfx = (Graphics2D) aPainter.getNative();
        renderImage.renderAndPaintToGraphics2D(gfx);
    }

    /**
     * The method that renders.
     */
    protected void render3D()
    {
        // Get GL and clear
        GL2 gl = getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Get/set Viewport size
        GLAutoDrawable drawable = getDrawable();
        int viewW = drawable.getSurfaceWidth();
        int viewH = drawable.getSurfaceHeight();
        gl.glViewport(0, 0, viewW, viewH);

        // Get projection transform and set
        Camera3D camera = getCamera();
        double[] projTrans = camera.getCameraToClipArray();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadMatrixd(projTrans, 0);

        // Get camera transform and set
        double[] sceneToCamera = camera.getSceneToCameraArray();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(sceneToCamera, 0);

        // Iterate over scene shapes and render each
        Scene3D scene = getScene();
        renderShape3D(scene);

        // Paint axes
        JoglUtils.render3DAxisLines(gl, 3, 100);
    }

    /**
     * Renders a Shape3D.
     */
    protected void renderShape3D(Shape3D aShape3D)
    {
        // If shape not visible, just return
        if (!aShape3D.isVisible())
            return;

        // Handle Parent: Iterate over children and recurse
        if (aShape3D instanceof ParentShape) {
            ParentShape parentShape = (ParentShape) aShape3D;
            Shape3D[] children = parentShape.getChildren();
            for (Shape3D child : children)
                renderShape3D(child);
        }

        // Handle child: Get VertexArray and render
        else {
            VertexArray vertexArray = aShape3D.getVertexArray();
            while (vertexArray != null) {
                renderVertexArray(vertexArray);
                vertexArray = vertexArray.getNext();
            }
        }
    }

    /**
     * Renders a VertexBuffer of triangles.
     */
    protected void renderVertexArray(VertexArray aVertexArray)
    {
        // Get Vertex points components array
        float[] pointsArray = aVertexArray.getPointArray();
        if (pointsArray.length == 0)
            return;

        // Get Vertex color components array
        float[] colorsArray = aVertexArray.isColorArraySet() ? aVertexArray.getColorArray() : null;

        // Get GL
        GL2 gl = getGL2();
        boolean doubleSided = aVertexArray.isDoubleSided();
        if (doubleSided)
            gl.glDisable(GL.GL_CULL_FACE);

        // Start GL rendering for triangles
        gl.glBegin(GL.GL_TRIANGLES);

        // If no color components, set global color
        if (colorsArray == null) {
            Color color = aVertexArray.getColor();
            if (color != null)
                gl.glColor4d(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            else System.err.println("JGLRenderer.renderVertexArray: No colors defined for VertexArray");
        }

        // Iterate over triangles and paint
        for (int i = 0, iMax = aVertexArray.getPointCount(), pointIndex = 0, colorIndex = 0; i < iMax; i++) {

            // Get/set vertex color (if present)
            if (colorsArray != null) {
                float red = colorsArray[colorIndex++];
                float green = colorsArray[colorIndex++];
                float blue = colorsArray[colorIndex++];
                gl.glColor4f(red, green, blue, 1);
            }

            // Get/set vertex point
            float px = pointsArray[pointIndex++];
            float py = pointsArray[pointIndex++];
            float pz = pointsArray[pointIndex++];
            gl.glVertex3d(px, py, pz);
        }

        // Close
        gl.glEnd();

        // Restore
        if (doubleSided)
            gl.glEnable(GL.GL_CULL_FACE);
    }

    /**
     * Override to handle ViewWidth/ViewHeight changes.
     */
    @Override
    protected void cameraDidPropChange(PropChange aPC)
    {
        // Do normal version
        super.cameraDidPropChange(aPC);

        // Handle ViewWidth, ViewHeight special
        String propName = aPC.getPropName();
        if (propName == Camera3D.ViewWidth_Prop || propName == Camera3D.ViewHeight_Prop) {
            resizeDrawableToCameraViewSize();
        }
    }

    /**
     * Resize drawable.
     */
    private void resizeDrawableToCameraViewSize()
    {
        // If RenderJOGL not set, just return
        if (_renderImage == null) return;

        // Get Camera ViewSize and set
        Camera3D camera = getCamera();
        int viewW = (int) Math.round(camera.getViewWidth());
        int viewH = (int) Math.round(camera.getViewHeight());
        _renderImage.setSize(viewW, viewH);
    }

    /**
     * Registers factory.
     */
    public static void registerFactory()
    {
        // If already set, just return
        for (RendererFactory factory : RendererFactory.getFactories())
            if (factory.getClass() == JGLRendererFactory.class)
                return;

        // Create, add and setDefault
        RendererFactory joglFactory = new JGLRendererFactory();
        RendererFactory.addFactory(joglFactory);
        RendererFactory.setDefaultFactory(joglFactory);
    }

    /**
     * A RendererFactory implementation for JGLRenderer.
     */
    public static class JGLRendererFactory extends RendererFactory {

        /**
         * Returns the renderer name.
         */
        public String getRendererName()  { return RENDERER_NAME; }

        /**
         * Returns a new default renderer.
         */
        public Renderer newRenderer(Camera3D aCamera)
        {
            return new JGLRendererX(aCamera);
        }
    }
}
