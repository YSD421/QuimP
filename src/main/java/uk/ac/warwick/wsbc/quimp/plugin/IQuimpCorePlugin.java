package uk.ac.warwick.wsbc.quimp.plugin;

// TODO: Auto-generated Javadoc
/**
 * General definition of plugin interface for QuimP
 * 
 * <p>Contain also flags understood by plugins
 * 
 * @author p.baniukiewicz
 * @see uk.ac.warwick.wsbc.quimp.PluginFactory
 */
public interface IQuimpCorePlugin {

  // any change here should be reflected in
  // uk.ac.warwick.wsbc.quimp.PluginFactory.getPluginType(File, String)
  /**
   * Type of plugin not defined.
   */
  int GENERAL = 0;
  /**
   * Plugin process snakes only.
   */
  int DOES_SNAKES = 1;
  /**
   * Plugin change size of input data.
   */
  int CHANGE_SIZE = 32;
  /**
   * Plugin modify input data in place.
   */
  int MODIFY_INPUT = 64;

  /**
   * Provide basic information to QuimP about plugin.
   * 
   * <p>It must return at least type of plugin
   * 
   * @return Combination of flags specifying: -# type of plugin (obligatory) -# modification of
   *         input size (e.g. reduction of polygon points)
   */
  int setup();

  /**
   * Pass to plugin its configuration data as pairs (key,value).
   * 
   * <p>This method is used for restoring configuration from config files maintained by caller.
   * Caller do not modify these values and only stores them as the were returned by
   * getPluginConfig() method.
   * 
   * <p>Numerical values should be passed as Double. setPluginConfig(ParamList) and
   * getPluginConfig() should use the same convention of key naming
   * and parameters casting
   * 
   * @param par plugin configuration.
   * @throws QuimpPluginException on problems with understanding parameters by plugin e.g. \b key
   *         is not understood or converting from \c String \b value to other type has not been
   *         successful.
   */
  void setPluginConfig(final ParamList par) throws QuimpPluginException;

  /**
   * Retrieve plugin configuration data as pairs (key,value>=).
   * 
   * <p>This configuration is not used by QuimP but it may be stored in QuimP configuration.
   * 
   * @return Plugin configuration
   * @see #setPluginConfig(ParamList)
   */
  ParamList getPluginConfig();

  /**
   * Show or hide plugin UI.
   * 
   * <p>UI is not obligatory. This function must be implemented but may do nothing.
   * 
   * @param val boolean
   * @return integer value that in principle can be e.g. information about cancelling window.
   *         Exemplary return can be: {@code return toggleWindow() ? 1 : 0;}, where toggle window
   *         returns boolean value.
   */
  int showUi(boolean val);

  /**
   * Get version of plugin.
   * 
   * <p>Versioning may be used for detecting incompatibilities between configurations. Plugin
   * version
   * is saved in QuimP config files, and then passed to plugin by setPluginConfig(ParamList) as \a
   * version key. This key is not available if plugin has not provided its version. The plugin is
   * responsible for parsing this parameter.
   * 
   * @return String with version (any format) or \c null if not supported
   */
  String getVersion();

  /**
   * Get short info about plugin.
   * 
   * <p>One can use white characters in this string to limit line length because there is no
   * guarantee that displayer will wrap lines.
   * 
   * <p>It can shows also simple help for supported macro options.
   * 
   * @return String about (any format) or null if not supported
   */
  String about();
}
