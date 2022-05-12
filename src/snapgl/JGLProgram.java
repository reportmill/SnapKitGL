package snapgl;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import snap.gfx.Color;
import snap.gfx3d.VertexArray;
import snap.util.ArrayUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * This class represents an OpenGL shader program.
 */
public class JGLProgram {

    // The Renderer
    private JGLRenderer  _rjx;

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

    // The last texCoordsArray
    private float[]  _texCoordsArray;

    // The last indexArray
    private int[]  _indexArray;
    private IntBuffer  _indexBuffer;

    // The PointAttr
    private int  _pointAttr;

    // The ColorAttr
    private int  _colorAttr;

    // The TexCoordAttr
    private int  _texCoordAttr;

    /**
     * Creates a ShaderProgram for VertexArray.
     */
    public JGLProgram(VertexArray aVA, JGLRenderer aRJX)
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
     * Sets the texture coords array.
     */
    public void setTexCoords(float[] texCoordsArray)
    {
        // Get program info
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();

        // Set TexCoordsArray
        _texCoordsArray = texCoordsArray;

        // Get vertTexCoord attribute and enable
        _texCoordAttr = gl2.glGetAttribLocation(programId, "vertTexCoord");
        gl2.glEnableVertexAttribArray(_texCoordAttr);

        // Get matrix as 4fv
        FloatBuffer texCoordsBuffer = Buffers.newDirectFloatBuffer(texCoordsArray.length);
        texCoordsBuffer.put(texCoordsArray);
        gl2.glVertexAttribPointer(_texCoordAttr, 2, GL2.GL_FLOAT, false, 0, texCoordsBuffer.rewind());
    }

    /**
     * Sets the index array.
     */
    public void setIndexArray(int[] indexArray)
    {
        // Set IndexArray
        _indexArray = indexArray;

        // Create IndexBuffer and load/rewind
        _indexBuffer = Buffers.newDirectIntBuffer(indexArray.length);
        _indexBuffer.put(indexArray);
        _indexBuffer.rewind();
    }

    /**
     * Sets the texture coords array.
     */
    public void setTexture(Texture aTexture)
    {
        // Get program info
        int programId = getId();
        GL2 gl2 = _rjx.getGL2();

        // Get fragTexture attribute
        int textureUnLoc = gl2.glGetUniformLocation(programId, "fragTexture");

        // Enable/bind
        gl2.glActiveTexture(GL2.GL_TEXTURE0);
        aTexture.enable(gl2);
        aTexture.bind(gl2);
        gl2.glUniform1i(textureUnLoc, 0);
    }

    /**
     * Runs the program.
     */
    public void runProgram()
    {
        // Get program info
        GL2 gl2 = _rjx.getGL2();

        // If IndexArray provided, drawElements with IndexBuffer
        if (_indexArray != null)
            gl2.glDrawElements(GL2.GL_TRIANGLES, _indexArray.length, GL2.GL_UNSIGNED_INT, _indexBuffer);

        // Otherwise, get PointCount and run
        else {
            int triangleCount = _pointsArray.length / 3;
            gl2.glDrawArrays(GL2.GL_TRIANGLES, 0, triangleCount);
        }

        // Disable attributes
        gl2.glDisableVertexAttribArray(_pointAttr);
        if (_colorsArray != null)
            gl2.glDisableVertexAttribArray(_colorAttr);
        if (_texCoordsArray != null)
            gl2.glDisableVertexAttribArray(_texCoordAttr);

        // Remove shader code from current rendering state
        gl2.glUseProgram(0);

        // Clear vars
        _pointsArray = _colorsArray = _texCoordsArray = null;
        _indexArray = null;
        _pointAttr = _colorAttr = _texCoordAttr = -1;
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
