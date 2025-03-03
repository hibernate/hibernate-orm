/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.xml;

import java.io.IOException;
import java.io.InputStream;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.processor.Context;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public class ResourceStreamLocatorImpl implements ResourceStreamLocator {
	/**
	 * Path separator used for resource loading
	 */
	private static final String RESOURCE_PATH_SEPARATOR = "/";

	private final Context context;

	public ResourceStreamLocatorImpl(Context context) {
		this.context = context;
	}

	@Override
	public @Nullable InputStream locateResourceStream(String resourceName) {
		// METAGEN-75
		if ( !resourceName.startsWith( RESOURCE_PATH_SEPARATOR ) ) {
			resourceName = RESOURCE_PATH_SEPARATOR + resourceName;
		}

		String pkg = getPackage( resourceName );
		String name = getRelativeName( resourceName );
		InputStream ormStream;
		try {
			FileObject fileObject = context.getProcessingEnvironment()
					.getFiler()
					.getResource( StandardLocation.CLASS_OUTPUT, pkg, name );
			ormStream = fileObject.openInputStream();
		}
		catch ( IOException e1 ) {
			// TODO - METAGEN-12
			// unfortunately, the Filer.getResource API seems not to be able to load from /META-INF. One gets a
			// FilerException with the message with "Illegal name /META-INF". This means that we have to revert to
			// using the classpath. This might mean that we find a persistence.xml which is 'part of another jar.
			// Not sure what else we can do here
			ormStream = this.getClass().getResourceAsStream( resourceName );
		}
		return ormStream;
	}

	private String getPackage(String resourceName) {
		if ( !resourceName.contains("/") ) {
			return "";
		}
		else {
			return resourceName.substring( 0, resourceName.lastIndexOf("/") );
		}
	}

	private String getRelativeName(String resourceName) {
		if ( !resourceName.contains("/") ) {
			return resourceName;
		}
		else {
			return resourceName.substring( resourceName.lastIndexOf("/") + 1 );
		}
	}
}
