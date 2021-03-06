package com.github.celldynamics.quimp;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for PropertyReader.
 * 
 * @author baniu
 *
 */
public class PropertyReaderTest {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(PropertyReaderTest.class.getName());

  /**
   * read property and display it.
   * 
   * <p>Post: value of key displayed
   *
   * @throws Exception the exception
   */
  @Test
  public void testReadProperty() throws Exception {
    LOGGER.debug(new PropertyReader().readProperty("quimpconfig.properties", "manualURL"));
  }

}
