/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.artifacts.Configuration;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static ClassLoader asClassLoader(Configuration... configurations) {
		final List<URL> urls = new ArrayList<>();

		for ( Configuration configuration : configurations ) {
			for ( File dependencyFile : configuration.resolve() ) {
				addElement( urls, dependencyFile );
			}
		}

		return new URLClassLoader( urls.toArray( new URL[0] ) );
	}

	private static void addElement(List<URL> urls, File element) {
		try {
			urls.add( element.toURI().toURL() );
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( "Unable to create URL for ClassLoader: " + element.getAbsolutePath(), e );
		}
	}
}
