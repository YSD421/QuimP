/**
 * 
 */
package uk.warwick.dic.lid;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import uk.warwick.tools.images.ExtraImageProcessor;

import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;
/**
 * Implementation of Kam algorithm with use of matrix approach
 * TODO description of algorithm and internal dependencies
 * @author baniuk
 * @date 10 Dec 2015
 *
 */
public class DICReconstruction {
	
	private static final Logger logger = LogManager.getLogger(DICReconstruction.class.getName());
	private final int shift = 1; // shift added to original image to eliminate 0 values
	
	private ImageProcessor srcIp;
	private double decay;
	private double angle;
	private double[] decays; // reference to preallocated decay data
	private int maxWidth;
	private int[][] ranges; // [r][0] - x of first pixel of line r of image, [r][1] - x of last pixel of image of line r 
	private ExtraImageProcessor srcImageCopyProcessor;
	private boolean isRotated; // true if srcImageCopyProcessor has been rotated already
	private ImageStatistics is;
	
	/**
	 * Default constructor that accepts ImagePlus
	 * It does not support stacks.
	 * @throws DicException Throws exception after generateRanges()
	 */
	public DICReconstruction(ImagePlus srcImage, double decay, double angle) throws DicException {
		this(srcImage.getProcessor(), decay, angle);
	}
	
	/**
	 * Default constructor that accepts ImageProcessor
	 * @throws DicException Throws exception after generateRanges()
	 */
	public DICReconstruction(ImageProcessor ip, double decay, double angle) throws DicException {
		this.angle = angle;
		this.decay = decay;
		this.isRotated = false;
		setIp(ip);
		recalculate(); 
	}

	/**
	 * Sets new reconstruction parameters for current object
	 * @param decay
	 * @param angle
	 * @throws DicException Rethrow exception after generateRanges()
	 */
	public void setParams(double decay, double angle) throws DicException {
		this.angle = angle;
		this.decay = decay;
		recalculate();
	}
	
	public void setIp(ImageProcessor ip) {
		this.srcIp = ip;
		// make copy of original image to not modify it - converting to 16bit
		this.srcImageCopyProcessor = new ExtraImageProcessor(srcIp.convertToShort(false));
		srcImageCopyProcessor.getIP().resetMinAndMax();	// ensure that minmax will be recalculated (usually they are stored in class field)
		logger.debug("Type of image " + srcImageCopyProcessor.getIP().getBitDepth() + " bit");
		// set interpolation
		srcImageCopyProcessor.getIP().setInterpolationMethod(ImageProcessor.BICUBIC);
		// Rotating image - set 0 background
		srcImageCopyProcessor.getIP().setBackgroundValue(0.0);
		// getting mean value
		is = srcImageCopyProcessor.getIP().getStatistics(); logger.debug("Mean value is " + is.mean);
		this.isRotated = false; // new Processor not rotated yet
	}
	
	/**
	 * Setup private fields.
	 * TODO should accept slice number?
	 * @throws DicException when input image is close to saturation e.g. has values of 65536-shift. This is due to applied algorithm 
	 * of detection image pixels after rotation.
	 * @return As modification of private class fields:
	 * \li \c maxWidth (private field)
	 * \li \c ranges (private field)
	 * \li \c maxWidth holds width of image after rotation,
	 * \li \c ranges table that holds first and last \a x position of image line (first and last pixel of image on background after rotation), \c srcImageCopyProcessor
	 * is rotated and shifted
	 */
	private void getRanges() throws DicException {
		double minpixel, maxpixel; // minimal pixel value
		int r; // loop indexes
		int firstpixel, lastpixel; // first and last pixel of image in line
			
		// check condition for removing 0 value from image
		minpixel = srcImageCopyProcessor.getIP().getMin();
		maxpixel = srcImageCopyProcessor.getIP().getMax();
		logger.debug("Pixel range is " + minpixel + " " + maxpixel);
		if(maxpixel > 65535-shift) {
			logger.error("Possible image clipping - check if image is saturated");
			throw new DicException(String.format("Possible image clipping - input image has at leas one pixel with value %d",65535-shift));
		}
		// scale pixels by adding 1 - we remove any 0 value from source image
		srcImageCopyProcessor.getIP().add(shift); 	
		srcImageCopyProcessor.getIP().resetMinAndMax(); logger.debug("Pixel range after shift is " + srcImageCopyProcessor.getIP().getMin() + " " + srcImageCopyProcessor.getIP().getMax());
		// rotate image with extending it. borders have the same value as background
		srcImageCopyProcessor.rotate(angle,true); // WARN May happen that after interpolation pixels gets 0 again ?
		isRotated = true; // current object was rotated
		int newWidth = srcImageCopyProcessor.getIP().getWidth();
		int newHeight = srcImageCopyProcessor.getIP().getHeight();
		ImageProcessor srcImageProcessorUnwrapped = srcImageCopyProcessor.getIP();
		maxWidth = newWidth;
		ranges = new int[newHeight][2];
		for(r=0; r<newHeight; r++) {
			// to not process whole line, detect where starts and ends pixels of image (reject background added during rotation)
			for(firstpixel=0; firstpixel<newWidth && srcImageProcessorUnwrapped.get(firstpixel,r)==0;firstpixel++);
			for(lastpixel=newWidth-1;lastpixel>=0 && srcImageProcessorUnwrapped.get(lastpixel,r)==0;lastpixel--);
			ranges[r][0] = firstpixel;
			ranges[r][1] = lastpixel;
		}
	}
	
