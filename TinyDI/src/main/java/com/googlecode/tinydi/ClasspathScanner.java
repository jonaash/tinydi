package com.googlecode.tinydi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans the classpath (both the filesystem and the jars) for classes inside a certain java package.
 *
 * @author Richard Pal
 *
 * Changed classloader that is used.
 * Characters %20 in paths are replaced with spaces.
 * @author Jonas Klimes
 */
public class ClasspathScanner {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  public List<String> getClassNamesFromPackage(String packagePrefix) throws IOException, URISyntaxException {
    ClassLoader classLoader = getClass().getClassLoader();
    ArrayList<String> classNameList = new ArrayList<String>();

    // replace "." with "/"
    String packageDir = packagePrefix.replace(".", "/") + "/";
    //URL packageURL = classLoader.getResource(packageDir);

    // fetch all JARs and filesystem urls which contain the package prefix:
    Enumeration<URL> resources = classLoader.getResources(packageDir);

    while(resources.hasMoreElements()) {
      URL url = resources.nextElement();
      logger.debug("url:{}", url);

      // decode URL to replace character %20 with spaces
      String path = URLDecoder.decode(url.getPath(), "UTF-8");
      logger.debug("path:{}", path);

      if(url.getProtocol().equals("jar")) {
        // extract from JAR
        getClassNamesFromJar(path, classNameList, packageDir);
      } else {
        // extract from filesystem:
        File folder = new File(path);
        getClassNamesFromFileSystem(classLoader, folder, classNameList, packagePrefix);
      }
    }
    return classNameList;
  }

  private void getClassNamesFromJar(String jarFileName, List<String> classList, String packagePrefix) throws
          IOException {
    jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
    logger.debug("JAR file name: {}", jarFileName);

    JarFile jf = new JarFile(jarFileName);
    Enumeration<JarEntry> jarEntries = jf.entries();

    while (jarEntries.hasMoreElements()){
      String entryName = jarEntries.nextElement().getName();
      // logger.debug("jar entry: {}", entryName);

      if (entryName.startsWith(packagePrefix)
        && entryName.length()> packagePrefix.length()+5
        && entryName.endsWith(".class")) {
        //entryName = entryName.substring(packagePrefix.length(),entryName.lastIndexOf('.'));
        entryName = entryName.substring(0,entryName.lastIndexOf('.')).replace("/", ".");
        classList.add(entryName);
        logger.debug("jar entry: {} added to list", entryName);
      }
    }
  }

  private void getClassNamesFromFileSystem(ClassLoader classLoader,
                                           File folder,
                                           List<String> classList,
                                           String packageName) {

    File[] folderContent = folder.listFiles();

    for (File actual: folderContent){
      String entryName = actual.getName();

      // is this a package directory?
      if (actual.isDirectory() && entryName.indexOf('.') == -1) {
        String childPackage = packageName + "." + entryName;
        logger.debug("drilling down to childpackage: {}", childPackage);

        getClassNamesFromFileSystem(classLoader, actual, classList, childPackage);

      } else if (entryName.endsWith(".class")) {
        // add the simple class:
        entryName = packageName + "." + entryName.substring(0, entryName.lastIndexOf('.'));
        logger.debug("adding class to list: {}", entryName);
        classList.add(entryName);
      }
    }
  }
}
