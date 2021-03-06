package com.github.celldynamics.quimp.plugin.engine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.QuimP;
import com.github.celldynamics.quimp.plugin.IQuimpCorePlugin;

/*
 * //!>
 * @startuml doc-files/PluginFactory_1_UML.png
 * actor user 
 * participant PluginFactory as PF
 * participant Plugin as PL 
 * == Create instance of PluginFactory == 
 * user -> PF : //<<create>>// 
 * activate PF 
 * PF -> PF : init ""availPlugins"" 
 * PF -> PF : scanDirectory() 
 * activate PF 
 * PF -> PF : discover qname getClassName
 * PF -> PF : getPluginType()
 * activate PF
 * PF -> PL : //<<getPluginInstance>>//
 * activate PL
 * PF -> PL : getPluginType()
 * PL --> PF : ""type""
 * PF -> PL : getPluginVersion()
 * PL --> PF : ""version""
 * destroy PL
 * PF -> PF : store at ""availPlugins""
 * deactivate PF
 * deactivate PF
 * == Get names ==
 * user -> PF : getPluginNames(type)
 * loop ""availPlugins""
 * PF -> PF : check ""type""
 * end
 * PF --> user : List
 * == Get Instance ==
 * user -> PF : getInstance(name)
 * PF -> PF : find plugin
 * PF -> PL : //<<getPluginInstance>>//
 * activate PL
 * PF --> user : ""instance""
 * note left
 * Only one instance of requested
 * plugin is created.
 * On next requests previous reference
 * is returned
 * endnote
 * @enduml
 * 
 * @startuml doc-files/PluginFactory_2_UML.png
 * partition PluginFactory(directory) {
 * (*) --> if "plugin directory\n exists" then
 * -->[true] init ""availPlugins""
 * --> "scanDirectory()"
 * -right-> (*)
 * else 
 * -->[false] "throw QuimpPluginException"
 * --> (*)
 * endif
 * }
 * @enduml
 * 
 * @startuml doc-files/PluginFactory_3_UML.png
 * partition scanDirectory() { 
 * (*) --> Get file \nfrom ""root""
 * if "file contains\n**-quimp.jar**" then
 * -->[true] Discover qualified name
 * --> getPluginType()
 * --> if Type valid\njar valid\nreadable then
 * -->[true] Store at ""availPlugins""
 * --> Get file \nfrom ""root""
 * else
 * -->[false] log error
 * --> Get file \nfrom ""root""
 * endif
 * else
 * -->[false] Get file \nfrom ""root""
 * endif
 * }
 * @enduml
 * 
 * @startuml doc-files/PluginFactory_4_UML.png
 * start 
 * :call ""setup()"" from jar;
 * if (valid plugin type?) then (true)
 * :Return plugin type;
 * stop
 * else (false)
 * :throw Exception;
 * endif
 * end
 * @enduml
 * 
 * @startuml doc-files/PluginFactory_5_UML.png
 * start
 * :Load jar;
 * :Create instance;
 * end
 * @enduml
 * 
 * @startuml doc-files/PluginFactory_6_UML.png
 * start
 * if (name is not empty) then (yes)
 *   :Build qualified name\nfrom ""getClassName()"";
 *   if (get plugin data from\n ""availPlugins"") then (null)
 *     :log error; 
 *     ->Return null;
 *     stop
 *   else
 *     if (was plugin used\nbefore) then (yes)
 *       :Restore instance;
 *       ->Return instance;
 *       stop
 *     else
 *        :""getPluginInstance"";
 *        :Store instance;
 *     endif   
 *   endif
 * else
 *   stop  
 * endif
 * stop
 * @enduml
 * //!<
 */
