package snapgl;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.gfx3d.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * A Renderer implementation using modern OpenGL (shaders).
 */
public class JGLRendererX extends JGLRenderer {

    // A map of shader programs
    private Map<String, JGLProgram>  _programs = new HashMap<>();

    // A map of vertex shaders
    private Map<String, JGLShader>  _vertShaders = new HashMap<>();

    // A map of fragment shaders
    private Map<String, JGLShader>  _fragShaders = new HashMap<>();

    // A map of textures
    private Map<Texture,com.jogamp.opengl.util.texture.Texture>  _textures = new HashMap<>();

    /**
     * Constructor.
     */
    public JGLRendererX(Camera3D aCamera)
    {
        super(aCamera);
    }

    /**
     * The method that renders.
     */
    @Override
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
        //double[] projTrans = getProjectionMatrix();
        //gl.glMatrixMode(GL2.GL_PROJECTION);
        //gl.glLoadMatrixd(projTrans, 0);

        // Get camera transform and set
        //double[] cameraTrans = getCameraTransform();
        //gl.glMatrixMode(GL2.GL_MODELVIEW);
        //gl.glLoadMatrixd(cameraTrans, 0);

        // Iterate over scene shapes and render each
        Scene3D scene = getScene();
        renderShape3D(scene);

        // Paint axes
        //RenderJOGL renderJOGL = getRenderJOGL();
        //JoglUtils.render3DAxisLines(gl, renderJOGL.getGlut(), 3, 100);
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
     * Override to render using modern OpenGL (shaders).
     */
    @Override
    protected void renderVertexArray(VertexArray aVertexArray)
    {
        // Get GL
        GL2 gl = getGL2();
        boolean doubleSided = aVertexArray.isDoubleSided();
        if (doubleSided)
            gl.glDisable(GL.GL_CULL_FACE);

        // Get shader Program
        JGLProgram program = getProgram(aVertexArray);

        // Use this program
        program.useProgram();

        // Set VertexShader Projection Matrix
        Camera3D camera = getCamera();
        double[] projMatrix = camera.getCameraToClipArray();
        program.setProjectionMatrix(projMatrix);

        // Set VertexShader Model Matrix
        double[] sceneToCamera = camera.getSceneToCameraArray();
        program.setViewMatrix(sceneToCamera);

        // Set VertexShader points
        float[] pointsArray = aVertexArray.getPointsArray();
        program.setPoints(pointsArray);

        // Set VertexShader color
        Color color = aVertexArray.getColor();
        float[] colors = aVertexArray.getColorsArray();
        if (aVertexArray.isColorsArraySet())
            program.setColors(colors);
        else program.setColor(color);

        // Set VertexShader texture coords
        Texture texture = aVertexArray.getTexture();
        float[] texCoords = aVertexArray.getTexCoordsArray();
        if (texture != null && texCoords != null && texCoords.length > 0) {
            com.jogamp.opengl.util.texture.Texture joglTexture = getTexture(texture);
            program.setTexture(joglTexture);
            program.setTexCoords(texCoords);
        }

        // Set IndexArray
        if (aVertexArray.isIndexArraySet()) {
            int[] indexArray = aVertexArray.getIndexArray();
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
        ImageUtil.flipImageVertically(awtImage);

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
        boolean hasColors = aVertexArray.isColorsArraySet();
        boolean hasTexCoords = aVertexArray.getTexCoordsArray().length > 0;

        if (hasTexCoords)
            return "Points_Color_Tex";

        return hasColors ? "Points_Colors" : "Points_Color";
    }
}
