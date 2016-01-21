package uk.ac.warwick.wsbc.tools.images;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

import javax.vecmath.Vector2d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple data loader for test
 * 
 * Load contours saved as one-column list with interleaving coordinates of 
 * vertices:
 * @code
 * X1
 * Y1
 * X2
 * Y2
 * ...
 * Xn
 * Yn
 * @endcode
 * The file must contain even number of data. Exemplary code in Matlab to create such file:
 * @code{.m}
 * addpath('/home/baniuk/Documents/QuimP11_MATLAB/')
 * qCells = readQanalysis('Resources/after-macro');
 * testFrames = [75 125 137 1];
 * clear coords;
 * for i=1:length(testFrames)
 *   coords{i} = qCells.outlines{testFrames(i)}(:,2:3);
 * end
 * for i=1:length(testFrames)
 *   fid = fopen(['testData_' num2str(testFrames(i)) '.dat'], 'w');
 *   xy = coords{i};
 *   xyr = reshape(xy',[],1); % x first
 *   fprintf(fid,'%.4f\n',xyr);
 *   fclose(fid);
 * end
 * @endcode
 * 
 * @author baniuk
 *
 */
class DataLoader {
	private static final Logger logger = LogManager.getLogger(DataLoader.class.getName());
	private List<Double> data; 
	public List<Vector2d> Vert;
	
	/**
	 * Construct dataLoader object.
	 * 
	 * Open and read datafile
	 * 
	 * @param fileName file with data (with path)
	 * @throws FileNotFoundException on bad file
	 * @throws IllegalArgumentException when the number of lines in \c fileName is not power of 2 
	 */
	DataLoader(String fileName) throws FileNotFoundException,IllegalArgumentException {
		data = new ArrayList<Double>();
		Vert = new ArrayList<Vector2d>();
		Scanner scanner = new Scanner(new File(fileName));
		while(scanner.hasNextDouble())
			data.add(scanner.nextDouble());
		scanner.close();
		convertToVector();
		logger.debug("File: "+fileName+" loaded");
	}
	
	/**
	 * Convert read List<Double> to List<Vector2d>
	 * 
	 * @throws IllegalArgumentException
	 */
	private void convertToVector() throws IllegalArgumentException{
		if(data.size()%2!=0)
			throw new IllegalArgumentException("Data must be multiply of 2");
		ListIterator<Double> it = data.listIterator();
		while(it.hasNext()) {
			Vert.add(new Vector2d(
					it.next().doubleValue(), // x coord
					it.next().doubleValue())); //y coord from input file
		}
	}
	
	/**
	 * Return loaded data
	 * 
	 * @return loaded polygon as List<Vector2d> 
	 * @retval List<Vector2d> 
	 */
	public List<Vector2d> getData() {
		return Vert;
	}
}