/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.MappingCustomizations;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;

import jakarta.persistence.SharedCacheMode;

public final class MetadataBuildingTestHelper {
	private MetadataBuildingTestHelper() {
	}

	public static Metadata buildMetadata(StandardServiceRegistry serviceRegistry) {
		return buildMetadata( serviceRegistry, new MappingSources() );
	}

	public static Metadata buildMetadata(StandardServiceRegistry serviceRegistry, MappingSources mappingSources) {
		return MetadataBuildingHelper.buildMetadata( serviceRegistry, mappingSources );
	}

	public static Metadata buildMetadata(StandardServiceRegistry serviceRegistry, Class<?>... managedClasses) {
		return buildMetadata( serviceRegistry, mappingSources( managedClasses ) );
	}

	public static MetadataImplementor buildValidatedMetadata(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources) {
		final MetadataImplementor metadata = MetadataBuildingHelper.buildMetadata( serviceRegistry, mappingSources );
		metadata.orderColumns( false );
		metadata.validate();
		return metadata;
	}

	public static MetadataImplementor buildValidatedMetadata(
			StandardServiceRegistry serviceRegistry,
			Class<?>... managedClasses) {
		return buildValidatedMetadata( serviceRegistry, mappingSources( managedClasses ) );
	}

	public static Metadata buildMetadata(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			MappingCustomizations mappingCustomizations) {
		return MetadataBuildingHelper.buildMetadata( serviceRegistry, mappingSources, mappingCustomizations );
	}

	public static Metadata buildMetadataWithImplicitNaming(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			ImplicitNamingStrategy implicitNamingStrategy) {
		return buildMetadata( serviceRegistry, mappingSources, customizations( implicitNamingStrategy, null ) );
	}

	public static Metadata buildMetadataWithPhysicalNaming(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			PhysicalNamingStrategy physicalNamingStrategy) {
		return buildMetadata( serviceRegistry, mappingSources, customizations( null, physicalNamingStrategy ) );
	}

	public static Metadata buildMetadataWithNaming(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			ImplicitNamingStrategy implicitNamingStrategy,
			PhysicalNamingStrategy physicalNamingStrategy) {
		return buildMetadata(
				serviceRegistry,
				mappingSources,
				customizations( implicitNamingStrategy, physicalNamingStrategy )
		);
	}

	public static Metadata buildMetadataWithAttributeConverters(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			ConverterDescriptor<?, ?>... attributeConverters) {
		return buildMetadata(
				serviceRegistry,
				mappingSources,
				new MappingCustomizations(
						Map.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of( attributeConverters ),
						null,
						null,
						null,
						null
				)
		);
	}

	public static Metadata buildMetadataWithSharedCacheMode(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			SharedCacheMode sharedCacheMode) {
		return buildMetadata(
				serviceRegistry,
				mappingSources,
				new MappingCustomizations(
						Map.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						List.of(),
						null,
						null,
						null,
						sharedCacheMode
				)
		);
	}

	private static MappingCustomizations customizations(
			ImplicitNamingStrategy implicitNamingStrategy,
			PhysicalNamingStrategy physicalNamingStrategy) {
		return new MappingCustomizations(
				Map.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				implicitNamingStrategy,
				physicalNamingStrategy,
				null,
				null
		);
	}

	private static MappingSources mappingSources(Class<?>... managedClasses) {
		final MappingSources mappingSources = new MappingSources();
		if ( managedClasses != null ) {
			for ( Class<?> managedClass : managedClasses ) {
				mappingSources.addManagedClass( managedClass );
			}
		}
		return mappingSources;
	}
}
