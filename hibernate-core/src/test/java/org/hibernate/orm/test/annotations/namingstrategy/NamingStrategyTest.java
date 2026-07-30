/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

	private StandardServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithCustomNamingStrategy() {
		MetadataBuildingTestHelper.buildMetadataWithPhysicalNaming(
				serviceRegistry,
				new MappingSources().addManagedClasses( Address.class, Person.class ),
				new DummyNamingStrategy()
		);
	}

	@Test
	public void testWithUpperCaseNamingStrategy() throws Exception {
		Metadata metadata = MetadataBuildingTestHelper.buildMetadataWithPhysicalNaming(
				serviceRegistry,
				new MappingSources().addManagedClass( A.class ),
				new PhysicalNamingStrategyStandardImpl() {
					@Override
					public Identifier toPhysicalColumnName(
							Identifier logicalName, JdbcEnvironment context) {
						return new Identifier( logicalName.getText().toUpperCase(), logicalName.isQuoted() );
					}
				}
		);

		PersistentClass entityBinding = metadata.getEntityBinding( A.class.getName() );
		assertThat( entityBinding.getProperty( "name" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "NAME" );
		assertThat( entityBinding.getProperty( "value" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "VALUE" );
	}

	@Test
	public void testWithJpaCompliantNamingStrategy() {
		Metadata metadata = MetadataBuildingTestHelper.buildMetadataWithImplicitNaming(
				serviceRegistry,
				new MappingSources().addManagedClasses( A.class, AddressEntry.class ),
				ImplicitNamingStrategyJpaCompliantImpl.INSTANCE
		);

		Collection collectionBinding = metadata.getCollectionBinding( A.class.getName() + ".address" );
		assertThat( collectionBinding.getCollectionTable().getQuotedName().toUpperCase( Locale.ROOT ) )
				.describedAs(
						"Expecting A#address collection table name (implicit) to be [A_address] per JPA spec (section 11.1.8)"
				)
				.isEqualTo( "A_ADDRESS" );
	}

	@Test
	public void testWithoutCustomNamingStrategy() {
		MetadataBuildingTestHelper.buildMetadata( serviceRegistry, Address.class, Person.class );
	}
}
