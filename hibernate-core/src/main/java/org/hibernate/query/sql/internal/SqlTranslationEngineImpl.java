/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

public class SqlTranslationEngineImpl implements SqlTranslationEngine {

	//TODO: consider unifying with SqlStringGenerationContextImpl

	private final SessionFactoryImplementor factory;
	private final TypeConfiguration typeConfiguration;
	private final Map<String, FetchProfile> fetchProfiles;

	public SqlTranslationEngineImpl(
			SessionFactoryImplementor factory,
			TypeConfiguration typeConfiguration,
			Map<String, FetchProfile> fetchProfiles) {
		this.factory = factory;
		this.typeConfiguration = typeConfiguration;
		this.fetchProfiles = fetchProfiles;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return factory.getMappingMetamodel();
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return factory.getSessionFactoryOptions().getMaximumFetchDepth();
	}

	@Override
	public boolean isJpaQueryComplianceEnabled() {
		return factory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled();
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return factory.getJpaMetamodel();
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return fetchProfiles.get( name );
	}

	@Override
	public boolean containsFetchProfileDefinition(String name) {
		return fetchProfiles.containsKey( name );
	}

	@Override
	public Set<String> getDefinedFetchProfileNames() {
		return unmodifiableSet( fetchProfiles.keySet() );
	}
}
