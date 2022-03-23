package snapgl;
import com.jogamp.opengl.GL2;
import snap.util.SnapUtils;

/**
 * This class represents an OpenGL shader.
 */
public class JGLShader {

    // The renderer
    private JGLRendererX _rjx;

    // The Shader type
    private ShaderType  _type;

    // The name
    private String  _name;

    // The id
    private int  _id;

    // Constant for shader type
    public enum ShaderType { Vertex, Fragment }

    /**
     * Creates a shader of given type for given Renderer.
     */
    public JGLShader(ShaderType aType, String aName, JGLRendererX aRJX)
    {
        _rjx = aRJX;
        _type = aType;
        _name = aName;

        // Create shader
        GL2 gl2 = aRJX.getGL2();
        int glType = _type == ShaderType.Vertex ? GL2.GL_VERTEX_SHADER : GL2.GL_FRAGMENT_SHADER;
        _id = gl2.glCreateShader(glType);

        // Load source text and set
        String sourceText = getSourceText();
        gl2.glShaderSource(_id, 1, new String[] { sourceText } , null);

        // Compile shader
        String compileError = compileShader();
        if (compileError != null)
            System.err.println("Shader.init: ERROR compiling shader: " + _name + ": " + compileError);
    }

    /**
     * Returns the id.
     */
    public int getId()  { return _id; }

    /**
     * Returns the full text string of shader file.
     */
    public String getSourceText()
    {
        String sourcePath = "shaders/" + getSourceName();
        return SnapUtils.getText(getClass(), sourcePath);
    }

    /**
     * Returns the shader file name.
     */
    public String getSourceName()
    {
        // Handle Vertex Shaders:
        if (_type == ShaderType.Vertex && _name.equals("Points_Color"))
            return "Points_Color.vs";
        if (_type == ShaderType.Vertex && _name.equals("Points_Colors"))
            return "Points_Colors.vs";

        // Handle Fragment Shaders
        if (_type == ShaderType.Fragment)
            return "General.fs";

        // Something went wrong
        return null;
    }

    /**
     * Compiles the shader and returns compile error (if error).
     */
    private String compileShader()
    {
        // Compile
        GL2 gl2 = _rjx.getGL2();
        gl2.glCompileShader(_id);

        // Get compile status
        int[] compileStatus = { 0 };
        gl2.glGetShaderiv(_id, GL2.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == GL2.GL_TRUE)
            return null;

        // Get compile error string
        int[] logLength = new int[1];
        gl2.glGetShaderiv(_id, GL2.GL_INFO_LOG_LENGTH, logLength, 0);
        byte[] log = new byte[logLength[0]];
        gl2.glGetShaderInfoLog(_id, logLength[0], null, 0, log, 0);
        return new String(log);
    }
}
