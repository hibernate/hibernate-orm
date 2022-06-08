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
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;

import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * @author Steve Ebersole
 */
public class Helper {

	public static String determineCompileSourceSetName(String name) {
		return determineCompileNameParts( name )[0];
	}

	public static String[] determineCompileNameParts(String name) {
		StringBuilder firstPart = null;
		StringBuilder secondPart = null;

		boolean processingFirstPart = false;
		boolean processingSecondPart = false;
		final char[] nameChars = name.toCharArray();
		for ( int i = 0; i < nameChars.length; i++ ) {
			final char nameChar = nameChars[ i ];
			if ( processingFirstPart ) {
				if ( Character.isUpperCase( nameChar ) ) {
					// this is the start of the second-part
					processingFirstPart = false;
					processingSecondPart = true;
					secondPart = new StringBuilder( String.valueOf( Character.toLowerCase( nameChar ) ) );
				}
				else {
					firstPart.append( nameChar );
				}
			}
			else if ( processingSecondPart ) {
				if ( Character.isUpperCase( nameChar ) ) {
					throw new RuntimeException( "Unexpected compilation task name : " + name );
				}
				else {
					secondPart.append( nameChar );
				}
			}
			else {
				if ( Character.isUpperCase( nameChar ) ) {
					processingFirstPart = true;
					firstPart = new StringBuilder( String.valueOf( Character.toLowerCase( nameChar ) ) );
				}
			}
		}

		if ( firstPart == null ) {
			throw new RuntimeException( "Unexpected compilation task name : " + name );
		}

		if ( secondPart == null ) {
			return new String[] { "main", firstPart.toString() };
		}

		return new String[] { firstPart.toString(), secondPart.toString() };
	}


	public static ClassLoader toClassLoader(FileCollection directories) {
		final Set<File> files = directories.getFiles();
		final URL[] urls = new URL[ files.size() ];
		int index = 0;
		for ( File classesDir : files ) {
			final URI classesDirUri = classesDir.toURI();
			try {
				urls[index] = classesDirUri.toURL();
			}
			catch (MalformedURLException e) {
				throw new GradleException( "Unable to resolve classpath entry to URL : " + classesDir.getAbsolutePath(), e );
			}

			index++;
		}
		return new URLClassLoader( urls, Enhancer.class.getClassLoader() );
	}

	public static ClassLoader toClassLoader(File classesDir) {
		final URI classesDirUri = classesDir.toURI();
		try {
			final URL url = classesDirUri.toURL();
			return new URLClassLoader( new URL[] { url }, Enhancer.class.getClassLoader() );
		}
		catch (MalformedURLException e) {
			throw new GradleException( "Unable to resolve classpath entry to URL : " + classesDir.getAbsolutePath(), e );
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
