package com.github.celldynamics.quimp;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Filter that filters out packages that not belong to QuimP.
 * 
 * <p>Used for limiting log output when log is set globally for the whole IJ.
 * 
 * @author p.baniukiewicz
 *
 */
public class LogbackFilter extends Filter<ILoggingEvent> {

  @SuppressWarnings("unused")
  private String packageName;

  /**
   * Main constructor.
   */
  public LogbackFilter() {
    packageName = LogbackFilter.class.getPackage().getName().toLowerCase();
  }

  /*
   * (non-Javadoc)
   * 
   * @see ch.qos.logback.core.filter.Filter#decide(java.lang.Object)
   */
  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (event.getLoggerName().toLowerCase().contains("quimp")) { // to catch plugins as well
      return FilterReply.NEUTRAL; // run next filters on QuimP
    } else {
      return FilterReply.DENY; // stop logging
    }
  }
}
