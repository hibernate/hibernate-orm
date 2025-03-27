/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Standard MultiNaturalIdLoader implementation
 */
public class MultiNaturalIdLoaderArrayParam<E> extends AbstractMultiNaturalIdLoader<E> implements SqlArrayMultiKeyLoader {
	private final Class<?> keyClass;

	public MultiNaturalIdLoaderArrayParam(EntityMappingType entityDescriptor) {
		super(entityDescriptor);

		assert entityDescriptor.getNaturalIdMapping() instanceof SimpleNaturalIdMapping;

		this.keyClass = entityDescriptor.getNaturalIdMapping().getJavaType().getJavaTypeClass();
	}

	protected SimpleNaturalIdMapping getNaturalIdMapping()  {
		return (SimpleNaturalIdMapping) getEntityDescriptor().getNaturalIdMapping();
	}

	protected BasicAttributeMapping getNaturalIdAttribute()  {
		return (BasicAttributeMapping) getNaturalIdMapping().asAttributeMapping();
	}

	@Override
	public List<E> loadEntitiesWithUnresolvedIds(Object[] naturalIds, SharedSessionContractImplementor session, LockOptions lockOptions) {

		final SessionFactoryImplementor sessionFactory = session.getFactory();

		naturalIds = LoaderHelper.normalizeKeys( naturalIds, getNaturalIdAttribute(), session, sessionFactory );

		final JdbcMapping arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				getNaturalIdMapping().getSingleJdbcMapping(),
				keyClass,
				sessionFactory
		);
		final JdbcParameter jdbcParameter = new JdbcParameterImpl( arrayJdbcMapping );

		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				getNaturalIdAttribute(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				sessionFactory
		);
		final JdbcOperationQuerySelect jdbcSelectOperation = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, new QueryOptionsAdapter() {
					@Override
					public LockOptions getLockOptions() {
						return lockOptions;
					}
				} );

		return LoaderHelper.loadByArrayParameter(
					naturalIds,
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