	/**
	 * Recalculates tables on demand
	 * @throws DicException Rethrow exception after generateRanges()
	 */
	private void recalculate() throws DicException {
		// calculate preallocated decay data
		// generateRanges() must be called first as it initializes fields used by generateDecay()
		getRanges();
		generateDeacy(decay, maxWidth);
	}
	
   /**
	 * Reconstruct DIC image by LID method using LID method
	 * Make copy of original image to not change it.
	 * It is assumed that user counts angle in anti-clockwise direction and the shear angle 
	 * must be specified in this way as well.
	 * The algorithm process only correct pixels of rotated image to prevent artifacts on edges. This
	 * pixels are detected in less-computational way. First the image is converted to 16bit and the value of \c shift is added
	 * to each pixel. In this way original image does not contain 0. Then image is rotated with 0 padding. Thus any 0 on 
	 * rotated and prepared to reconstruction image does not belong to right pixels. 
	 * @remarks The reconstruction algorithm assumes that input image bas-reliefs are oriented horizontally, thus correct \c angle should be provided
	 * @warning Used optimisation with detecting of image pixels based on their value may not be accurate when input image 
	 * will contain saturated pixels
	 * @retval ImageProcessor
	 * @return Return reconstruction of \c srcImage as 8-bit image
	 */
	public ImageProcessor reconstructionDicLid() {
		logger.debug("Input image: "+ String.valueOf(srcIp.getWidth()) + " " + 
				String.valueOf(srcIp.getHeight()) + " " + 
				String.valueOf(srcIp.getBitDepth()));
		
		double cumsumup, cumsumdown;
		int c,u,d,r; // loop indexes
		int linindex = 0; // output table linear index
		if(!isRotated)	{ // rotate if not rotated in getRanges - e.g. we have new Processor added by setIp	
			srcImageCopyProcessor.getIP().add(shift); // we use different IP so shift must be added
			srcImageCopyProcessor.rotate(angle,true);
		}
		// dereferencing for optimization purposes
		int newWidth = srcImageCopyProcessor.getIP().getWidth();
		int newHeight = srcImageCopyProcessor.getIP().getHeight();
		ImageProcessor srcImageProcessorUnwrapped = srcImageCopyProcessor.getIP();
		// create array for storing results - 32bit float as imageprocessor		
		ExtraImageProcessor outputArrayProcessor = new ExtraImageProcessor(new FloatProcessor(newWidth, newHeight));
		float[] outputPixelArray = (float[]) outputArrayProcessor.getIP().getPixels();
		
		// do for every row - bas-relief is oriented horizontally 
		for(r=0; r<newHeight; r++) {
			// ranges[r][0] - first image pixel in line r
			// ranges[r][1] - last image pixel in line r
			linindex = linindex + ranges[r][0];
			// for every point apply KAM formula
			for(c=ranges[r][0]; c<=ranges[r][1]; c++) {
				// up
				cumsumup = 0;
				for(u=c; u>=ranges[r][0]; u--) {
					cumsumup += (srcImageProcessorUnwrapped.get(u, r)-shift-is.mean)*decays[Math.abs(u-c)];
				}
				// down
				cumsumdown = 0; // cumulative sum from point r to the end of column
				for(d=c; d<=ranges[r][1]; d++) {
					cumsumdown += (srcImageProcessorUnwrapped.get(d,r)-shift-is.mean)*decays[Math.abs(d-c)];
				}
				// integral
				outputPixelArray[linindex] = (float)(cumsumup - cumsumdown); // linear indexing is in row-order
				linindex++;
			}
			linindex = linindex + newWidth-ranges[r][1]-1;
		}
		// rotate back output processor
		outputArrayProcessor.getIP().setBackgroundValue(0.0);
		outputArrayProcessor.getIP().rotate(-angle);
		// crop it back to original size
		outputArrayProcessor.cropImageAfterRotation(srcIp.getWidth(), srcIp.getHeight());

		return outputArrayProcessor.getIP().convertToByte(true); // return reconstruction
	}
	
	/**
	 * Generates decay table with exponential distances between pixels multiplied by decay coefficient
	 * @param decay The value of decay coefficient
	 * @param length Length of table, usually equals to longest processed line on image
	 * @return Table with decays coefficients (private field)
	 */
	private void generateDeacy(double decay, int length) {
		decays = new double[length];
		
		for(int i=0;i<length;i++)
			decays[i] = Math.exp(-decay*i);
	}
}
