/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test harness for ANN-716.
 *
 * @author Hardy Ferentschik
 */
@BaseUnitTest
public class NamingStrategyTest {

	private ServiceRegistry serviceRegistry;

	@BeforeAll
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterAll
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithCustomNamingStrategy() {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new DummyNamingStrategy() )
				.build();
	}

	@Test
	public void testWithUpperCaseNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( A.class )
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
		assertThat( entityBinding.getProperty( "name" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "NAME" );
		assertThat( entityBinding.getProperty( "value" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "VALUE" );
	}

	@Test
	public void testWithJpaCompliantNamingStrategy() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( AddressEntry.class )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE )
				.build();

		Collection collectionBinding = metadata.getCollectionBinding( A.class.getName() + ".address" );
		assertThat( collectionBinding.getCollectionTable().getQuotedName().toUpperCase( Locale.ROOT ) )
				.describedAs(
						"Expecting A#address collection table name (implicit) to be [A_address] per JPA spec (section 11.1.8)"
				)
				.isEqualTo( "A_ADDRESS" );
	}

	@Test
	public void testWithoutCustomNamingStrategy() {
		new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.buildMetadata();
	}
}
