/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.metamodel.unified.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.metamodel.internal.ClassLoaderAccessImpl;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbBasic;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.xml.internal.jaxb.UnifiedMappingBinder;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class SimpleBindingTest extends BaseUnitTestCase {
	private UnifiedMappingBinder binder = new UnifiedMappingBinder( true,
			new ClassLoaderAccessImpl( null, new BootstrapServiceRegistryImpl() ) );

	@Test
	public void simpleSpecCompliantMappingTest() throws Exception {
		JaxbEntityMappings entityMappings = fromResource(
				"org/hibernate/test/metamodel/unified/jaxb/spec-compliant-orm.xml"
		);

		check( entityMappings, false );
	}

	@Test
	public void simpleExtendedMappingTest() throws Exception {
		JaxbEntityMappings entityMappings = fromResource(
				"org/hibernate/test/metamodel/unified/jaxb/extended-mapping.xml"
		);

		check( entityMappings, true );
	}

	@Test
	public void simpleLegacyMappingTest() throws Exception {
		JaxbEntityMappings entityMappings = fromResource(
				"org/hibernate/test/metamodel/unified/jaxb/legacy-mapping.hbm.xml"
		);

//		check( entityMappings, true );
	}

	private JaxbEntityMappings fromResource(String resource) throws Exception {
		final Origin origin = new Origin( SourceType.RESOURCE, resource );
		InputStream stream = getClass().getClassLoader().getResource( origin.getName() ).openStream();
		try {
			return binder.bind( stream, origin );
		}
		finally {
			stream.close();
		}
	}

	private void check(JaxbEntityMappings entityMappings, boolean expectNaturalId) {
		assertEquals( 1, entityMappings.getEntity().size() );
		JaxbEntity entity = entityMappings.getEntity().get( 0 );
		assertEquals( User.class.getName(), entity.getClazz() );

		JaxbAttributes attributes = entity.getAttributes();

		assertEquals( 1, attributes.getId().size() );
		assertEquals( "id", attributes.getId().get( 0 ).getName() );

		if ( expectNaturalId ) {
			assertEquals( 1, attributes.getBasic().size() );
			assertEquals( "realName", attributes.getBasic().get( 0 ).getName() );

			assertEquals( 1, attributes.getNaturalId().getBasic().size() );
			assertEquals( "username", attributes.getNaturalId().getBasic().get( 0 ).getName() );
		}
		else {
			assertEquals( 2, attributes.getBasic().size() );
			boolean foundUsername = false;
			boolean foundRealName = false;

			for ( JaxbBasic basic : attributes.getBasic() ) {
				if ( "username".equals( basic.getName() ) ) {
					foundUsername = true;
				}
				else if ( "realName".equals( basic.getName() ) ) {
					foundRealName = true;
				}
				else {
					fail( "Found unexpected entity attribute : " + basic.getName() );
				}
			}

			assertTrue( foundRealName );
			assertTrue( foundUsername );
		}
	}
}
