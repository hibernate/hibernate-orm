/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;

/**
 * Standard MultiNaturalIdLoader implementation
 */
public class MultiNaturalIdLoaderArrayParam<E> implements MultiNaturalIdLoader<E>, SqlArrayMultiKeyLoader {
	private final EntityMappingType entityDescriptor;
	private final Class<?> keyArrayClass;

	public MultiNaturalIdLoaderArrayParam(EntityMappingType entityDescriptor) {
		assert entityDescriptor.getNaturalIdMapping() instanceof SimpleNaturalIdMapping;

		this.entityDescriptor = entityDescriptor;

		final Class<?> keyClass = entityDescriptor.getNaturalIdMapping().getJavaType().getJavaTypeClass();
		this.keyArrayClass = LoaderHelper.createTypedArray( keyClass, 0 ).getClass();
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	protected SimpleNaturalIdMapping getNaturalIdMapping()  {
		return (SimpleNaturalIdMapping) entityDescriptor.getNaturalIdMapping();
	}

	protected BasicAttributeMapping getNaturalIdAttribute()  {
		return (BasicAttributeMapping) getNaturalIdMapping().asAttributeMapping();
	}

	@Override
	public <K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions loadOptions, SharedSessionContractImplementor session) {
		if ( naturalIds == null ) {
			throw new IllegalArgumentException( "`naturalIds` is null" );
		}

		if ( naturalIds.length == 0 ) {
			return Collections.emptyList();
		}

		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef( "MultiNaturalIdLoaderArrayParam#multiLoadStarting - `%s`", entityDescriptor.getEntityName() );
		}

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		naturalIds = LoaderHelper.normalizeKeys( naturalIds, getNaturalIdAttribute(), session, sessionFactory );

		final LockOptions lockOptions = (loadOptions.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		final BasicTypeRegistry basicTypeRegistry = sessionFactory.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<?> arrayBasicType = basicTypeRegistry.getRegisteredType( keyArrayClass );
		final JdbcMapping arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				arrayBasicType,
				getNaturalIdMapping().getSingleJdbcMapping(),
				keyArrayClass,
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
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );

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
