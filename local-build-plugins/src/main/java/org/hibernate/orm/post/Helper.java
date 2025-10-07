/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.file.FileCollection;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static ClassLoader asClassLoader(FileCollection... fileCollections) {
		final List<URL> urls = new ArrayList<>();

		for ( FileCollection fileCollection : fileCollections ) {
			fileCollection.getFiles().forEach( dependencyFile -> {
				addElement( urls, dependencyFile );
			} );
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
