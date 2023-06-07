/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapgl;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.gfx.Painter;
import snap.gfx3d.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.opengl.*;
import snap.props.PropChange;

/**
 * A Renderer implementation for OpenGL with JOGL.
 */
public class JGLRenderer extends Renderer {

    // A RenderImage
    private RenderImage  _renderImage;

    // A map of shader programs
    private Map<String, JGLProgram> _programs = new HashMap<>();

    // A map of vertex shaders
    private Map<String, JGLShader>  _vertShaders = new HashMap<>();

    // A map of fragment shaders
    private Map<String, JGLShader>  _fragShaders = new HashMap<>();

    // A map of textures
    private Map<Texture,com.jogamp.opengl.util.texture.Texture>  _textures = new HashMap<>();

    // Constant for name
    private static final String RENDERER_NAME = "JOGL";

    /**
     * Constructor.
     */
    public JGLRenderer(Camera aCamera)
    {
        super(aCamera);
    }

    /**
     * Returns the name.
     */
    @Override
    public String getName()  { return RENDERER_NAME; }

    /**
     * Override to render with RenderImage and paint to Graphics2D.
     */
    @Override
    public void renderAndPaint(Painter aPainter)
    {
        RenderImage renderImage = getRenderImage();
        Graphics2D gfx = (Graphics2D) aPainter.getNative();
        renderImage.renderAndPaintToGraphics2D(gfx);
    }

