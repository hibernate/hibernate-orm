/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Statement;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import org.hibernate.LockOptions;
import org.hibernate.query.spi.QueryOptions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static org.hibernate.jpa.internal.util.FlushModeTypeHelper.queryFlushModeFromFlushMode;

/**
 * Support for the type-safe option contracts introduced by Jakarta Persistence 4.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
public final class OptionsHelper {

	public static void applyOption(EntityManager entityManager, EntityManager.Option option) {
		Objects.requireNonNull( option, "option" );
		if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			entityManager.setCacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			entityManager.setCacheStoreMode( cacheStoreMode );
		}
		else if ( option instanceof FlushModeType flushModeType ) {
			entityManager.setFlushMode( flushModeType );
		}
	}

	public static Set<EntityManager.Option> getOptions(EntityManager entityManager) {
		final Set<EntityManager.Option> options = new HashSet<>();
		addIfNotNull( options, entityManager.getCacheRetrieveMode() );
		addIfNotNull( options, entityManager.getCacheStoreMode() );
		addIfNotNull( options, entityManager.getFlushMode() );
		return options;
	}

	public static void applyOption(EntityAgent entityAgent, EntityAgent.Option option) {
		Objects.requireNonNull( option, "option" );
		if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			entityAgent.setCacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			entityAgent.setCacheStoreMode( cacheStoreMode );
		}
	}

	public static Set<EntityAgent.Option> getOptions(EntityAgent entityAgent) {
		final Set<EntityAgent.Option> options = new HashSet<>();
		addIfNotNull( options, entityAgent.getCacheRetrieveMode() );
		addIfNotNull( options, entityAgent.getCacheStoreMode() );
		return unmodifiableSet( options );
	}

	public static void applyOption(TypedQuery<?> query, TypedQuery.Option option) {
		Objects.requireNonNull( option, "option" );
		if ( option instanceof Timeout timeout ) {
			query.setTimeout( timeout );
		}
		else if ( option instanceof QueryFlushMode queryFlushMode ) {
			query.setQueryFlushMode( queryFlushMode );
		}
		else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			query.setCacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			query.setCacheStoreMode( cacheStoreMode );
		}
		else if ( option instanceof LockModeType lockModeType ) {
			query.setLockMode( lockModeType );
		}
		else if ( option instanceof PessimisticLockScope lockScope ) {
			query.setLockScope( lockScope );
		}
	}

	public static Set<TypedQuery.Option> getTypedQueryOptions(QueryOptions queryOptions) {
		final Set<TypedQuery.Option> options = new HashSet<>();
		addIfNotNull( options, queryOptions.getTimeout() );
		final var queryFlushMode = queryFlushModeFromFlushMode( queryOptions.getFlushMode() );
		if ( queryFlushMode != null && queryFlushMode != QueryFlushMode.DEFAULT ) {
			options.add( queryFlushMode );
		}
		addIfNotNull( options, queryOptions.getCacheRetrieveMode() );
		addIfNotNull( options, queryOptions.getCacheStoreMode() );
		addLockOptions( options, queryOptions.getLockOptions() );
		return options;
	}

	public static void applyOption(Statement statement, Statement.Option option) {
		Objects.requireNonNull( option, "option" );
		if ( option instanceof Timeout timeout ) {
			statement.setTimeout( timeout );
		}
		else if ( option instanceof QueryFlushMode queryFlushMode ) {
			statement.setQueryFlushMode( queryFlushMode );
		}
	}

	public static Set<Statement.Option> getStatementOptions(QueryOptions queryOptions) {
		final Set<Statement.Option> options = new HashSet<>();
		addIfNotNull( options, queryOptions.getTimeout() );
		final var queryFlushMode = queryFlushModeFromFlushMode( queryOptions.getFlushMode() );
		if ( queryFlushMode != null && queryFlushMode != QueryFlushMode.DEFAULT ) {
			options.add( queryFlushMode );
		}
		return options;
	}

	public static void applyOption(StoredProcedureQuery query, StoredProcedureQuery.Option option) {
		Objects.requireNonNull( option, "option" );
		if ( option instanceof Timeout timeout ) {
			query.setTimeout( timeout );
		}
		else if ( option instanceof QueryFlushMode queryFlushMode ) {
			query.setQueryFlushMode( queryFlushMode );
		}
	}

	public static Set<StoredProcedureQuery.Option> getStoredProcedureOptions(QueryOptions queryOptions) {
		final Set<StoredProcedureQuery.Option> options = new HashSet<>();
		addIfNotNull( options, queryOptions.getTimeout() );
		final var queryFlushMode = queryFlushModeFromFlushMode( queryOptions.getFlushMode() );
		if ( queryFlushMode != null && queryFlushMode != QueryFlushMode.DEFAULT ) {
			options.add( queryFlushMode );
		}
		return unmodifiableSet( options );
	}

	private static void addLockOptions(Set<TypedQuery.Option> options, LockOptions lockOptions) {
		if ( lockOptions != null ) {
			final var lockMode = lockOptions.getLockMode();
			final var lockModeType = lockMode == null ? null : lockMode.toJpaLockMode();
			if ( lockModeType != null && lockModeType != LockModeType.NONE ) {
				options.add( lockModeType );
			}
			final var lockScope = lockOptions.getLockScope();
			if ( lockScope != null && lockScope != PessimisticLockScope.NORMAL ) {
				options.add( lockScope );
			}
		}
	}

	private static <O> void addIfNotNull(Set<O> options, O option) {
		if ( option != null ) {
			options.add( option );
		}
	}
}
