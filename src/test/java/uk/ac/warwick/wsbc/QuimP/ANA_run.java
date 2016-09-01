package uk.ac.warwick.wsbc.QuimP;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

/**
 * Plugin runner for in-place tests.
 * 
 * @author p.baniukiewicz
 */
public class ANA_run {

    /**
     * 
     */
    public ANA_run() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        ImageJ ij = new ImageJ();
        ImagePlus im = IJ.openImage("src/test/resources/fluoreszenz-test_eq_smooth.tif");
        im.show();
        ANA_ ana = new ANA_();
        ana.setup(new String(), im);
        // load paQP and QCONF file related to tiff pointed above
        ana.run(im.getProcessor());

    }

}
