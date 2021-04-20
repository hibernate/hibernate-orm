/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import java.io.IOException;
import java.io.InputStream;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.boot.jaxb.spi.Binding;

import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.junit.Assert;

/**
 * A small helper class for parsing XML mappings, to be used in unit tests.
 */
public final class XMLMappingHelper {
	private final MappingBinder binder;

	public XMLMappingHelper() {
		binder = new MappingBinder( ClassLoaderServiceTestingImpl.INSTANCE, true );
	}

	public JaxbEntityMappings readOrmXmlMappings(String name) throws IOException {
		try (InputStream is = ClassLoaderServiceTestingImpl.INSTANCE.locateResourceStream( name )) {
			return readOrmXmlMappings( is, name );
		}
	}

	public JaxbEntityMappings readOrmXmlMappings(InputStream is, String name) {
		try {
			Assert.assertNotNull( "Resource not found: " + name, is );
			Binding<?> binding = binder.bind( is, new Origin( SourceType.JAR, name ) );
			return (JaxbEntityMappings) binding.getRoot();
		}
		catch (RuntimeException e) {
			throw new IllegalStateException( "Could not parse orm.xml mapping '" + name + "': " + e.getMessage(), e );
		}
	}
}
