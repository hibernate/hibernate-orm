/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import static org.hibernate.query.internal.AbstractSqmQuery.CRITERIA_HQL_STRING;

public class CriteriaSelectionMementoImpl<R>
		extends AbstractSelectionMemento<R>
		implements SqmSelectionMemento<R>, Serializable {

	private final SqmSelectStatement<R> selectAst;
	private final String entityGraphName;
	private final Map<String, String> parameterTypes;

	public CriteriaSelectionMementoImpl(
			String name,
			@Nullable Class<R> resultType,
			String entityGraphName,
			SqmStatement<R> sqmStatement,
			Integer firstResult,
			Integer maxResults,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockMode lockMode,
			PessimisticLockScope lockScope,
			Timeout lockTimeout,
			Locking.FollowOn followOnLockingStrategy,
			Timeout timeout,
			Integer fetchSize,
			String comment,
			Map<String, String> parameterTypes,
			Map<String, Object> hints) {
		super( name, resultType,
				flushMode, timeout, comment,
				readOnly, fetchSize, firstResult, maxResults,
				cacheable, cacheMode, cacheRegion,
				lockMode, lockScope, lockTimeout, followOnLockingStrategy,
				hints );
		this.selectAst = SqmUtil.asSelectStatement( sqmStatement, CRITERIA_HQL_STRING );
		this.entityGraphName = StringHelper.nullIfEmpty( entityGraphName );
		this.parameterTypes = parameterTypes;
	}

	public CriteriaSelectionMementoImpl(String name, CriteriaSelectionMementoImpl<R> original) {
		super( name, original );
		this.selectAst = original.selectAst;
		this.entityGraphName = original.entityGraphName;
		this.parameterTypes = original.parameterTypes;
	}

	@Override
	public String getHqlString() {
		return CRITERIA_HQL_STRING;
	}

	@Override
	public String getSelectionString() {
		return getHqlString();
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return selectAst;
	}

	@Override
	public Map<String, String> getAnticipatedParameterTypes() {
		return parameterTypes;
	}

	@Override
	public String getEntityGraphName() {
		return null;
	}

	@Override
	public NamedSqmQueryMemento<R> makeCopy(String name) {
		return new CriteriaSelectionMementoImpl<>( name, this );
	}

	@Override
	public void validate(QueryEngine queryEngine) {
		// nothing to do
	}

	@Override
	public SelectionQueryImplementor<R> toSelectionQuery(SharedSessionContractImplementor session) {
		return toSelectionQuery( session, queryType );
	}

	@Override
	public <T> SelectionQueryImplementor<T> toSelectionQuery(SharedSessionContractImplementor session, Class<T> javaType) {
		checkResultType( javaType, selectAst );
		//noinspection rawtypes,unchecked
		return new SelectionQueryImpl( this, selectAst, javaType, session );
	}

	private static <T> void checkResultType(Class<T> resultType, SqmSelectStatement<?> selectStatement) {
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
