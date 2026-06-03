/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityAgent;

import java.time.Instant;

/// Declares various for [session][EntityManager.CreationOption] and
/// [stateless session][EntityAgent.CreationOption] creation options.
///
/// @apiNote Serves no functional purpose - a place to consolidate
///          the actual options for easier discovery / documentation.
///
/// @see ReadOnlyMode
/// @see FlushMode
/// @see BatchSize
/// @see CacheMode
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public interface SessionCreationOption {
	/// Specifies the tenant-id which should be used when creating a Session or StatelessSession.
	///
	/// @see SharedSessionContract#getTenantIdentifier()
	record TenantId(String value) implements EntityManager.CreationOption, EntityAgent.CreationOption {
	}

	/// Specify the [changeset id][org.hibernate.cfg.StateManagementSettings#CHANGESET_ID_SUPPLIER]
	/// for reading [temporal][org.hibernate.annotations.Temporal] or
	/// [audited][org.hibernate.annotations.Audited]/ entity data.
	/// Instances of temporal  or audited entities retrieved in/ the session represent the state
	/// effective at the given changeset.
	record EffectiveChangeset(Object changesetId) implements EntityManager.CreationOption, EntityAgent.CreationOption {
	}

	/// Specify the instant for reading [temporal][org.hibernate.annotations.Temporal] entity data.
	/// Instances of temporal entities retrieved in the created session represent the
	/// revisions effective at the given instant.
	record EffectiveAt(Instant instant) implements EntityManager.CreationOption, EntityAgent.CreationOption {
	}

	/// Allow explicitly enabling or disabling subselect fetching for an EntityManager.
	///
	/// @see org.hibernate.cfg.FetchSettings#USE_SUBSELECT_FETCH
	enum SubselectFetchMode implements EntityManager.CreationOption {
		/// Enables subselect fetching.
		ENABLED,
		/// Disables subselect fetching.
		DISABLED
	}
}
