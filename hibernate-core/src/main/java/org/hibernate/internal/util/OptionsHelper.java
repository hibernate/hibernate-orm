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
import org.hibernate.BatchSize;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.ReadOnlyMode;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionCreationOption;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.query.spi.QueryOptions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.jpa.internal.util.FlushModeTypeHelper.queryFlushModeFromFlushMode;

/**
 * Support for the type-safe option contracts introduced by Jakarta Persistence 4.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
public final class OptionsHelper {
	public static void applyOption(SessionBuilder builder, EntityManager.CreationOption option) {
		if ( option instanceof CacheMode cacheMode ) {
			builder.initialCacheMode( cacheMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			builder.cacheStoreMode( cacheStoreMode );
		}
		else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			builder.cacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof FlushMode flushMode ) {
			builder.flushMode( flushMode );
		}
		else if ( option instanceof FlushModeType flushModeType ) {
			builder.flushMode( FlushMode.fromJpaFlushMode(  flushModeType ) );
		}
		else if ( option instanceof BatchSize batchSize) {
			builder.jdbcBatchSize( batchSize.batchSize() );
		}
		else if ( option instanceof ReadOnlyMode readOnlyMode) {
			builder.readOnly( readOnlyMode == ReadOnlyMode.READ_ONLY );
		}
		else if ( option instanceof SessionCreationOption.SubselectFetchMode subselectFetchMode ) {
			builder.subselectFetchEnabled( subselectFetchMode == SessionCreationOption.SubselectFetchMode.ENABLED );
		}
		else if ( option instanceof SessionCreationOption.TenantId tenantId ) {
			builder.tenantIdentifier( tenantId.value() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveChangeset effectiveChangeset ) {
			builder.atChangeset( effectiveChangeset.changesetId() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveAt effectiveAt ) {
			builder.asOf( effectiveAt.instant() );
		}
	}

	public static void applyOption(StatelessSessionBuilder builder, EntityAgent.CreationOption option) {
		if ( option instanceof CacheMode cacheMode ) {
			builder.initialCacheMode( cacheMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			builder.cacheStoreMode( cacheStoreMode );
		}
		else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			builder.cacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof BatchSize batchSize) {
			builder.jdbcBatchSize( batchSize.batchSize() );
		}
		else if ( option instanceof SessionCreationOption.TenantId tenantId ) {
			builder.tenantIdentifier( tenantId.value() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveChangeset effectiveChangeset ) {
			builder.atChangeset( effectiveChangeset.changesetId() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveAt effectiveAt ) {
			builder.asOf( effectiveAt.instant() );
		}

	}

	public static void applyOption(SessionImplementor entityManager, EntityManager.Option option) {
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
		else if ( option instanceof ReadOnlyMode readOnlyMode ) {
			entityManager.setDefaultReadOnly( readOnlyMode == ReadOnlyMode.READ_ONLY );
		}
		else if ( option instanceof BatchSize batchSize ) {
			entityManager.setJdbcBatchSize( batchSize.batchSize() );
		}
		else if ( option instanceof CacheMode cacheMode ) {
			entityManager.setCacheMode( cacheMode );
		}
	}

	public static Set<EntityManager.Option> getOptions(SessionImplementor entityManager) {
		final Set<EntityManager.Option> options = new HashSet<>();
		addIfNotNull( options, entityManager.getCacheRetrieveMode() );
		addIfNotNull( options, entityManager.getCacheStoreMode() );
		addIfNotNull( options, entityManager.getFlushMode() );
		if ( entityManager.isDefaultReadOnly() ) {
			options.add( ReadOnlyMode.READ_ONLY );
		}
		if ( entityManager.getJdbcBatchSize() != null ) {
			options.add( new BatchSize( entityManager.getJdbcBatchSize() ) );
		}
		options.add( entityManager.getCacheMode() );
		return options;
	}

	public static void applyOption(StatelessSessionImplementor entityAgent, EntityAgent.Option option) {
		Objects.requireNonNull( option, "option" );
		if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			entityAgent.setCacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			entityAgent.setCacheStoreMode( cacheStoreMode );
		}
		else if ( option instanceof BatchSize batchSize ) {
			entityAgent.setJdbcBatchSize( batchSize.batchSize() );
		}
		else if ( option instanceof CacheMode cacheMode ) {
			entityAgent.setCacheMode( cacheMode );
		}
	}

	public static Set<EntityAgent.Option> getOptions(StatelessSessionImplementor entityAgent) {
		final Set<EntityAgent.Option> options = new HashSet<>();
		addIfNotNull( options, entityAgent.getCacheRetrieveMode() );
		addIfNotNull( options, entityAgent.getCacheStoreMode() );
		if ( entityAgent.getJdbcBatchSize() != null ) {
			options.add( new BatchSize( entityAgent.getJdbcBatchSize() ) );
		}
		options.add( entityAgent.getCacheMode() );
		return options;
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
		return options;
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