/**
 * Plugin jar loader.
 * 
 * <p>Created object is connected with directory where plugins exist. This directory is scanned for
 * jar
 * files that meet given below naming conventions. Every file that meets naming convention is loaded
 * and asked for method IQuimpPlugin.setup(). On success the plugin is registered in availPlugins
 * database:
 * 
 * {@code <Name, <File, Type, ClassName>>}
 * 
 * <p>Where Name is name of plugin extracted form file name (see below required naming conventions),
 * File is handle to file on disk, Type is type of plugin according to types defined in
 * warwick.wsbc.plugin.IQuimpPlugin and ClassName is qualified name of class of plugin. The
 * ClassName is extracted from the plugin jar file assuming that plugin class contains underscore _
 * in its name. If more classes underscored is found in jar, only the first discovered is loaded.
 * Thus the following conventions are required:
 * <ol>
 * <li>Plugin name must contain <b>-quimp</b> to be considered as plugin (see PATTERN field)
 * <li>Class name in plugin must end with underscore to be considered as plugin main class
 * </ol>
 *
 * <p>Simplified sequence diagrams are as follows: <br>
 * <img src="doc-files/PluginFactory_1_UML.png"/><br>
 * 
 * <p>This class try to hide all exceptions that can be thrown during loading plugins from user. In
 * general only when user pass wrong path to plugins directory exception is thrown. In all other
 * cases class returns null pointers or empty lists. Error handling:
 * <ol>
 * <li>Given directory exists but there is no plugins inside
 * <ol>
 * <li>getPluginNames(int) returns empty list (length 0)
 * </ol>
 * <li>Given directory exists but plugins are corrupted - they fulfil naming criterion but they are
 * not valid QuimP plugins
 * <ol>
 * <li>getInstance(final String) returns <tt>null</tt> when correct name is given. It means that
 * plugin has
 * been registered by scanDirectory() so it had correct name and supported
 * wsbc.plugin.IQuimpPlugin.setup() method
 * </ol>
 * <li>Given directory does not exist
 * <ol>
 * <li>Constructor throws QuimpPluginException
 * </ol>
 * <li>User asked for unknown name in getInstance(final String)
 * <ol>
 * <li>getInstance(final String) return null
 * </ol>
 * </ol>
 * Internally getPluginType(final File, final String) and getInstance(final String) throw exceptions
 * around class loading and running methods from them. Additionally getPluginType(final File, final
 * String) throws exception when unknown type is returned from valid plugin. These exceptions are
 * caught preventing adding that plugin into availPlugins database (scanDirectory()) or hidden in
 * getInstance that returns null in this case. All exceptions are masked besides scanDirectory()
 * that can throw checked PluginException that must be handled by caller. It usually means that
 * given plugin directory does not exist.
 * 
 * <p>Each jar is loaded only once on first request. Next requests get the same instance.
 * 
 * @author p.baniukiewicz
 */
public class PluginFactory {

  /**
   * The Constant LOGGER.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(PluginFactory.class.getName());
  /**
   * Name pattern of plugins.
   */
  private static final String PATTERN = "-quimp";

  /**
   * List of plugins found in initial directory path passed to constructor.
   * 
   * <p>Plugins are organized in list [name, [path, qname, type]] where:
   * <ol>
   * <li>name is the name of plugin extracted from plugin jar filename. Name is always encoded as
   * Name - starts with capital letter
   * <li>path is full path with jar filename
   * <li>qname is qualified name of plugin class obtained from jar name
   * <li>type is type of plugin read from IQuimpPlugin.setup() method
   * </ol>
   * 
   * <p>This field is set by scanDirectory() method -> getPluginType()
   */
  private HashMap<String, PluginProperties> availPlugins;
  private Path root;

  /**
   * Accessor to internal database of loaded plugins.
   * 
   * @return Non-modifiable database of loaded plugins
   */
  public Map<String, PluginProperties> getRegisterdPlugins() {

    return Collections.unmodifiableMap(availPlugins);
  }

  /**
   * Build object connected to plugin directory.
   * 
   * <p>Can throw exception if there is no directory path. <br>
   * <img src="doc-files/PluginFactory_2_UML.png"/><br>
   * 
   * @param path
   * 
   */
  public PluginFactory(final Path path) {
    if (QuimP.SUPER_DEBUG) {
      getSystemClassPath();
    }
    LOGGER.debug("Attached " + path.toString());
    availPlugins = new HashMap<String, PluginProperties>();
    // check if dir exists
    if (Files.notExists(path)) {
      LOGGER.warn("Plugin directory can not be read");
      root = Paths.get("/");
    } else {
      root = path;
      scanDirectory();
    }
  }

