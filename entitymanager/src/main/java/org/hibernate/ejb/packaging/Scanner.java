/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.packaging;

import java.util.Set;
import java.net.URL;
import java.lang.annotation.Annotation;

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
	 * Return all files in the classpath (ie PU visibility) matching one of these file names
	 * if filePatterns is empty, return all files
	 * the use case is really exact file name.
	 *
	 * NOT USED by HEM at the moment. We use exact file search via getResourceAsStream for now.
	 */
	Set<NamedInputStream> getFilesInClasspath(Set<String> filePatterns);

	/**
	 * return the unqualified JAR name ie customer-model.jar or store.war
	 */
	String getUnqualifiedJarName(URL jarUrl);

}
