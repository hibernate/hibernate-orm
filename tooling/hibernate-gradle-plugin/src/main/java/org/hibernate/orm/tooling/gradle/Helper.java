/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;

import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * @author Steve Ebersole
 */
public class Helper {

	public static ClassLoader toClassLoader(FileCollection classesDirs, Set<File> dependencyFiles) {
		final List<URL> urls = new ArrayList<>();

		for ( File classesDir : classesDirs ) {
			urls.add( toUrl( classesDir ) );
		}

		for ( File dependencyFile : dependencyFiles ) {
			urls.add( toUrl( dependencyFile ) );
		}

		return new URLClassLoader( urls.toArray(new URL[0]), Enhancer.class.getClassLoader() );
	}

	private static URL toUrl(File file) {
		final URI classesDirUri = file.toURI();
		try {
			return classesDirUri.toURL();
		}
		catch (MalformedURLException e) {
			throw new GradleException( "Unable to resolve classpath entry to URL : " + file.getAbsolutePath(), e );
		}
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