    /**
     * Returns the RenderImage.
     */
    public RenderImage getRenderImage()
    {
        // If already set, just return
        if (_renderImage != null) return _renderImage;

        // Get camera view size
        Camera camera = getCamera();
        int viewW = (int) Math.round(camera.getViewWidth());
        int viewH = (int) Math.round(camera.getViewHeight());

        // Create RenderImage for size
        RenderImage renderImage = new RenderImage(viewW, viewH) {
            public void display(GLAutoDrawable drawable) {
                JGLRenderer.this.renderAll();
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
        GLAutoDrawable drawable = getDrawable();
        GL gl = drawable.getGL();
        return gl.getGL2();
    }

    /**
     * The top level render method.
     */
    protected void renderAll()
    {
        // Get GL and clear
        GL2 gl = getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Get/set Viewport size
        GLAutoDrawable drawable = getDrawable();
        int viewW = drawable.getSurfaceWidth();
        int viewH = drawable.getSurfaceHeight();
        gl.glViewport(0, 0, viewW, viewH);

        // Iterate over scene shapes and render each
        Scene3D scene = getScene();
        renderShape3D(scene);
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
            VertexArray triangleArray = aShape3D.getTriangleArray();
            while (triangleArray != null) {
                renderTriangleArray(triangleArray);
                triangleArray = triangleArray.getNext();
            }
        }
    }

    /**
     * Renders the given triangle VertexArray.
     */
    protected void renderTriangleArray(VertexArray aTriangleArray)
    {
        // Get GL
        GL2 gl = getGL2();
        boolean doubleSided = aTriangleArray.isDoubleSided();
        if (doubleSided)
            gl.glDisable(GL.GL_CULL_FACE);

        // Get shader Program
        JGLProgram program = getProgram(aTriangleArray);

        // Use this program
        program.useProgram();

        // Set VertexShader Projection Matrix
        Camera camera = getCamera();
        double[] projMatrix = camera.getCameraToClipArray();
        program.setProjectionMatrix(projMatrix);

        // Set VertexShader Model Matrix
        double[] sceneToCamera = camera.getSceneToCameraArray();
        program.setViewMatrix(sceneToCamera);

        // Set VertexShader points
        float[] pointsArray = aTriangleArray.getPointArray();
        program.setPoints(pointsArray);

        // Set VertexShader color
        Color color = aTriangleArray.getColor();
        float[] colors = aTriangleArray.getColorArray();
        if (aTriangleArray.isColorArraySet())
            program.setColors(colors);
        else program.setColor(color);

        // Set VertexShader texture coords
        Texture texture = aTriangleArray.getTexture();
        float[] texCoords = aTriangleArray.getTexCoordArray();
        if (texture != null && texCoords != null && texCoords.length > 0) {
            com.jogamp.opengl.util.texture.Texture joglTexture = getTexture(texture);
            program.setTexture(joglTexture);
            program.setTexCoords(texCoords);
        }

        // Set IndexArray
        if (aTriangleArray.isIndexArraySet()) {
            int[] indexArray = aTriangleArray.getIndexArray();
            program.setIndexArray(indexArray);
        }

        // Run program
        program.runProgram();

        // Restore
        if (doubleSided)
            gl.glEnable(GL.GL_CULL_FACE);
    }

    /**
     * Returns a ShaderProgram for VertexArray.
     */
    public JGLProgram getProgram(VertexArray aVertexArray)
    {
        // If shader exists, return
        String name = getShaderString(aVertexArray);
        JGLProgram program = _programs.get(name);
        if (program != null)
            return program;

        // Create, set and return
        program = new JGLProgram(aVertexArray, this);
        _programs.put(name, program);
        return program;
    }

    /**
     * Returns a VertexShader for given VertexArray.
     */
    public JGLShader getVertexShader(VertexArray aVertexArray)
    {
        // If shader exists, return
        String name = getShaderString(aVertexArray);
        JGLShader shader = _vertShaders.get(name);
        if (shader != null)
            return shader;

        // Create, set and return
        shader = new JGLShader(JGLShader.ShaderType.Vertex, name, this);
        _vertShaders.put(name, shader);
        return shader;
    }

    /**
     * Returns a Fragment Shader for given VertexArray.
     */
    public JGLShader getFragmentShader(VertexArray aVertexArray)
    {
        // If shader exists, return
        String name = getShaderString(aVertexArray);
        JGLShader shader = _fragShaders.get(name);
        if (shader != null)
            return shader;

        // Create, set and return
        shader = new JGLShader(JGLShader.ShaderType.Fragment, name, this);
        _fragShaders.put(name, shader);
        return shader;
    }

    /**
     * Returns a JOGL texture for given Snap texture.
     */
    public com.jogamp.opengl.util.texture.Texture getTexture(Texture aTexture)
    {
        // Get from Textures map (Just return if found)
        com.jogamp.opengl.util.texture.Texture joglTexture = _textures.get(aTexture);
        if (joglTexture != null)
            return joglTexture;

        // Get BufferedImage and flip for OpenGL
        Image image = aTexture.getImage();
        BufferedImage awtImage = (BufferedImage) image.getNative();

        // Make sure texture is flipped
        if (!aTexture.isFlipped()) {
            ImageUtil.flipImageVertically(awtImage);
            aTexture.setFlipped(true);
        }

        // Create JOGLTexture from image
        GLProfile profile = getDrawable().getGLProfile();
        joglTexture = AWTTextureIO.newTexture(profile, awtImage, false);

        // Add to textures map and return
        _textures.put(aTexture, joglTexture);
        return joglTexture;
    }

    /**
     * Returns a unique string.
     */
    public String getShaderString(VertexArray aVertexArray)
    {
        // Handle TexCoordArray set
        boolean hasTexCoords = aVertexArray.isTexCoordArraySet();
        if (hasTexCoords)
            return "Points_Color_Tex";

        // Handle ColorArray set
        boolean hasColors = aVertexArray.isColorArraySet();
        return hasColors ? "Points_Colors" : "Points_Color";
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
        if (propName == Camera.ViewWidth_Prop || propName == Camera.ViewHeight_Prop) {
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
        Camera camera = getCamera();
        int viewW = (int) Math.round(camera.getViewWidth());
        int viewH = (int) Math.round(camera.getViewHeight());
        _renderImage.setSize(viewW, viewH);
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
        public Renderer newRenderer(Camera aCamera)
        {
            return new JGLRenderer(aCamera);
        }
    }
}
