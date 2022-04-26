/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapgl;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;
import java.nio.DoubleBuffer;

/**
 * Handy JOGL utility methods.
 */
public class JoglUtils {

    /**
     * Render axis.
     */
    public static void render3DAxisLines(GL2 gl, GLUT glut, double aRadius, double aHeight)
    {
        // Get info
        int slices = 32;
        int stacks = 32;

        // Make sure it draws in front
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

        // Save transform
        gl.glPushMatrix();

        // Render X axis
        gl.glColor3d(1d, 0d, 0d);
        gl.glRotated(-90, 1, 0, 0);
        glut.glutSolidCylinder(aRadius, aHeight, slices, stacks);
        gl.glRotated(90, 1, 0, 0);

        // Render Y axis
        gl.glColor3d(0d, 1d, 0);
        gl.glRotated(90, 0, 1, 0);
        glut.glutSolidCylinder(aRadius, aHeight, slices, stacks);
        gl.glRotated(-90, 0, 1, 0);

        // Render Z axis
        gl.glColor3d(0d, 0d, 1d);
        glut.glutSolidCylinder(aRadius, aHeight, slices, stacks);

        // Restore transform
        gl.glPopMatrix();
    }

    /**
     * Renders a line as a cylinder between points.
     */
    public static void renderLineAsCylinder(GL2 gl2, GLU glu,
            double x0, double y0, double z0, double x1, double y1, double z1, double aRadius)
    {
        double dir_x = x1-x0;
        double dir_y = y1-y0;
        double dir_z = z1-z0;
        double bone_length = Math.sqrt (dir_x * dir_x + dir_y * dir_y + dir_z * dir_z);
        GLUquadric quad_obj = glu.gluNewQuadric();

        glu.gluQuadricDrawStyle (quad_obj, GLU.GLU_FILL);
        glu.gluQuadricNormals (quad_obj, GLU.GLU_SMOOTH);
        gl2.glPushMatrix();
        //Pan to the starting point
        gl2.glTranslated (x0, y0, z0);
        //Calculate the length
        double length = Math.sqrt (dir_x * dir_x + dir_y * dir_y + dir_z * dir_z);
        if (length <0.0001) {
            dir_x = 0.0; dir_y = 0.0; dir_z = 1.0; length = 1.0;
        }
        dir_x/= length; dir_y/= length; dir_z/= length;
        double up_x, up_y, up_z;
        up_x = 0.0;
        up_y = 1.0;
        up_z = 0.0;
        double side_x, side_y, side_z;
        side_x = up_y * dir_z-up_z * dir_y;
        side_y = up_z * dir_x-up_x * dir_z;
        side_z = up_x * dir_y-up_y * dir_x;
        length = Math.sqrt (side_x * side_x + side_y * side_y + side_z * side_z);
        if (length <0.0001) {
            side_x = 1.0; side_y = 0.0; side_z = 0.0; length = 1.0;
        }
        side_x/= length; side_y/= length; side_z/= length;
        up_x = dir_y * side_z-dir_z * side_y;
        up_y = dir_z * side_x-dir_x * side_z;
        up_z = dir_x * side_y-dir_y * side_x;
        //Calculate the transformation matrix
        double m[] = { side_x, side_y, side_z, 0.0,
            up_x, up_y, up_z, 0.0,
            dir_x, dir_y, dir_z, 0.0,
            0.0, 0.0, 0.0, 1.0};
        DoubleBuffer mdb = DoubleBuffer.wrap(m);
        gl2.glMultMatrixd (mdb);
        //Cylinder parameters
        int slices = 8;//number of segments
        int stack = 3;//Recursion times
        glu.gluCylinder(quad_obj, aRadius, aRadius, bone_length, slices, stack);
        gl2.glPopMatrix();
    }
}
