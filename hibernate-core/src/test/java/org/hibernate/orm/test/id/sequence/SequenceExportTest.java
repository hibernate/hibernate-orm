/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.sequence;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-10320")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class )
@ServiceRegistry
public class SequenceExportTest implements ServiceRegistryProducer {

	@Test
	@JiraKey("HHH-9936")
	public void testMultipleUsesOfDefaultSequenceName(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = MetadataBuildingTestHelper.buildValidatedMetadata(
				registryScope.getRegistry(),
				new MappingSources()
						.addManagedClass( Entity1.class )
						.addManagedClass( Entity2.class )
		);

		int namespaceCount = 0;
		int sequenceCount = 0;
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			namespaceCount++;
			for ( Sequence sequence : namespace.getSequences() ) {
				sequenceCount++;
			}
		}

		assertThat( namespaceCount ).isEqualTo( 1 );
		// 1 per entity
		assertThat( sequenceCount ).isEqualTo( 2 );
	}

	@Test
	@JiraKey("HHH-9936")
	public void testMultipleUsesOfExplicitSequenceName(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = MetadataBuildingTestHelper.buildValidatedMetadata(
				registryScope.getRegistry(),
				new MappingSources()
						.addManagedClass( Entity3.class )
						.addManagedClass( Entity4.class )
		);

		int namespaceCount = 0;
		int sequenceCount = 0;
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			namespaceCount++;
			for ( Sequence sequence : namespace.getSequences() ) {
				sequenceCount++;
			}
		}

		assertThat( namespaceCount ).isEqualTo( 1 );
		assertThat( sequenceCount ).isEqualTo( 1 );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return ServiceRegistryUtil.applySettings( builder ).build();
	}

	@Entity( name = "Entity1" )
	@Table( name = "Entity1" )
	public static class Entity1 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
		public Integer id;
	}

	@Entity( name = "Entity2" )
	@Table( name = "Entity2" )
	public static class Entity2 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE )
		public Integer id;
	}

	@Entity( name = "Entity3" )
	@Table( name = "Entity3" )
	public static class Entity3 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "my_sequence" )
		public Integer id;
	}

	@Entity( name = "Entity4" )
	@Table( name = "Entity4" )
	public static class Entity4 {
		@Id
		@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "my_sequence" )
		public Integer id;
	}
}
