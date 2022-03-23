package snapgl;

public class SnapChartsGL {

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        //new TestRenderJOGL();

        JGLRenderer.registerFactory();

        snapcharts.app.App.main(args);
    }
}
