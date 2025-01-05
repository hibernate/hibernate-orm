/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.type.spi.TypeConfiguration;

public class SqlTranslationEngineImpl implements SqlTranslationEngine {

	//TODO: consider unifying with SqlStringGenerationContextImpl

	private final SessionFactoryImplementor factory;
	private final TypeConfiguration typeConfiguration;

	public SqlTranslationEngineImpl(SessionFactoryImplementor factory, TypeConfiguration typeConfiguration) {
		this.factory = factory;
		this.typeConfiguration = typeConfiguration;
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
		return factory.getFetchProfile( name );
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}
}
