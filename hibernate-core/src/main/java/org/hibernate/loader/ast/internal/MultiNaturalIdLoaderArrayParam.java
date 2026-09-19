/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.loader.ast.internal.LoaderHelper.loadByArrayParameter;
import static org.hibernate.loader.ast.internal.LoaderHelper.normalizeKeys;

/**
 * Standard MultiNaturalIdLoader implementation
 */
public class MultiNaturalIdLoaderArrayParam<E> extends AbstractMultiNaturalIdLoader<E> implements SqlArrayMultiKeyLoader {
	private final Class<?> keyClass;

	public MultiNaturalIdLoaderArrayParam(@Nonnull EntityMappingType entityDescriptor) {
		super(entityDescriptor);
		assert entityDescriptor.getNaturalIdMapping() instanceof SimpleNaturalIdMapping;
		keyClass = entityDescriptor.getNaturalIdMapping().getJavaType().getJavaTypeClass();
	}

	@Nonnull
	protected SimpleNaturalIdMapping getNaturalIdMapping()  {
		return (SimpleNaturalIdMapping) castNonNull( getEntityDescriptor().getNaturalIdMapping() );
	}

	@Nonnull
	protected BasicAttributeMapping getNaturalIdAttribute()  {
		return (BasicAttributeMapping) castNonNull( getNaturalIdMapping().asAttributeMapping() );
	}

	@Nonnull
	@Override
	public List<E> loadEntitiesWithUnresolvedIds(
			@Nonnull Object[] naturalIds,
			@Nonnull MultiNaturalIdLoadOptions loadOptions,
			@Nonnull LockOptions lockOptions,
			@Nonnull SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		final var selectable = getNaturalIdAttribute().getSelectable( 0 );
		final JdbcMapping jdbcMapping = selectable.getJdbcMapping();
		final SqlTypedMapping arraySqlTypedMapping = new SqlTypedMappingImpl(
				selectable.getLength(),
				selectable.getArrayLength(),
				selectable.getPrecision(),
				selectable.getScale(),
				selectable.getTemporalPrecision(),
				MultiKeyLoadHelper.resolveArrayJdbcMapping(
						jdbcMapping,
						jdbcMapping.getJdbcJavaType().getJavaTypeClass(),
						factory
				)
		);
		final var jdbcParameter = new SqlTypedMappingJdbcParameter( arraySqlTypedMapping );
		final var sqlAst = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				getNaturalIdAttribute(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				new SqlAliasBaseManager(),
				factory
		);
		final var jdbcSelectOperation =
				factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.Select( factory, sqlAst ) )
						.translate( JdbcParameterBindings.NO_BINDINGS, new QueryOptionsAdapter() {
							@Override
							@Nonnull
							public LockOptions getLockOptions() {
								return lockOptions;
							}
						} );

		return loadByArrayParameter(
				normalizeKeys( naturalIds, getNaturalIdAttribute(), session, factory ),
				sqlAst,
				jdbcSelectOperation,
				jdbcParameter,
				arraySqlTypedMapping.getJdbcMapping(),
				null,
				null,
				null,
				lockOptions,
				session.isDefaultReadOnly(),
				session
		);
	}

}
