/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.spi.NamedSelectionQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.models.spi.AnnotationTarget;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
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
			@Nonnull String name,
			@Nullable String location,
			@Nonnull String hqlString,
			@Nullable Class<R> resultType,
			@Nullable String entityGraphName,
			@Nullable FlushMode flushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nullable Boolean readOnly,
			@Nullable Integer fetchSize,
			@Nullable Integer firstResult,
			@Nullable Integer maxResults,
			@Nullable Boolean cacheable,
			@Nullable CacheMode cacheMode,
			@Nullable String cacheRegion,
			@Nullable LockMode lockMode,
			@Nullable PessimisticLockScope lockScope,
			@Nullable Timeout lockTimeout,
			@Nullable Locking.FollowOn followOnLockingStrategy,
			@Nullable Map<String,String> parameterTypes,
			@Nonnull Map<String,Object> hints) {
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

	@Nonnull
	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Nonnull
	@Override
	public String getQueryString() {
		return getHqlString();
	}

	@Override
	@Nullable
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return entityGraphName;
	}

	@Nonnull
	@Override
	public NamedSqmQueryMemento<R> resolve(@Nonnull SessionFactoryImplementor factory) {
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
	public static NamedHqlSelectionDefinitionImpl<?> from(
			@Nonnull NamedQuery annotation,
			@Nullable AnnotationTarget location) {
		//noinspection rawtypes,unchecked
		return new NamedHqlSelectionDefinitionImpl(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				null,
				resolveFlushMode( annotation.flush() ),
				cleanTimeout( annotation.timeout() ),
				annotation.comment(),
				annotation.readOnly(),
				annotation.fetchSize(),
				null,
				null,
				annotation.cacheable(),
				CacheMode.resolve( annotation.cacheMode(),
						annotation.cacheRetrieveMode(),
						annotation.cacheStoreMode() ),
				annotation.cacheRegion(),
				null,
				null,
				null,
				null,
				Map.of(),
				Map.of()
		);
	}

	@Nullable
	private static FlushMode resolveFlushMode(@Nonnull QueryFlushMode queryFlushMode) {
		if ( queryFlushMode == QueryFlushMode.DEFAULT ) {
			return null;
		}
		else {
			return queryFlushMode == QueryFlushMode.FLUSH
					? FlushMode.ALWAYS
					: FlushMode.MANUAL;
		}
	}

	/// Build a definition from Hibernate's [HQLSelect] annotation.
	///
	/// @param name The name to use.
	/// @param annotation The annotation.
	/// @param location Location where the annotation was found.
	@Nonnull
	public static NamedHqlSelectionDefinitionImpl<?> from(
			@Nonnull String name, HQLSelect annotation,
			@Nullable AnnotationTarget location) {
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
	@Nonnull
	public static NamedHqlSelectionDefinitionImpl<?> from(
			@Nonnull jakarta.persistence.NamedQuery annotation,
			@Nullable AnnotationTarget location) {
		//noinspection rawtypes,unchecked
		return new NamedHqlSelectionDefinitionImpl(
				annotation.name(),
				location == null ? null : location.getName(),
				annotation.query(),
				annotation.resultClass() == void.class ? null : annotation.resultClass(),
				annotation.entityGraph(),
				FlushModeTypeHelper.getFlushMode( annotation.flush() ),
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
