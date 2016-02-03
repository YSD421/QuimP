package uk.ac.warwick.wsbc.tools.images.filters;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.ac.warwick.wsbc.plugin.utils.IPadArray;

public class IPadArray_test implements IPadArray {

	private int window = 5; // size of virtual window
//	private static final Logger logger = LogManager.getLogger(IPadArray_test.class.getName());

	/**
	 * @test test of getIndex method for symmetric padding
	 * @pre vector of 0:9 numbers (virtual indexes) and window of size 5
	 * @post 
	 */
	@Test
	public void test_getIndex_CIRCULAR() {
		int dataSize = 10;
		int expected[] = {5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4};
		for(int l=0; l<expected.length; l++) {
			assertTrue(expected[l]==IPadArray.getIndex(dataSize, l-window, IPadArray.CIRCULARPAD));
		}
	}
	
	@Test
	public void test_getIndex_SYMMETRIC() {
		int dataSize = 10;
		int tmp;
		int expected[] = {4,3,2,1,0,0,1,2,3,4,5,6,7,8,9,9,8,7,6,5};
		for(int l=0; l<expected.length; l++) {
			tmp = IPadArray.getIndex(dataSize, l-window, IPadArray.SYMMETRICPAD);
			assertTrue(expected[l]==tmp);
		}
	}

}