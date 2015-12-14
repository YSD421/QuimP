/**
 * 
 */
package uk.warwick.dic.lid;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * @author baniuk
 *
 */
public class DICReconstructionTest {
	
	private ImagePlus image;
	private static final Logger logger = LogManager.getLogger(DICReconstructionTest.class.getName());
	
	/**
	 * Load test image
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		image = IJ.openImage("src/test/java/uk/warwick/dic/lid/testObject.tif"); // opens test image
	}

	/**
	 * @throws java.lang.Exception
	 * @warning May not detect changes done on image (e.g. rotation)
	 */
	@After
	public void tearDown() throws Exception {
		if(image.changes) { // check if source was modified
			image.changes = false; // set flag to false to prevent save dialog
			image.close(); // close image
			throw new Exception("Image has been modified"); // throw exception if source image was modified
		}
		image.close();
	}

 	/**
 	 * @test Test method for {@link uk.warwick.dic.lid.DICReconstruction#reconstructionDicLid(ImagePlus, double, double)}.
	 * Saves output image at \c /tmp/testDicReconstructionLidMatrix.tif
	 * @pre
	 * Input image is square
	 * @post
	 * Output image should be properly reconstructed and have correct size of input image
	 */
	@Test
	public void testreconstructionDicLid() {
		ImageProcessor ret;
		DICReconstruction dcr;
		try {
			dcr = new DICReconstruction(image, 0.04, 135f);
			// replace outputImage processor with result array with scaling conversion
			ret = dcr.reconstructionDicLid();
			ImagePlus outputImage = new ImagePlus("", ret);
			
			assertEquals(513,outputImage.getWidth()); // size of the image
			assertEquals(513,outputImage.getHeight());
			IJ.saveAsTiff(outputImage, "/tmp/testDicReconstructionLidMatrix.tif"); 
			logger.info("Check /tmp/testDicReconstructionLidMatrix.tif to see results");
		} catch (DicException e) {
			logger.error(e);
		}

	}
	
	@Test(expected=DicException.class)
	public void testreconstructionDicLid_saturated() throws DicException {
		ImageProcessor ret;
		DICReconstruction dcr;
		ImageConverter.setDoScaling(true);
		ImageConverter image16 = new ImageConverter(image);
		image16.convertToGray16();
		
		image.getProcessor().putPixel(100, 100, 65535);
		
		try {
			dcr = new DICReconstruction(image, 0.04, 135f);
			ret = dcr.reconstructionDicLid();
			ImagePlus outputImage = new ImagePlus("", ret);
			assertEquals(513,outputImage.getWidth()); // size of the image
			assertEquals(513,outputImage.getHeight());
			IJ.saveAsTiff(outputImage, "/tmp/testDicReconstructionLidMatrix_sat.tif"); 
			logger.info("Check /tmp/testDicReconstructionLidMatrix_sat.tif to see results");
		} catch (DicException e) {
			logger.error(e);
			throw e;
		}

	}

}
