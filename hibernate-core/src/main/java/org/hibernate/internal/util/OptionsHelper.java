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
import org.hibernate.EnabledFetchProfile;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.ReadOnlyMode;
import org.hibernate.SessionCreationOption;
import org.hibernate.engine.creation.internal.options.StatefulOptions;
import org.hibernate.engine.creation.internal.options.StatelessOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.query.QueryOption;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.QueryOptions;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.jpa.internal.util.FlushModeTypeHelper.queryFlushModeFromFlushMode;

/// Support for the type-safe option contracts introduced by Jakarta Persistence 4.
///
/// @since 8.0
/// @author Gavin King
public final class OptionsHelper {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Session

	/// Applies a `Session` creation `option` to the `options` collector.
	public static void applyOption(StatefulOptions options, EntityManager.CreationOption option) {
		if ( option instanceof CacheMode cacheMode ) {
			options.initialCacheMode( cacheMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			options.cacheStoreMode( cacheStoreMode );
		}
		else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			options.cacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof FlushMode flushMode ) {
			options.flushMode( flushMode );
		}
		else if ( option instanceof FlushModeType flushModeType ) {
			options.flushMode( FlushMode.fromJpaFlushMode( flushModeType ) );
		}
		else if ( option instanceof BatchSize batchSize) {
			options.jdbcBatchSize( batchSize.batchSize() );
		}
		else if ( option instanceof ReadOnlyMode readOnlyMode) {
			options.readOnly( readOnlyMode == ReadOnlyMode.READ_ONLY );
		}
		else if ( option instanceof SessionCreationOption.SubselectFetchMode subselectFetchMode ) {
			options.subselectFetchEnabled( subselectFetchMode == SessionCreationOption.SubselectFetchMode.ENABLED );
		}
		else if ( option instanceof SessionCreationOption.TenantId tenantId ) {
			options.tenantIdentifier( tenantId.value() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveChangeset effectiveChangeset ) {
			options.atChangeset( effectiveChangeset.changesetId() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveAt effectiveAt ) {
			options.asOf( effectiveAt.instant() );
		}
	}


	/// Applies a runtime-adjustable `option` to the `Session`.
	public static void applyOption(SessionImplementor entityManager, EntityManager.Option option) {
		Objects.requireNonNull( option, "option" );

		if ( option instanceof CacheMode cacheMode ) {
			entityManager.setCacheMode( cacheMode );
		}
		else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
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
	}

	/// Collects all options from the `Session`.
	public static Set<EntityManager.Option> getOptions(SessionImplementor entityManager) {
		final Set<EntityManager.Option> options = new HashSet<>();
		options.add( entityManager.getCacheMode() );
		addIfNotNull( options, entityManager.getCacheRetrieveMode() );
		addIfNotNull( options, entityManager.getCacheStoreMode() );
		addIfNotNull( options, entityManager.getFlushMode() );
		addIfNotNull( options, entityManager.getHibernateFlushMode() );
		if ( entityManager.isDefaultReadOnly() ) {
			options.add( ReadOnlyMode.READ_ONLY );
		}
		if ( entityManager.getJdbcBatchSize() != null ) {
			options.add( new BatchSize( entityManager.getJdbcBatchSize() ) );
		}
		return options;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// StatelessSession

	/// Applies a `StatelessSession` creation `option` to the `options` collector.
	public static void applyOption(StatelessOptions options, EntityAgent.CreationOption option) {
		if ( option instanceof CacheMode cacheMode ) {
			options.initialCacheMode( cacheMode );
		}
		else if ( option instanceof CacheStoreMode cacheStoreMode ) {
			options.cacheStoreMode( cacheStoreMode );
		}
		else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
			options.cacheRetrieveMode( cacheRetrieveMode );
		}
		else if ( option instanceof BatchSize batchSize) {
			options.jdbcBatchSize( batchSize.batchSize() );
		}
		else if ( option instanceof SessionCreationOption.TenantId tenantId ) {
			options.tenantIdentifier( tenantId.value() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveChangeset effectiveChangeset ) {
			options.atChangeset( effectiveChangeset.changesetId() );
		}
		else if ( option instanceof SessionCreationOption.EffectiveAt effectiveAt ) {
			options.asOf( effectiveAt.instant() );
		}
	}

	/// Applies a runtime-adjustable `option` to the `StatelessSession`.
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

	/// Collects all options from the `StatelessSession`.
	public static Set<EntityAgent.Option> getOptions(StatelessSessionImplementor entityAgent) {
		final Set<EntityAgent.Option> options = new HashSet<>();
		options.add( entityAgent.getCacheMode() );
		addIfNotNull( options, entityAgent.getCacheRetrieveMode() );
		addIfNotNull( options, entityAgent.getCacheStoreMode() );
		if ( entityAgent.getJdbcBatchSize() != null ) {
			options.add( new BatchSize( entityAgent.getJdbcBatchSize() ) );
		}
		return options;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query

	public static void applyOption(TypedQuery<?> query, TypedQuery.Option option) {
		Objects.requireNonNull( option, "option" );
		final var selectionQuery = (SelectionQuery<?>) query;
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
		else if ( option instanceof CacheMode cacheMode ) {
			selectionQuery.setCacheMode( cacheMode );
		}
		else if ( option instanceof ReadOnlyMode readOnlyMode ) {
			selectionQuery.setReadOnly( readOnlyMode == ReadOnlyMode.READ_ONLY );
		}
		else if ( option instanceof QueryOption.ResultSetCache resultSetCache ) {
			selectionQuery.setCacheable( true );
			selectionQuery.setCacheRegion( resultSetCache.region() );
		}
		else if ( option instanceof QueryOption.Comment comment ) {
			selectionQuery.setComment( comment.comment() );
		}
		else if ( option instanceof EnabledFetchProfile fetchProfile ) {
			fetchProfile.enable( selectionQuery );
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
		addCustomOptions( queryOptions, options );
		addLockOptions( options, queryOptions.getLockOptions() );
		return options;
	}

	private static void addCustomOptions(QueryOptions queryOptions, Set<TypedQuery.Option> options) {
		final Boolean readOnly = queryOptions.isReadOnly();
		if ( readOnly != null ) {
			options.add( readOnly ? ReadOnlyMode.READ_ONLY : ReadOnlyMode.READ_WRITE );
		}

		final Boolean resultCachingEnabled = queryOptions.isResultCachingEnabled();
		if ( resultCachingEnabled == Boolean.TRUE ) {
			options.add( new QueryOption.ResultSetCache( queryOptions.getResultCacheRegionName() ) );
		}

		final String comment = queryOptions.getComment();
		if ( comment != null ) {
			options.add( new QueryOption.Comment( comment ) );
		}

		final var enabledFetchProfiles = queryOptions.getEnabledFetchProfiles();
		if ( enabledFetchProfiles != null ) {
			for ( String fetchProfile : enabledFetchProfiles ) {
				options.add( new EnabledFetchProfile( fetchProfile ) );
			}
		}
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
