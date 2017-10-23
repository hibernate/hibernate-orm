/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance.discriminatoroptions;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the @DiscriminatorOptions annotations.
 *
 * @author Hardy Ferentschik
 */
public class DiscriminatorOptionsTest extends BaseUnitTestCase {
	@Test
	public void testNonDefaultOptions() throws Exception {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( BaseClass.class )
					.addAnnotatedClass( SubClass.class )
					.buildMetadata();

			PersistentClass persistentClass = metadata.getEntityBinding( BaseClass.class.getName() );
			assertNotNull( persistentClass );
			assertTrue( persistentClass instanceof RootClass );

			RootClass root = (RootClass) persistentClass;
			assertTrue( "Discriminator should be forced", root.isForceDiscriminator() );
			assertFalse( "Discriminator should not be insertable", root.isDiscriminatorInsertable() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testBaseline() throws Exception {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( BaseClass2.class )
					.addAnnotatedClass( SubClass2.class )
					.buildMetadata();

			PersistentClass persistentClass = metadata.getEntityBinding( BaseClass2.class.getName() );
			assertNotNull( persistentClass );
			assertTrue( persistentClass instanceof RootClass );

			RootClass root = ( RootClass ) persistentClass;
			assertFalse( "Discriminator should not be forced by default", root.isForceDiscriminator() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testPropertyBasedDiscriminatorForcing() throws Exception {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( BaseClass2.class )
					.addAnnotatedClass( SubClass2.class )
					.getMetadataBuilder()
					.enableImplicitForcingOfDiscriminatorsInSelect( true )
					.build();

			PersistentClass persistentClass = metadata.getEntityBinding( BaseClass2.class.getName() );
			assertNotNull( persistentClass );
			assertTrue( persistentClass instanceof RootClass );

			RootClass root = ( RootClass ) persistentClass;
			assertTrue( "Discriminator should be forced by property", root.isForceDiscriminator() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
