/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.serial.internal.MappingResolutionDetailsCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.MappingContext;

/// Mutable mapping products and options used while resolving mappings.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappingResolutionState(
		InFlightMetadataCollector metadataCollector,
		MappingContext mappingContext,
		Database database,
		MappingResolutionOptions options,
		TypeDefinitionRegistry typeDefinitionRegistry,
		Map<String, BasicType<?>> adHocBasicTypeRegistrations,
		MappingResolutionDetailsCollector resolutionDetailsCollector) {

	public MappingResolutionState(
			InFlightMetadataCollector metadataCollector,
			MappingResolutionOptions options,
			TypeDefinitionRegistry typeDefinitionRegistry) {
		this( metadataCollector, options, typeDefinitionRegistry, new HashMap<>() );
	}

	public MappingResolutionState(
			MappingContext mappingContext,
			Database database,
			MappingResolutionOptions options,
			TypeDefinitionRegistry typeDefinitionRegistry) {
		this( null, mappingContext, database, options, typeDefinitionRegistry, new HashMap<>(), null );
	}

	private MappingResolutionState(
			InFlightMetadataCollector metadataCollector,
			MappingResolutionOptions options,
			TypeDefinitionRegistry typeDefinitionRegistry,
			Map<String, BasicType<?>> adHocBasicTypeRegistrations) {
		this(
				metadataCollector,
				metadataCollector,
				metadataCollector.getDatabase(),
				options,
				typeDefinitionRegistry,
				adHocBasicTypeRegistrations,
				options.getResolutionDetailsCollector()
		);
	}

	public static MappingResolutionState from(MetadataBuildingContext buildingContext) {
		return new MappingResolutionState(
				buildingContext.getMetadataCollector(),
				buildingContext.getBuildingPlan(),
				buildingContext.getTypeDefinitionRegistry()
		);
	}

	public void captureResolutionDetails(org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails details) {
		if ( resolutionDetailsCollector != null ) {
			resolutionDetailsCollector.capture( details );
		}
	}

	public void registerAdHocBasicType(BasicType<?> basicType) {
		adHocBasicTypeRegistrations.put( basicType.getName(), basicType );
	}

	public <T> BasicType<T> resolveAdHocBasicType(String key) {
		//noinspection unchecked
		return (BasicType<T>) adHocBasicTypeRegistrations.get( key );
	}

	public <T> BasicType<T> findAdHocBasicType(JavaType<T> javaType, JdbcType jdbcType) {
		for ( BasicType<?> basicType : adHocBasicTypeRegistrations.values() ) {
			if ( basicType.getClass() == BasicTypeImpl.class
					&& basicType.getJavaTypeDescriptor() == javaType
					&& basicType.getJdbcType() == jdbcType ) {
				//noinspection unchecked
				return (BasicType<T>) basicType;
			}
		}

		return null;
	}
}
