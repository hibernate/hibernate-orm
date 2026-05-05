/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.spi;

import org.hibernate.Incubating;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.persister.state.internal.HistoryStateManagement;
import org.hibernate.persister.state.internal.SoftDeleteStateManagement;
import org.hibernate.persister.state.internal.StandardStateManagement;
import org.hibernate.persister.state.internal.TemporalStateManagement;

/// Integrates a state-management strategy with Hibernate's mapping model and
/// mutation execution infrastructure.
///
/// The root contract identifies the state-management model for a mapping.
/// Its direct methods currently build the auxiliary mapping artifact used to
/// represent temporal, history, audit, and soft-delete state in the runtime
/// mapping model.  Legacy-only mutation hooks for that artifact are isolated in
/// [org.hibernate.metamodel.mapping.LegacyAuxiliaryMutationSupport].
///
/// Queue-specific execution concerns are exposed through nested integration
/// contracts:
///
/// - [#getGraphIntegration()] exposes graph action-queue decomposition
///   contributors.
/// - [#getLegacyIntegration()] exposes legacy action-queue coordinator
///   factories.
///
/// This separation keeps graph decomposition from depending on legacy
/// coordinator concepts, and keeps the explicit legacy coordinator surface
/// isolated for eventual removal.
///
/// Every concrete implementation of this interface should declare a field
/// `public static final StateManagement INSTANCE`.
///
/// @see StandardStateManagement
/// @see SoftDeleteStateManagement
/// @see TemporalStateManagement
/// @see HistoryStateManagement
/// @see AuditStateManagement
///
/// @author Gavin King
/// @since 7.4
@Incubating
public interface StateManagement {

	/// Creates the auxiliary entity mapping required by this state-management
	/// strategy, or `null` when the entity does not need one.
	///
	/// Auxiliary mappings are rooted in the mapping model.  Legacy-only mutation
	/// behavior is exposed separately through
	/// [org.hibernate.metamodel.mapping.LegacyAuxiliaryMutationSupport].
	AuxiliaryMapping createAuxiliaryMapping(
			EntityPersister persister,
			RootClass bootDescriptor,
			MappingModelCreationProcess creationProcess);

	/// Creates the auxiliary collection mapping required by this
	/// state-management strategy, or `null` when the collection does not need
	/// one.
	///
	/// Auxiliary mappings are rooted in the mapping model.  Legacy-only mutation
	/// behavior is exposed separately through
	/// [org.hibernate.metamodel.mapping.LegacyAuxiliaryMutationSupport].
	AuxiliaryMapping createAuxiliaryMapping(
			PluralAttributeMapping pluralAttributeMapping,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess);

	/// Provides the graph action-queue integration for this state-management
	/// strategy.
	///
	/// The default integration contributes standard entity and collection
	/// mutation plans.
	default StateManagementGraphIntegration getGraphIntegration() {
		return StateManagementGraphIntegration.STANDARD;
	}

	/// Provides the legacy action-queue integration for this state-management
	/// strategy.
	///
	/// This is the coordinator-based integration used by the legacy queue.  It
	/// is intentionally isolated from the root contract so the legacy surface can
	/// be retired without disturbing mapping semantics or graph integration.
	StateManagementLegacyIntegration getLegacyIntegration();
}
