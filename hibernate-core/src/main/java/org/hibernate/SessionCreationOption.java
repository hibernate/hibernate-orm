/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityAgent;
import org.hibernate.engine.creation.CommonBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/// Declares various for [session][EntityManager.CreationOption] and
/// [stateless session][EntityAgent.CreationOption] creation options.
///
/// @apiNote Serves no functional purpose - a place to consolidate
///          the actual options for easier discovery / documentation.
///
/// @see ReadOnlyMode
/// @see FlushMode
/// @see CacheMode
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public interface SessionCreationOption {
	/// Specifies the [tenant id][org.hibernate.annotations.TenantId] which should
	/// be used when accessing a multi-tenant database.
	///
	/// @see SharedSessionContract#getTenantIdentifier()
	/// @see CommonBuilder#tenantIdentifier(Object)
	record TenantId(Object value)
			implements EntityManager.CreationOption, EntityAgent.CreationOption {
	}

	/// Specify the
	/// [changeset id][org.hibernate.cfg.StateManagementSettings#CHANGESET_ID_SUPPLIER]
	/// for reading [temporal][org.hibernate.annotations.Temporal] or
	/// [audited][org.hibernate.annotations.Audited] entity data. Instances of
	/// temporal or audited entities retrieved in the created session represent the
	/// state effective at the given changeset.
	///
	/// @see CommonBuilder#atChangeset(Object)
	record EffectiveChangeset(Object changesetId)
			implements EntityManager.CreationOption, EntityAgent.CreationOption {
	}

	/// Specify the instant for reading [temporal][org.hibernate.annotations.Temporal]
	/// entity data. Instances of temporal entities retrieved in the created session
	/// represent the revisions effective at the given instant.
	///
	/// @see CommonBuilder#asOf(Instant)
	record EffectiveAt(Instant instant)
			implements EntityManager.CreationOption, EntityAgent.CreationOption {
	}

	/// Specifies that the named [filter][org.hibernate.annotations.FilterDef]
	/// should be enabled with the given arguments to its parameters.
	///
	/// @param name The [name][org.hibernate.annotations.FilterDef#name] of the filter
	/// @param arguments The arguments to the named parameters of the filter
	///
	/// @see SharedSessionContract#enableFilter(String)
	record EnabledFilter(String name, Map<String, ?> arguments)
			implements EntityManager.CreationOption, EntityAgent.CreationOption {
		public EnabledFilter {
			arguments = new HashMap<>( arguments );
		}
	}

	/// Enables batch fetching and specifies how many entities should be fetched in
	/// each request to the database.
	///
	/// - By default, the batch sizing strategy is determined by the
	///   [SQL Dialect][org.hibernate.dialect.Dialect#getBatchLoadSizingStrategy],
	///   but
	/// - if some `batchSize>1` is specified using this option, then that batch size
	///   is used.
	///
	/// If an explicit batch size is set manually, care should be taken to not exceed
	/// the capabilities of the underlying database.
	///
	/// The performance impact of setting a batch size depends on whether a SQL array
	/// may be used to pass the list of identifiers to the database:
	///
	/// - for databases which support standard SQL arrays, a smaller batch size might
	///   be extremely inefficient compared to a very large batch size or no batching
	///   at all, but
	/// - on the other hand, for databases with no SQL array type, a large batch size
	///   results in long SQL statements with many JDBC parameters.
	///
	/// @param batchSize The batch size
	///
	/// @see Session#setFetchBatchSize(int)
	/// @see org.hibernate.cfg.FetchSettings#DEFAULT_BATCH_FETCH_SIZE
	record FetchBatchSize(int batchSize)
			implements EntityManager.CreationOption, EntityAgent.CreationOption {
	}

	/// Enables or disables the use of [subselect fetching][FetchMethod#BY_SUBQUERY].
	///
	/// @see org.hibernate.Session#setSubselectFetchingEnabled(boolean)
	/// @see org.hibernate.cfg.FetchSettings#USE_SUBSELECT_FETCH
	enum PreferredFetchMethod
			implements EntityManager.CreationOption, EntityAgent.CreationOption {
		/// Enables [subselect fetching][FetchMethod#BY_SUBQUERY]
		BY_SUBQUERY,
		/// Disables [subselect fetching][FetchMethod#BY_SUBQUERY]
		BY_ID
	}

	/**
	 * Enables JDBC statement batching and specifies a batch size.
	 *
	 * @param batchSize The batch size for JDBC batch updates
	 *
	 * @see Session#setJdbcBatchSize(Integer)
	 * @see org.hibernate.cfg.BatchSettings#STATEMENT_BATCH_SIZE
	 *
	 * @apiNote Not defined as an {@link EntityAgent.CreationOption}
	 * because JDBC batching at the session level results in a sort
	 * of write-behind behavior which is foreign to the nature of
	 * the stateless programming model.
	 */
	record JdbcBatchSize(int batchSize)
			implements EntityManager.CreationOption {
	}
}