  /**
   * Scan path for files that match PATTERN name and end with .jar.
   * 
   * <p>Fill availPlugins field. Field name is filled as Name without dependency how original
   * filename was written. It is converted to small letters and then first char is upper-case
   * written. <br>
   * <img src="doc-files/PluginFactory_3_UML.png"/><br>
   * 
   * @return table of files that fulfill criterion:
   *         <ol>
   *         <li>have extension
   *         <li>extension is .jar or .JAR
   *         <li>contain PATTERN in name
   *         </ol>
   *         If there is no plugins in directory it returns 0 length array
   */
  private File[] scanDirectory() {
    File fi = new File(root.toString());
    File[] listFiles = fi.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, final String name) {
        String sname = name.toLowerCase();
        if (sname.lastIndexOf('.') <= 0) {
          return false; // no extension
        }
        int lastIndex = sname.lastIndexOf('.');
        // get extension
        String ext = sname.substring(lastIndex);
        if (!ext.equals(".jar")) {
          return false; // no jar extension
        }
        // now we have .jar file, check name pattern
        if (sname.contains(PATTERN)) {
          return true;
        } else {
          return false;
        }
      }
    });
    if (listFiles == null) {
      return new File[0]; // but if yes return empty array
    }
    // decode names from listFiles and fill availPlugins names and paths
    for (File f : listFiles) {
      // build plugin name from file name
      String filename = f.getName().toLowerCase();
      int lastindex = filename.lastIndexOf(PATTERN);
      // cut from beginning to -quimp
      String pluginName = filename.substring(0, lastindex);
      // change first letter to upper to match class-naming convention
      pluginName = pluginName.substring(0, 1).toUpperCase() + pluginName.substring(1);
      // check plugin type
      try {
        // ask for class names in jar
        String cname = getClassName(f);
        // make temporary instance
        Object inst = getPluginInstance(f, cname);
        // get type of path.classname plugin
        int type = getPluginType(inst);
        // get version of path.classname plugin
        String ver = getPluginVersion(inst);
        // create entry with classname and path
        availPlugins.put(pluginName, new PluginProperties(f, cname, type, ver));
        LOGGER.debug(
                "Registered plugin: " + pluginName + " " + availPlugins.get(pluginName).toString());
        // catch any error in plugin services - plugin is not stored
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException
              | InstantiationException | IllegalAccessException | IllegalArgumentException
              | InvocationTargetException | ClassCastException | IOException
              | NoClassDefFoundError e) {
        LOGGER.error("Type of plugin " + pluginName + " in jar: " + f.getPath()
                + " can not be obtained. Ignoring this plugin");
        LOGGER.debug(e.getMessage(), e);
      }

    }
    return Arrays.copyOf(listFiles, listFiles.length);
  }

  /**
   * Extracts qualified name of classes in jar file. Class name must contain underscore.
   * 
   * @param pathToJar path to jar file
   * @return Name of first discovered class with underscore
   * @throws IOException When jar can not be opened
   * @throws IllegalArgumentException when there is no classes in jar
   * @see <a href=
   *      "link">http://stackoverflow.com/questions/11016092/how-to-load-classes-at-runtime-from-a-folder-or-jar</a>
   */
  private String getClassName(File pathToJar) throws IOException {
    ArrayList<String> names = new ArrayList<>(); // all discovered names
    JarFile jarFile = new JarFile(pathToJar);
    Enumeration<JarEntry> e = jarFile.entries();

    while (e.hasMoreElements()) {
      JarEntry je = (JarEntry) e.nextElement();
      String entryname = je.getName();
      if (je.isDirectory() || !entryname.endsWith("_.class")) {
        continue;
      }
      // -6 because of .class
      String className = je.getName().substring(0, je.getName().length() - 6);
      className = className.replace('/', '.');
      names.add(className);
      LOGGER.debug("In " + pathToJar.toString() + " found class " + entryname);
    }
    jarFile.close();
    if (names.isEmpty()) {
      throw new IllegalArgumentException("getClassName: There is no underscored classes in jar");
    }
    if (names.size() > 1) {
      LOGGER.warn("More than one underscored class in jar " + pathToJar.toString()
              + " Take first one " + names.get(0));
    }
    return names.get(0);
  }

  /**
   * Gets type of plugin.
   * 
   * <p>Calls IQuimpPlugin.setup() method from plugin. <br>
   * <img src="doc-files/PluginFactory_4_UML.png"/><br>
   * 
   * @param instance Instance of plugin
   * @return Codes of types from IQuimpPlugin
   * @throws IllegalArgumentException When returned type is unknown
   * @throws NoSuchMethodException wrong plugin
   * @throws InvocationTargetException wrong plugin
   * @see com.github.celldynamics.quimp.plugin.IQuimpCorePlugin
   */
  private int getPluginType(Object instance)
          throws IllegalArgumentException, NoSuchMethodException, InvocationTargetException {

    int result = (int) ((IQuimpCorePlugin) instance).setup();
    // decode returned result for plugin type
    if ((result & IQuimpCorePlugin.DOES_SNAKES) == IQuimpCorePlugin.DOES_SNAKES) {
      return IQuimpCorePlugin.DOES_SNAKES;
    } else {
      throw new IllegalArgumentException("Plugin returned unknown type");
    }
  }

  /**
   * Gets version of plugin.
   * 
   * <p>Calls IQuimpPlugin.getVersion() method from plugin
   * 
   * @param instance Instance of plugin
   * @return String representing version of plugin or null if plugin does not support versioning
   * @throws NoSuchMethodException wrong plugin
   * @throws InvocationTargetException wrong plugin
   */
  private String getPluginVersion(Object instance)
          throws NoSuchMethodException, InvocationTargetException {
    return ((IQuimpCorePlugin) instance).getVersion();
  }

  /**
   * Creates instance of plugin. <br>
   * <img src="doc-files/PluginFactory_5_UML.png"/><br>
   * 
   * @param plugin plugin File handler to plugin
   * @param className full class name
   * @return className Formatted fully qualified class name
   * @throws InstantiationException wrong plugin
   * @throws IllegalAccessException wrong plugin
   * @throws ClassNotFoundException wrong plugin
   * @throws MalformedURLException wrong plugin
   */
  private Object getPluginInstance(final File plugin, final String className)
          throws InstantiationException, IllegalAccessException, ClassNotFoundException,
          MalformedURLException {
    URL[] url = new URL[] { plugin.toURI().toURL() };
    ClassLoader child = new URLClassLoader(url);
    LOGGER.trace("Trying to load class: " + className + " from " + plugin.toString());
    Class<?> classToLoad = Class.forName(className, true, child);
    Object instance = classToLoad.newInstance();
    return instance;
  }

  /**
   * Return list of plugins of given types.
   * 
   * @param type Type defined in com.github.celldynamics.plugin.IQuimpPlugin
   * @return List of names of plugins of type type. If there is no plugins in directory (this type
   *         or any) returned list has length 0
   */
  public ArrayList<String> getPluginNames(int type) {
    ArrayList<String> ret = new ArrayList<String>();
    // Iterate over our collection
    Iterator<Map.Entry<String, PluginProperties>> it = availPlugins.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, PluginProperties> me = it.next();
      if (me.getValue().getType() == type) {
        ret.add(me.getKey()); // add to list of plugins of this type
      }
    }
    if (ret.isEmpty()) {
      LOGGER.warn("No plugins found");
    }
    return ret;
  }

  /**
   * Return instance of named plugin.
   * 
   * <p><br>
   * <img src="doc-files/PluginFactory_6_UML.png"/><br>
   * 
   * <p>JAR is loaded only once on first request. Then its reference is stored in
   * {@link PluginProperties} and served on next demand. Thus, if plugin is used on different frames
   * in stack it is the same instance.
   * 
   * @param name Name of plugin compatible with general rules
   * @return reference to plugin of name or null when there is any problem with creating instance
   *         or given name does not exist in availPlugins base
   */
  public IQuimpCorePlugin getInstance(final String name) {
    try {
      if (name.isEmpty()) {
        throw new IllegalArgumentException("Plugin of name: " + name + " is not loaded");
      }
      // usually name of plugin is spelled with Capital letter first
      // make sure that name is in correct format
      String qname = name.substring(0, 1).toUpperCase() + name.substring(1);
      // find name in database
      PluginProperties pp = availPlugins.get(qname);
      if (pp == null) {
        throw new IllegalArgumentException("Plugin of name: " + name + " is not loaded");
      }
      // load class and create instance
      IQuimpCorePlugin instance;
      if (pp.getRef() == null) { // not used yet
        instance = (IQuimpCorePlugin) getPluginInstance(pp.getFile(), pp.getClassName());
        pp.setRef(instance); // store for next request
      } else {
        instance = pp.getRef(); // return same instance as previous for "name"
      }
      return instance;
    } catch (MalformedURLException | ClassNotFoundException | InstantiationException
            | IllegalAccessException | IllegalArgumentException e) {
      LOGGER.error("Plugin " + name + " can not be instanced (reason: " + e.getMessage() + ")");
      LOGGER.debug(e.getMessage(), e);
      return null;
    }

  }

  /**
   * Prints system classpath.
   */
  public static void getSystemClassPath() {
    LOGGER.trace("--- CLASPATH ---");
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    URL[] urls = ((URLClassLoader) cl).getURLs();
    for (URL urll : urls) {
      LOGGER.trace(urll.getFile());
    }
    LOGGER.trace("--- CLASPATH ---");
  }
}
