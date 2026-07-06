/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.collectionJoinTableNaming;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Steve Ebersole
 * @author Alessandro Polverini
 */
@BaseUnitTest
public class CollectionJoinTableNamingTest {

	@Test
	@JiraKey( value = "HHH-9908" )
	public void testCollectionJoinTableNamingBase() {
		// really the same as the JPA compliant tests; here we just pick up the default ImplicitNamingStrategy
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final Metadata metadata = buildMetadata( serviceRegistry, null );

			assertSameTableUsed( metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@JiraKey( value = "HHH-9908" )
	public void testCollectionJoinTableNamingLegacyJpaStrategy() {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final Metadata metadata = buildMetadata( serviceRegistry, ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );

			assertSameTableUsed( metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@JiraKey( value = "HHH-9908" )
	public void testCollectionJoinTableNamingLegacyHbmStrategy() {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final Metadata metadata = buildMetadata( serviceRegistry, ImplicitNamingStrategyLegacyHbmImpl.INSTANCE );

			Collection inputs1Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs1" );
			assertEquals( "ptx_inputs1", inputs1Mapping.getCollectionTable().getName() );

			Collection inputs2Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs2" );
			assertEquals( "ptx_inputs2", inputs2Mapping.getCollectionTable().getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@JiraKey( value = "HHH-9908" )
	public void testCollectionJoinTableNamingJpaCompliantStrategy() {
		// Even in 4.3, with JPA compliant naming, Hibernate creates an unusable table...

		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final Metadata metadata = buildMetadata( serviceRegistry, ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );

			assertSameTableUsed( metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	private Metadata buildMetadata(
			StandardServiceRegistry serviceRegistry,
			ImplicitNamingStrategy implicitNamingStrategy) {
		final MappingSources mappingSources = new MappingSources()
				.addManagedClass( Input.class )
				.addManagedClass( Ptx.class );
		if ( implicitNamingStrategy == null ) {
			return MetadataBuildingTestHelper.buildMetadata( serviceRegistry, mappingSources );
		}
		return MetadataBuildingTestHelper.buildMetadataWithImplicitNaming(
				serviceRegistry,
				mappingSources,
				implicitNamingStrategy
		);
	}

	private void assertSameTableUsed(Metadata metadata) {
		Collection inputs1Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs1" );
		assertEquals( "ptx_input", inputs1Mapping.getCollectionTable().getName() );

		Collection inputs2Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs2" );
		assertEquals( "ptx_input", inputs2Mapping.getCollectionTable().getName() );

		Assertions.assertSame( inputs1Mapping.getCollectionTable(), inputs2Mapping.getCollectionTable() );

		// NOTE : here so that tester can more easily see the produced table. It is only dumped to stdout
		new SchemaExport().create( EnumSet.of( TargetType.STDOUT ), metadata );

		for ( int i = 0; i < inputs1Mapping.getCollectionTable().getColumnSpan(); i++ ) {
			final Column column = inputs1Mapping.getCollectionTable().getColumn( i );

			// this, coupled with JPA saying the 2 collections implicitly map to the same table,
			// is the crux of the problem: all columns are null, so we effectively can never
			// insert rows into it.
			assertFalse( column.isNullable() );
		}
	}

	@Entity
	@Table(name = "ptx")
	public static class Ptx {
		@Id
		@GeneratedValue
		private Integer id;

		@OrderColumn
		@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
		private List<Input> inputs1;

		@OrderColumn
		@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
		private List<Input> inputs2;

	}

	@Entity
	@Table(name = "input")
	public class Input implements Serializable {
		@Id
		@GeneratedValue
		private Integer id;
	}
}
