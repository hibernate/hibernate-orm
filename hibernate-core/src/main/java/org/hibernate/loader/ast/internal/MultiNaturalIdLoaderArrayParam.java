/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static org.hibernate.loader.ast.internal.LoaderHelper.loadByArrayParameter;
import static org.hibernate.loader.ast.internal.LoaderHelper.normalizeKeys;

/**
 * Standard MultiNaturalIdLoader implementation
 */
public class MultiNaturalIdLoaderArrayParam<E> extends AbstractMultiNaturalIdLoader<E> implements SqlArrayMultiKeyLoader {
	private final Class<?> keyClass;

	public MultiNaturalIdLoaderArrayParam(EntityMappingType entityDescriptor) {
		super(entityDescriptor);
		assert entityDescriptor.getNaturalIdMapping() instanceof SimpleNaturalIdMapping;
		keyClass = entityDescriptor.getNaturalIdMapping().getJavaType().getJavaTypeClass();
	}

	protected SimpleNaturalIdMapping getNaturalIdMapping()  {
		return (SimpleNaturalIdMapping) getEntityDescriptor().getNaturalIdMapping();
	}

	protected BasicAttributeMapping getNaturalIdAttribute()  {
		return (BasicAttributeMapping) getNaturalIdMapping().asAttributeMapping();
	}

	@Override
	public List<E> loadEntitiesWithUnresolvedIds(
			Object[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		final var arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				getNaturalIdMapping().getSingleJdbcMapping(),
				keyClass,
				factory
		);
		final var jdbcParameter = new JdbcParameterImpl( arrayJdbcMapping );
		final var sqlAst = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				getNaturalIdAttribute(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				factory
		);
		final var jdbcSelectOperation =
				factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( factory, sqlAst )
						.translate( JdbcParameterBindings.NO_BINDINGS, new QueryOptionsAdapter() {
							@Override
							public LockOptions getLockOptions() {
								return lockOptions;
							}
						} );

		return loadByArrayParameter(
				normalizeKeys( naturalIds, getNaturalIdAttribute(), session, factory ),
				sqlAst,
				jdbcSelectOperation,
				jdbcParameter,
				arrayJdbcMapping,
				null,
				null,
				null,
				lockOptions,
				session.isDefaultReadOnly(),
				session
		);
	}

}
