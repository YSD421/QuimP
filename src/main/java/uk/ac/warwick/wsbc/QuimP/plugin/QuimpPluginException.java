package uk.ac.warwick.wsbc.QuimP.plugin;

/**
 * Basic class derived from Exception for purposes of QuimP plugins
 * 
 * @author p.baniukiewicz
 * @date 20 Jan 2016
 * 
 * @todo //TODO in future it will be top class derived from QuimpException
 */
@SuppressWarnings("serial")
public class QuimpPluginException extends Exception {

    public QuimpPluginException() {
        super();
    }

    public QuimpPluginException(String message, Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public QuimpPluginException(Throwable cause) {
        super(cause);
    }

    /**
     * Main constructor
     * 
     * @param arg0
     * Reason of exception
     */
    public QuimpPluginException(String arg0) {
        super(arg0);
    }

    public QuimpPluginException(String arg0, Throwable cause) {
        super(arg0, cause);
    }
}