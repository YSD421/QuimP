/**
 * 
 */
package uk.ac.warwick.wsbc.QuimP.plugin.dic;

import uk.ac.warwick.wsbc.QuimP.QuimpException;

/**
 * Basic class derived from Exception for purposes of DICReconstruction module
 * 
 * @author p.baniukiewicz
 */
@SuppressWarnings("serial")
public class DicException extends QuimpException {

    /**
     * Main constructor
     * 
     * @param arg0 Reason of exception
     */
    public DicException(final String arg0) {
        super(arg0);
    }

}
