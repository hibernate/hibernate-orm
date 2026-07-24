/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.spi.TypeConfiguration;

public class SqlTranslationEngineImpl implements SqlTranslationEngine {

	//TODO: consider unifying with SqlStringGenerationContextImpl

	private final SqlTranslationContextImpl context;

	public SqlTranslationEngineImpl(SqlTranslationContextImpl context) {
		this.context = context;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return context.getTypeConfiguration();
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return context.getMappingMetamodel();
	}

	@Override
	public SqmCreationContext getSqmCreationContext() {
		return context.getSqmCreationContext();
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return context.getMaximumFetchDepth();
	}

	@Override
	public boolean isJpaQueryComplianceEnabled() {
		return context.isJpaQueryComplianceEnabled();
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return context.getJpaMetamodel();
	}

	@Override
	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return context.getSqmFunctionRegistry();
	}

	@Override
	public Dialect getDialect() {
		return context.getDialect();
	}

	@Override
	public WrapperOptions getWrapperOptions() {
		return context.getWrapperOptions();
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return context.getFetchProfile( name );
	}

	@Override
	public boolean containsFetchProfileDefinition(String name) {
		return context.containsFetchProfileDefinition( name );
	}

	@Override
	public Set<String> getDefinedFetchProfileNames() {
		return context.getDefinedFetchProfileNames();
	}
}
