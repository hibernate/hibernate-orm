/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.spi.SqmStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import static org.hibernate.query.internal.AbstractSqmQuery.CRITERIA_HQL_STRING;

public class CriteriaSelectionMementoImpl<R>
		extends AbstractSelectionMemento<R>
		implements SqmSelectionMemento<R>, Serializable {

	private final SqmSelectStatement<R> selectAst;
	private final @Nullable String entityGraphName;
	private final @Nullable Map<String, String> parameterTypes;

	public CriteriaSelectionMementoImpl(
			@Nonnull String name,
			@Nullable Class<R> resultType,
			@Nullable String entityGraphName,
			@Nonnull SqmStatement<R> sqmStatement,
			@Nullable Integer firstResult,
			@Nullable Integer maxResults,
			@Nullable Boolean cacheable,
			@Nullable String cacheRegion,
			@Nullable CacheMode cacheMode,
			@Nullable QueryFlushMode queryFlushMode,
			@Nullable Boolean readOnly,
			@Nullable LockMode lockMode,
			@Nullable PessimisticLockScope lockScope,
			@Nullable Timeout lockTimeout,
			@Nullable Locking.FollowOn followOnLockingStrategy,
			@Nullable Timeout timeout,
			@Nullable Integer fetchSize,
			@Nullable String comment,
			@Nullable Map<String, String> parameterTypes,
			@Nonnull Map<String, Object> hints) {
		super( name, resultType,
				queryFlushMode, timeout, comment,
				readOnly, fetchSize, firstResult, maxResults,
				cacheable, cacheMode, cacheRegion,
				lockMode, lockScope, lockTimeout, followOnLockingStrategy,
				hints );
		this.selectAst = SqmUtil.asSelectStatement( sqmStatement, CRITERIA_HQL_STRING );
		this.entityGraphName = StringHelper.nullIfEmpty( entityGraphName );
		this.parameterTypes = parameterTypes;
	}

	public CriteriaSelectionMementoImpl(@Nonnull String name, @Nonnull CriteriaSelectionMementoImpl<R> original) {
		super( name, original );
		this.selectAst = original.selectAst;
		this.entityGraphName = original.entityGraphName;
		this.parameterTypes = original.parameterTypes;
	}

	@Nonnull
	@Override
	public String getHqlString() {
		return CRITERIA_HQL_STRING;
	}

	@Nonnull
	@Override
	public String getSelectionString() {
		return getHqlString();
	}

	@Nonnull
	@Override
	public SqmStatement<R> getSqmStatement() {
		return selectAst;
	}

	@Nullable
	@Override
	public Map<String, String> getAnticipatedParameterTypes() {
		return parameterTypes;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return null;
	}

	@Nonnull
	@Override
	public NamedSqmQueryMemento<R> makeCopy(@Nonnull String name) {
		return new CriteriaSelectionMementoImpl<>( name, this );
	}

	@Override
	public void validate(@Nonnull QueryEngine queryEngine) {
		// nothing to do
	}

	@Nonnull
	@Override
	public SelectionQueryImplementor<R> toSelectionQuery(@Nonnull SharedSessionContractImplementor session) {
		return toSelectionQuery( session, queryType );
	}

	@Nonnull
	@Override
	public <T> SelectionQueryImplementor<T> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<T> javaType) {
		checkResultType( javaType, selectAst );
		//noinspection rawtypes,unchecked
		return new SelectionQueryImpl( this, selectAst, javaType, session );
	}

	private static <T> void checkResultType(
			@Nullable Class<T> resultType,
			@Nonnull SqmSelectStatement<?> selectStatement) {
		if ( resultType == null ) {
			return;
		}
		final Class<?> expectedResultType = selectStatement.getResultType();
		if ( expectedResultType != Object.class
			&& !resultType.isAssignableFrom( expectedResultType ) ) {
			throw new QueryTypeMismatchException(
					String.format(
							Locale.ROOT,
							"Incorrect query result type: query produces '%s' but type '%s' was given",
							expectedResultType.getName(),
							resultType.getName()
					)
			);
		}
	}

}
