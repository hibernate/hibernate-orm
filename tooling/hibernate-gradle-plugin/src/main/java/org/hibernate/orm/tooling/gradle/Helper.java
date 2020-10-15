/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;

import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * @author Steve Ebersole
 */
public class Helper {

	public static ClassLoader toClassLoader(Directory classesDir) {
		final File classesDirFile = classesDir.getAsFile();
		final URI classesDirUri = classesDirFile.toURI();
		try {
			final URL url = classesDirUri.toURL();
			return new URLClassLoader( new URL[] { url }, Enhancer.class.getClassLoader() );
		}
		catch (MalformedURLException e) {
			throw new GradleException( "Unable to resolve classpath entry to URL : " + classesDirFile.getAbsolutePath(), e );
		}
	}

	public static String determineClassName(File root, File javaClassFile) {
		final Path relativeClassPath = root.toPath().relativize( javaClassFile.toPath() );
		final String relativeClassPathString = relativeClassPath.toString();
		final String classNameBase = relativeClassPathString.substring(
				0,
				relativeClassPathString.length() - ".class".length()
		);
		return classNameBase.replace( File.separatorChar, '.' );
	}
}
