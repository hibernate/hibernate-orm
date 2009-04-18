package org.hibernate.ejb.packaging;

import java.util.Set;
import java.util.List;
import java.net.URL;
import java.lang.annotation.Annotation;
import java.io.InputStream;

/**
 * @author Emmanuel Bernard
 */
public interface Scanner {
	/**
	 * return all packages in the jar matching one of these annotations
	 * if annotationsToLookFor is empty, return all packages
	 */
	Set<Package> getPackagesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor);

	/**
	 * return all classes in the jar matching one of these annotations
	 * if annotationsToLookFor is empty, return all classes
	 */
	Set<Class<?>> getClassesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor);

	/**
	 * return all files in the jar matching one of these file names
	 * if filePatterns is empty, return all files
	 * eg **\/*.hbm.xml, META-INF/orm.xml
	 */
	Set<NamedInputStream> getFilesInJar(URL jartoScan, Set<String> filePatterns);


	/**
	 * return all files in the classpath (ie PU visibility) matching one of these file names
	 * if filePatterns is empty, return all files
	 * the use case is really exact file name.
	 */
	Set<NamedInputStream> getFilesInClasspath(URL jartoScan, Set<String> filePatterns);

	/**
	 * return the unqualified JAR name ie customer-model.jar or store.war
	 */
	String getUnqualifiedJarName(URL jarUrl);

}
