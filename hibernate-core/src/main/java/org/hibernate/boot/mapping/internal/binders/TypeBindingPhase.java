/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

/// Narrow contracts for coordinator-driven type binding phases.
///
/// Each nested interface represents one phase a type binder may participate in.
/// Binders implement only the phases that apply to them, avoiding no-op phase
/// methods while still making the coordinator's ordering explicit.
///
/// The coordinator runs participating binders in this order:
///
/// 1. [TypeSkeleton] publishes minimal mapping objects so later phases can
///    resolve local type binders without falling back to global metadata lookups.
/// 2. [Tables] creates primary, secondary, joined-subclass, and other table
///    shells before values need to attach to them.
/// 3. [SuperType] connects identifiable mapping objects to their already-created
///    super type skeletons.
/// 4. [EntityMetadata] applies entity-level metadata that does not depend on
///    member value binding.
/// 5. [Identifiers] creates root identifier value shapes.
/// 6. [AssociationIdentifiers] completes identifier attributes that are
///    associations, after every root identifier shape is known.
/// 7. [Members] binds discriminator, version, tenant id, and persistent
///    attributes.
/// 8. [CustomMapping] runs [custom type binders][org.hibernate.binder.TypeBinder]
///    against structurally complete managed-type mapping objects.
/// 9. [ComponentBindingPhase.CustomMapping] runs custom component binders
///    against structurally complete component mapping objects.
/// 10. [AttributeBindingPhase.CustomMapping] runs custom attribute/value binders
///    against structurally complete attribute/value mapping objects.
/// 11. [AttributeBindingPhase.ValueResolution] finalizes materializer-created
///     basic values after custom mapping binders have had a chance to mutate
///     them.
/// 12. [DiscriminatorValues] derives explicit and implicit discriminator values
///     after discriminator basic values have been resolved.
/// 13. [CollectionIndexes] resolves collection index/key values that refer to
///    element properties, such as `@MapKey(name)`.
/// 14. [AssociationTargets] resolves non-primary-key association targets.
/// 15. [DerivedIdentifiers] resolves derived identifier associations such as
///     `@MapsId`.
/// 16. Component, attribute custom-mapping, and value-resolution phases are
///     drained again for mappings created by the late association/identifier
///     phases.
/// 17. [TableKeys] creates dependent table keys for joined-subclass,
///     secondary-table, and collection/association-table structures.
/// 18. [InverseAssociations] copies owning-side key/value state for `mappedBy`
///     associations.
/// 19. [ForeignKeys] creates and customizes physical foreign-key constraints.
/// 20. [Finalization] derives final mapping side effects after all boot
///     model facts for the new pipeline are available.
///
/// Later phases should consume typed state produced by earlier phases rather than
/// searching the partially-built mapping model opportunistically.  When a new
/// ordering dependency appears, prefer adding a narrow phase or typed pending
/// binding over reintroducing callback-style "second pass" work.
///
/// @since 9.0
/// @author Steve Ebersole
public interface TypeBindingPhase {
	/// Publish the minimal type skeleton so other binders can resolve it.
	interface TypeSkeleton {
		void bindTypeSkeleton();
	}

	/// Bind table shells owned by the participating type.
	interface Tables {
		void bindTables();
	}

	/// Wire the participating type to its resolved super type.
	interface SuperType {
		void bindSuperType();
	}

	/// Bind entity-level metadata that does not require member value binding.
	interface EntityMetadata {
		void bindEntityMetadata();
	}

	/// Bind the root identifier shape for an entity hierarchy.
	interface Identifiers {
		void bindIdentifier();
	}

	/// Resolve identifier attributes that are themselves associations after target
	/// identifier shapes are available, deferring final validation until derived
	/// identifier columns have been normalized.
	interface AssociationIdentifiers {
		/// @return {@code true} when at least one pending association identifier was completed
		boolean bindAssociationIdentifiers(boolean strict);
	}

	/// Bind table keys that depend on the completed root identifier shape and
	/// table-valued members.
	///
	/// Examples include joined-subclass table keys, secondary-table join keys,
	/// and association-table join keys.
	interface TableKeys {
		void bindTableKeys();
	}

	/// Resolve inverse associations that depend on key/value state produced by
	/// owning-side association binding.
	interface InverseAssociations {
		void bindInverseAssociations();
	}

	/// Create and customize physical foreign-key constraints after association
	/// values, table keys, and inverse association structures are available.
	interface ForeignKeys {
		void bindForeignKeys();
	}

	/// Bind discriminator, version, tenant id, and attributes.
	interface Members {
		void bindMembers();
	}

	/// Derive entity discriminator values after discriminator value resolution.
	interface DiscriminatorValues {
		void bindDiscriminatorValues();
	}

	/// Apply custom type mapping after the managed-type mapping object is
	/// structurally available and before attribute/value custom mapping.
	interface CustomMapping {
		void bindCustomMapping();
	}

	/// Resolve collection index values that depend on member bindings from
	/// another type.
	///
	/// For example, {@code @MapKey(name)} points at a property of the collection
	/// element type.  The collection member can be created before that property is
	/// bound, so this phase runs after all members and before table keys call
	/// collection key creation.
	interface CollectionIndexes {
		void bindCollectionIndexes();
	}

	/// Resolve association target properties for non-primary-key references.
	interface AssociationTargets {
		void bindAssociationTargets();
	}

	/// Resolve derived identifier associations such as {@code @MapsId} after
	/// owner identifiers and to-one members have both been created.
	interface DerivedIdentifiers {
		void bindDerivedIdentifiers();
	}

	/// Finalize mapping details that need the complete bound model but are not
	/// unresolved binding work.
	interface Finalization {
		void finalizeBinding();
	}
}
