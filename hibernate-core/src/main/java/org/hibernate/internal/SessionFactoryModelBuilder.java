/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.Map;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.internal.RuntimeMetamodelsImpl;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.query.internal.QueryEngineImpl;
import org.hibernate.query.sql.internal.FetchProfileRegistry;
import org.hibernate.query.sql.internal.SqlTranslationContextImpl;
import org.hibernate.query.sql.internal.SqlTranslationEngineImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.spi.TypeConfiguration;

/// Builds the in-flight model/query infrastructure used during
/// SessionFactory construction.
///
/// @since 9.0
/// @author Steve Ebersole
final class SessionFactoryModelBuilder {
	private SessionFactoryModelBuilder() {
	}

	static InFlightSessionFactoryModel buildInFlight(
			MetadataImplementor bootMetamodel,
			SessionFactoryOptions options,
			TypeConfiguration typeConfiguration,
			WrapperOptions wrapperOptions,
			ServiceRegistryImplementor serviceRegistry,
			Map<String, Object> settings,
			String sessionFactoryName) {
		final var runtimeMetamodelsImpl = new RuntimeMetamodelsImpl( typeConfiguration );

		// Build this before the mapping model is finished because some SQL AST
		// rendering during persister initialization needs the SqmFunctionRegistry.
		final var queryEngine = new QueryEngineImpl(
				bootMetamodel,
				options,
				runtimeMetamodelsImpl,
				serviceRegistry,
				settings,
				sessionFactoryName
		);
		final var fetchProfileRegistry = new FetchProfileRegistry();
		final var sqlTranslationContext = new SqlTranslationContextImpl(
				runtimeMetamodelsImpl,
				options,
				typeConfiguration,
				queryEngine,
				wrapperOptions,
				fetchProfileRegistry
		);
		final var sqlTranslationEngine = new SqlTranslationEngineImpl( sqlTranslationContext );

		final var mappingMetamodelImpl = new MappingMetamodelImpl( typeConfiguration, serviceRegistry );
		runtimeMetamodelsImpl.setMappingMetamodel( mappingMetamodelImpl );

		return new InFlightSessionFactoryModel(
				runtimeMetamodelsImpl,
				queryEngine,
				sqlTranslationEngine,
				mappingMetamodelImpl,
				fetchProfileRegistry
		);
	}
}
