/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.spi.NamedSelectionQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.named.internal.HqlSelectionMementoImpl;

import java.util.Map;

/// Support for [jakarta.persistence.NamedQuery] and
/// [org.hibernate.annotations.NamedQuery] definitions.
///
/// @author Steve Ebersole
public class NamedHqlSelectionDefinitionImpl<R>
		extends AbstractNamedSelectionDefinition<R>
		implements NamedHqlQueryDefinition<R>, NamedSelectionQueryDefinition<R> {
	private final String hqlString;
	private final Class<R> resultType;
	private final String entityGraphName;
	private final Map<String, String> parameterTypes;

	public NamedHqlSelectionDefinitionImpl(
			String name,
			@Nullable String location,
			String hqlString,
			@Nullable Class<R> resultType,
			String entityGraphName,
			FlushMode flushMode,
			Timeout timeout,
			String comment,
			Boolean readOnly,
			Integer fetchSize,
			Integer firstResult,
			Integer maxResults,
			Boolean cacheable,
			CacheMode cacheMode,
			String cacheRegion,
			LockMode lockMode,
			PessimisticLockScope lockScope,
			Timeout lockTimeout,
			Locking.FollowOn followOnLockingStrategy,
			Map<String,String> parameterTypes,
			Map<String,Object> hints) {
		super(
				name,
				location,
				flushMode,
				timeout,
				comment,
				readOnly,
				fetchSize,
				firstResult,
				maxResults,
				cacheable,
				cacheRegion,
				cacheMode,
				lockMode,
				lockScope,
				lockTimeout,
				followOnLockingStrategy,
				hints
		);
		this.hqlString = hqlString;
		this.resultType = resultType;
		this.entityGraphName = StringHelper.nullIfEmpty( entityGraphName );
		this.parameterTypes = parameterTypes;
	}

	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Override
	public String getQueryString() {
		return getHqlString();
	}

	@Override
	public @Nullable Class<R> getResultType() {
		return resultType;
	}

	@Override
	public String getEntityGraphName() {
		return entityGraphName;
	}

	@Override
	public NamedSqmQueryMemento<R> resolve(SessionFactoryImplementor factory) {
		return new HqlSelectionMementoImpl<>(
				getRegistrationName(), hqlString,
				getResultType(), entityGraphName,
				flushMode, timeout, comment, readOnly,
				fetchSize, firstRow, maxRows,
				cacheable, cacheMode, cacheRegion,
				lockMode, lockScope, lockTimeout, followOnLockingStrategy,
				parameterTypes,
				hints
		);
	}

	/// Build a definition from Hibernate's [NamedQuery] annotation.
	///
	/// @param annotation The annotation.
	/// @param location Where the annotation was found.
	public static NamedHqlSelectionDefinitionImpl<?> from(NamedQuery annotation, AnnotationTarget location) {
		//noinspection rawtypes,unchecked
		return new NamedHqlSelectionDefinitionImpl(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				null,
				resolveFlushMode( annotation.flush(), annotation.flushMode() ),
				cleanTimeout( annotation.timeout() ),
				annotation.comment(),
				annotation.readOnly(),
				annotation.fetchSize(),
				null,
				null,
				annotation.cacheable(),
				CacheMode.resolve( annotation.cacheMode(), annotation.cacheRetrieveMode(), annotation.cacheStoreMode() ),
				annotation.cacheRegion(),
				null,
				null,
				null,
				null,
				Map.of(),
				Map.of()
		);
	}

	private static FlushMode resolveFlushMode(QueryFlushMode queryFlushMode, FlushModeType flushModeType) {
		if ( queryFlushMode == QueryFlushMode.DEFAULT ) {
			if ( flushModeType != FlushModeType.PERSISTENCE_CONTEXT ) {
				return flushModeType.toFlushMode();
			}
			return null;
		}
		return queryFlushMode == QueryFlushMode.FLUSH
				? FlushMode.ALWAYS
				: FlushMode.MANUAL;
	}

	/// Build a definition from Hibernate's [HQLSelect] annotation.
	///
	/// @param name The name to use.
	/// @param annotation The annotation.
	/// @param location Location where the annotation was found.
	public static NamedHqlSelectionDefinitionImpl<?> from(String name, HQLSelect annotation, AnnotationTarget location) {
		return new NamedHqlSelectionDefinitionImpl<>(
				name,
				location == null ? null : location.getName(),
				annotation.query(),
				null,
				null,
				FlushMode.MANUAL,
				Timeouts.WAIT_FOREVER,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				Map.of(),
				Map.of()
		);
	}

	/// Build a definition from JPA's [jakarta.persistence.NamedQuery] annotation.
	///
	/// @param annotation The annotation.
	/// @param location Where the annotation was found.
	public static NamedHqlSelectionDefinitionImpl<?> from(jakarta.persistence.NamedQuery annotation, AnnotationTarget location) {
		//noinspection rawtypes,unchecked
		return new NamedHqlSelectionDefinitionImpl(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				LockMode.fromJpaLockMode( annotation.lockMode() ),
				annotation.lockScope(),
				null,
				null,
				Map.of(),
				Helper.extractHints( annotation.hints() )
		);
	}
}
