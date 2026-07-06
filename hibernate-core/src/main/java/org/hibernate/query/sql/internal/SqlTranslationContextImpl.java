/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.SqlTranslationContext;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Set;

/// Default [SqlTranslationContext] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public class SqlTranslationContextImpl implements SqlTranslationContext {
	private final RuntimeMetamodelsImplementor runtimeMetamodels;
	private final SessionFactoryOptions sessionFactoryOptions;
	private final TypeConfiguration typeConfiguration;
	private final SqmCreationContext sqmCreationContext;
	private final SqmFunctionRegistry sqmFunctionRegistry;
	private final Dialect dialect;
	private final WrapperOptions wrapperOptions;
	private final FetchProfileRegistry fetchProfileRegistry;

	public SqlTranslationContextImpl(
			RuntimeMetamodelsImplementor runtimeMetamodels,
			SessionFactoryOptions sessionFactoryOptions,
			TypeConfiguration typeConfiguration,
			QueryEngine queryEngine,
			WrapperOptions wrapperOptions,
			FetchProfileRegistry fetchProfileRegistry) {
		this.runtimeMetamodels = runtimeMetamodels;
		this.sessionFactoryOptions = sessionFactoryOptions;
		this.typeConfiguration = typeConfiguration;
		this.sqmCreationContext = queryEngine.getCriteriaBuilder();
		this.sqmFunctionRegistry = queryEngine.getSqmFunctionRegistry();
		this.dialect = queryEngine.getDialect();
		this.wrapperOptions = wrapperOptions;
		this.fetchProfileRegistry = fetchProfileRegistry;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return runtimeMetamodels.getMappingMetamodel();
	}

	@Override
	public SqmCreationContext getSqmCreationContext() {
		return sqmCreationContext;
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return sessionFactoryOptions.getMaximumFetchDepth();
	}

	@Override
	public boolean isJpaQueryComplianceEnabled() {
		return sessionFactoryOptions.getJpaCompliance().isJpaQueryComplianceEnabled();
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return runtimeMetamodels.getJpaMetamodel();
	}

	@Override
	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public WrapperOptions getWrapperOptions() {
		return wrapperOptions;
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return fetchProfileRegistry.get( name );
	}

	public boolean containsFetchProfileDefinition(String name) {
		return fetchProfileRegistry.contains( name );
	}

	public Set<String> getDefinedFetchProfileNames() {
		return fetchProfileRegistry.names();
	}
}
