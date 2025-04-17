/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import com.fasterxml.jackson.core.JsonFactory;
import oracle.jdbc.provider.oson.OsonFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.type.format.jackson.JacksonIntegration;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hibernate.type.format.jackson.JacksonIntegration.isJacksonOsonExtensionAvailable;

public class OracleOsonJacksonHelper {

	public static final @Nullable Class<? extends Closeable> READER_CLASS = loadOrNull( "com.fasterxml.jackson.core.JsonParser" );
	public static final @Nullable Class<? extends Closeable> WRITER_CLASS = loadOrNull( "com.fasterxml.jackson.core.JsonGenerator" );

	private static @Nullable Class<? extends Closeable> loadOrNull(String name) {
		try {
			//N.B. intentionally not using the context classloader
			// as we're storing these in static references;
			// IMO it's reasonable to expect that such dependencies are made reachable from the ORM classloader.
			// (we can change this if it's more problematic than expected).
			//noinspection unchecked
			return (Class<? extends Closeable>) JacksonIntegration.class.getClassLoader().loadClass( name );
		}
		catch (ClassNotFoundException | LinkageError e) {
			return null;
		}
	}

	private OracleOsonJacksonHelper() {
	}

	public static Closeable createWriteTarget(OutputStream out) throws IOException {
		return FactoryHolder.JACKSON_FACTORY.createGenerator( out );
	}


	public static Closeable createReadSource(InputStream osonBytes) throws IOException {
		return FactoryHolder.JACKSON_FACTORY.createParser( osonBytes );
	}

	private static final class FactoryHolder {
		// Intentionally storing the jackson typed factory in a different class,
		// to avoid linkage errors for the outer class if Jackson is not available
		private static final JsonFactory JACKSON_FACTORY = isJacksonOsonExtensionAvailable()
				? new OsonFactory()
				: new JsonFactory();

	}

}
