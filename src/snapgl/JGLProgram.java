package snapgl;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import snap.gfx.Color;
import snap.gfx3d.VertexArray;
import snap.util.ArrayUtils;
import java.nio.FloatBuffer;

/**
 * This class represents an OpenGL shader program.
 */
public class JGLProgram {

    // The Renderer
    private JGLRendererX _rjx;

    // The id
    private int  _id;

    // The Vertex Shader
    private JGLShader _vertexShader;

    // The Fragment Shader
    private JGLShader _fragmentShader;

    // The last pointsArray
    private float[]  _pointsArray;

    // The last colorsArray
    private float[]  _colorsArray;

    // The PointAttr
    private int  _pointAttr;

    // The ColorAttr
    private int  _colorAttr;

    /**
     * Creates a ShaderProgram for VertexArray.
     */
    public JGLProgram(VertexArray aVA, JGLRendererX aRJX)
    {
        _rjx = aRJX;

        // Create Program
        GL2 gl2 = aRJX.getGL2();
        _id = gl2.glCreateProgram();

        // Load shaders
        loadShaders(aVA);

        // Link program
        String linkError = linkProgram();
        if (linkError != null)
            System.err.println("ShaderProgram.init: ERROR linking program: " + linkError);
    }

    /**
     * Loads the shaders.
     */
    private void loadShaders(VertexArray aVA)
    {
        // Create VertexShader
        GL2 gl2 = _rjx.getGL2();
        _vertexShader = _rjx.getVertexShader(aVA);
        int vertexShaderId = _vertexShader.getId();
        gl2.glAttachShader(_id, vertexShaderId);

        // Create FragmentShader
        _fragmentShader = _rjx.getFragmentShader(aVA);
        int fragmentShaderId = _fragmentShader.getId();
        gl2.glAttachShader(_id, fragmentShaderId);
    }

    /**
     * Links the program.
     */
    private String linkProgram()
    {
        // Link program
        GL2 gl2 = _rjx.getGL2();
        gl2.glLinkProgram(_id);

        // Get link status
        int[] linkStatus = { 0 };
        gl2.glGetProgramiv(_id, GL2.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == GL2.GL_TRUE)
            return null;

        // Get compile error string
        int[] logLength = new int[1];
        gl2.glGetProgramiv(_id, GL2.GL_INFO_LOG_LENGTH, logLength, 0);
        byte[] log = new byte[logLength[0]];
        gl2.glGetProgramInfoLog(_id, logLength[0], null, 0, log, 0);
        return new String(log);
    }

    /**
     * Returns the id.
     */
    public int getId()  { return _id; }

    /**
     * Returns the Vertex Shader.
     */
    public JGLShader getVertexShader()  { return _vertexShader; }

    /**
     * Returns the Fragment Shader.
     */
    public JGLShader getFragmentShader()  { return _fragmentShader; }

    /**
     * Starts the program.
     */
    public void useProgram()
    {
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();
        gl2.glUseProgram(programId);
    }

    /**
     * Sets the projection matrix.
     */
    public void setProjectionMatrix(double[] aMatrix)
    {
        // Get program info
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();

        // Get matrix as 4fv
        float[] matrix4fv = ArrayUtils.floatArray(aMatrix);

        // Set ProjMatrix
        int projMatrixUniform = gl2.glGetUniformLocation(programId, "projMatrix");
        gl2.glUniformMatrix4fv(projMatrixUniform, 1, false, matrix4fv, 0);
    }

    /**
     * Sets the View matrix.
     */
    public void setViewMatrix(double[] aMatrix)
    {
        // Get program info
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();

        // Get matrix as 4fv
        float[] matrix4fv = ArrayUtils.floatArray(aMatrix);

        // Get ViewMatrix uniform and set
        int viewMatrixUniform = gl2.glGetUniformLocation(programId, "viewMatrix");
        gl2.glUniformMatrix4fv(viewMatrixUniform, 1, false, matrix4fv, 0);
    }

    /**
     * Sets the points array.
     */
    public void setPoints(float[] pointsArray)
    {
        // Get program info
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();

        // Set PointsArray
        _pointsArray = pointsArray;

        // Get Points attribute and enable
        _pointAttr = gl2.glGetAttribLocation(programId, "vertPoint");
        gl2.glEnableVertexAttribArray(_pointAttr);

        // Get matrix as 4fv
        FloatBuffer pointBuffer = Buffers.newDirectFloatBuffer(pointsArray.length);
        pointBuffer.put(pointsArray);
        gl2.glVertexAttribPointer(_pointAttr, 3, GL2.GL_FLOAT, false, 0, pointBuffer.rewind());
    }

    /**
     * Sets the color.
     */
    public void setColor(Color aColor)
    {
        // Get program info
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();

        // Get color as 3fv
        Color color = aColor;
        if (color == null)  { color = Color.RED; System.err.println("ShaderProgram.setColor: Null color"); }
        float[] color3fv = { (float) color.getRed(), (float) color.getGreen(), (float) color.getBlue() };

        // Set VertColor
        int vertColorUniform = gl2.glGetUniformLocation(programId, "vertColor");
        gl2.glUniform3fv(vertColorUniform, 1, color3fv, 0);
    }

    /**
     * Sets the color array.
     */
    public void setColors(float[] colorsArray)
    {
        // Get program info
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();

        // Set ColorsArray
        _colorsArray = colorsArray;

        // Get vertColor attribute and enable
        _colorAttr = gl2.glGetAttribLocation(programId, "vertColor");
        gl2.glEnableVertexAttribArray(_colorAttr);

        // Get matrix as 4fv
        FloatBuffer colorsBuffer = Buffers.newDirectFloatBuffer(colorsArray.length);
        colorsBuffer.put(colorsArray);
        gl2.glVertexAttribPointer(_colorAttr, 3, GL2.GL_FLOAT, false, 0, colorsBuffer.rewind());
    }

    /**
     * Runs the program.
     */
    public void runProgram()
    {
        // Get program info
        GL2 gl2 = _rjx.getGL2();

        // Get PointCount and run
        int triangleCount = _pointsArray.length / 3;
        gl2.glDrawArrays(GL2.GL_TRIANGLES, 0, triangleCount);

        // Disable attributes
        gl2.glDisableVertexAttribArray(_pointAttr);
        if (_colorsArray != null)
            gl2.glDisableVertexAttribArray(_colorAttr);

        // Remove shader code from current rendering state
        gl2.glUseProgram(0);

        // Clear vars
        _pointsArray = _colorsArray = null;
        _pointAttr = _colorAttr = -1;
    }

    /**
     * Cleanup.
     */
    public void disposeShaderProgram()
    {
        GL2 gl2 = _rjx.getGL2();
        int programId = _id;
        gl2.glDetachShader(programId, _vertexShader.getId());
        gl2.glDetachShader(programId, _fragmentShader.getId());
        gl2.glDeleteProgram(programId);
    }
}
