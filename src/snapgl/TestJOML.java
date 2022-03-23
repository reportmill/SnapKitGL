package snapgl;

import org.joml.Matrix4d;
import snap.gfx3d.Matrix3D;

public class TestJOML {

    public static void main(String[] args)
    {
        Matrix4d mtx = new Matrix4d();
        mtx.translate(50, 50, 50);
        //mtx.rotateX(Math.toRadians(45));
        mtx.rotateXYZ(Math.toRadians(30), Math.toRadians(45), Math.toRadians(60));

        Matrix3D xfm = new Matrix3D();
        //xfm.rotateX(45);
        //xfm.translate(50, 50, 50);
        xfm.rotateXYZ(30, 45, 60);
        xfm.translate(50, 50, 50);

        double[] mtxD = mtx.get(new double[16]);
        double[] xfmD = xfm.toArray(new double[16]);

        System.out.println("Hi");

    }
}
