/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import java.util.Locale;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test harness for ANN-716.
 *
 * @author Hardy Ferentschik
 */
public class NamingStrategyTest extends BaseUnitTestCase {

	private ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithCustomNamingStrategy() throws Exception {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass(Address.class)
				.addAnnotatedClass(Person.class)
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new DummyNamingStrategy() )
				.build();
	}

	@Test
	public void testWithUpperCaseNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass(A.class)
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new PhysicalNamingStrategyStandardImpl() {
					@Override
					public Identifier toPhysicalColumnName(
							Identifier logicalName, JdbcEnvironment context) {
						return new Identifier( logicalName.getText().toUpperCase(), logicalName.isQuoted() );
					}
				} )
				.build();

		PersistentClass entityBinding = metadata.getEntityBinding( A.class.getName() );
		assertEquals("NAME",
					((Selectable) entityBinding.getProperty( "name" ).getSelectables().get( 0 ) ).getText());
		assertEquals("VALUE",
					((Selectable) entityBinding.getProperty( "value" ).getSelectables().get( 0 ) ).getText());
	}

	@Test
	public void testWithJpaCompliantNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( AddressEntry.class )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE )
				.build();

		Collection collectionBinding = metadata.getCollectionBinding( A.class.getName() + ".address" );
		assertEquals(
				"Expecting A#address collection table name (implicit) to be [A_address] per JPA spec (section 11.1.8)",
				"A_ADDRESS",
				collectionBinding.getCollectionTable().getQuotedName().toUpperCase(Locale.ROOT)
		);
	}

	@Test
	public void testWithoutCustomNamingStrategy() throws Exception {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.buildMetadata();
	}
}
