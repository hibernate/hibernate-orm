/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal.util.xml;

import java.io.IOException;
import java.io.InputStream;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;

import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.junit.jupiter.api.Assertions;

/**
 * A small helper class for parsing XML mappings, to be used in unit tests.
 */
public final class XMLMappingHelper {
	private final MappingBinder binder;

	public XMLMappingHelper() {
		binder = new MappingBinder( ClassLoaderServiceTestingImpl.INSTANCE, MappingBinder.VALIDATING );
	}

	public JaxbEntityMappingsImpl readOrmXmlMappings(String name) throws IOException {
		try ( InputStream is = ClassLoaderServiceTestingImpl.INSTANCE.locateResourceStream( name ) ) {
			return readOrmXmlMappings( is, name );
		}
	}

	public JaxbEntityMappingsImpl readOrmXmlMappings(InputStream is, String name) {
		try {
			Assertions.assertNotNull( is, "Resource not found: " + name );
			Binding<?> binding = binder.bind( is, new Origin( SourceType.JAR, name ) );
			return (JaxbEntityMappingsImpl) binding.getRoot();
		}
		catch (RuntimeException e) {
			throw new IllegalStateException( "Could not parse orm.xml mapping '" + name + "': " + e.getMessage(), e );
		}
	}
}
