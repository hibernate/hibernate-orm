/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.spi.DiscriminatorColumnNamingInput;
import org.hibernate.boot.model.naming.spi.TenantColumnNamingInput;
import org.hibernate.boot.model.naming.spi.JoinColumnNamingInput;
import org.hibernate.boot.model.naming.spi.CollectionKeyNamingInput;
import org.hibernate.boot.model.naming.spi.AssociationKeyNamingInput;
import org.hibernate.boot.model.naming.spi.MapKeyJoinColumnNamingInput;
import org.hibernate.boot.model.naming.spi.PrimaryKeyJoinColumnNamingInput;

import org.hibernate.boot.model.naming.spi.AnyColumnNamingInput;
import org.hibernate.boot.model.naming.spi.BasicColumnNamingInput;
import org.hibernate.boot.model.naming.spi.CollectionElementColumnNamingInput;
import org.hibernate.boot.model.naming.spi.IdentifierColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ListIndexColumnNamingInput;
import org.hibernate.boot.model.naming.spi.MapKeyColumnNamingInput;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.naming.spi.PrimaryTableNamingInput;
import org.hibernate.boot.model.naming.spi.AssociationTableNamingInput;
import org.hibernate.boot.model.naming.spi.CollectionTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;

/// Determines the [logical name][LogicalName] of a mapped database object when its
/// name is not explicitly supplied in mapping metadata, such as annotations or XML.
/// Each callback addresses a particular naming role, such as a primary table,
/// a basic column, or an association key column.
///
/// For example, if a Java class annotated [`@Entity`][jakarta.persistence.Entity]
/// has no explicit table name, then
/// [`determinePrimaryTableName`][#determinePrimaryTableName(PrimaryTableNamingInput, ImplicitNamingContext)]
/// receives a [PrimaryTableNamingInput] describing its entity naming information.
/// This applies both when [`@Table`][jakarta.persistence.Table] is absent and when
/// its [name][jakarta.persistence.Table#name()] is left unspecified.
///
/// An explicit name bypasses the corresponding implicit naming callback. For example,
/// `@Table(name = "people")` supplies the logical table name `people` directly.
/// Both explicit and implicit logical names subsequently undergo
/// [physical naming][PhysicalNamingStrategy] for roles supported by that contract.
/// Explicit naming therefore does not bypass physical naming.
///
/// Callbacks accepting an [ImplicitNamingContext] receive immutable inputs describing
/// the naming decision, together with naming defaults and helpers in the context.
/// Dependency inputs may expose both logical and physical names of related objects,
/// allowing a strategy to choose which spelling to use in composing a new logical name.
/// Methods returning [LogicalName] must return a non-null name with
/// [explicit provenance][LogicalName#isExplicit()] set to `false`; use
/// [ImplicitNamingContext#implicitName(String)] or its quoting overload to construct it.
///
/// Hibernate's default [StandardImplicitNamingStrategy][org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy]
/// uses full attribute paths to distinguish repeated embeddables and logical dependency
/// names to compose association-table names. [ImplicitNamingStrategyJpaCompliantImpl]
/// provides the separately selectable JPA naming conventions. Applications may select
/// a supplied strategy, subclass one to customize individual roles, or implement this
/// interface. Changing strategies may change schema names; verify the resulting mappings
/// when migrating an existing application.
///
/// Select a strategy using the configuration property
/// {@value org.hibernate.cfg.MappingSettings#IMPLICIT_NAMING_STRATEGY} or
/// [Configuration#setImplicitNamingStrategy][org.hibernate.cfg.Configuration#setImplicitNamingStrategy(ImplicitNamingStrategy)].
///
/// @see PhysicalNamingStrategy
/// @see org.hibernate.cfg.Configuration#setImplicitNamingStrategy(ImplicitNamingStrategy)
/// @see org.hibernate.cfg.MappingSettings#IMPLICIT_NAMING_STRATEGY
///
/// @author Steve Ebersole
@Incubating(since = "6.0")
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface ImplicitNamingStrategy {

	/// Determine the implicit name of an [entity's][jakarta.persistence.Entity] primary table
	/// when no name is supplied by [jakarta.persistence.Table#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit table name.
	LogicalName determinePrimaryTableName(PrimaryTableNamingInput input, ImplicitNamingContext context);

	/// Determine the name of an association join table given the source naming
	/// information, when a name is not explicitly given. This method is called
	/// for any sort of association with a join table, no matter what the logical
	/// cardinality. The explicit name is supplied by [jakarta.persistence.JoinTable#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit table name.
	LogicalName determineAssociationTableName(AssociationTableNamingInput input, ImplicitNamingContext context);

	/// Determine the name of a collection join table given the source naming
	/// information, when a name is not explicitly given. This method is called
	/// only for [collections of basic or embeddable values][jakarta.persistence.ElementCollection],
	/// and never for associations. The explicit name is supplied by
	/// [jakarta.persistence.CollectionTable#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit table name.
	LogicalName determineCollectionTableName(CollectionTableNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit entity discriminator-column name, defaulting to `DTYPE`.
	/// A present [jakarta.persistence.DiscriminatorColumn] with a nonempty
	/// [name][jakarta.persistence.DiscriminatorColumn#name()],
	/// including its annotation default `DTYPE`, bypasses this callback.
	///
	/// @param input Root-entity naming information
	/// @param context Focused naming defaults and helpers
	/// @return A non-null implicit logical name
	LogicalName determineDiscriminatorColumnName(DiscriminatorColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit column name for an [org.hibernate.annotations.TenantId]
	/// attribute when no name is supplied by [jakarta.persistence.Column#name()].
	/// Supplied strategies use the terminal attribute name, including for embedded attributes.
	/// Explicit column names bypass this callback; existing identifier columns are reused.
	///
	/// @param input entity naming information and the tenant attribute path
	/// @param context naming defaults and helpers
	/// @return a non-null implicit logical column name
	LogicalName determineTenantColumnName(TenantColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the name of the [identifier][jakarta.persistence.Id] column
	/// belonging to the given entity when it is not explicitly specified using
	/// [jakarta.persistence.Column#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	/// @return The determined identifier column name
	///
	/// @see jakarta.persistence.EmbeddedId
	/// @see jakarta.persistence.AttributeOverride#column()
	LogicalName determineIdentifierColumnName(IdentifierColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the column name for a [basic][jakarta.persistence.Basic] or
	/// [version][jakarta.persistence.Version] attribute when it is not explicitly specified using
	/// [jakarta.persistence.Column#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit column name.
	LogicalName determineBasicColumnName(BasicColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit column name for a basic [jakarta.persistence.ElementCollection]
	/// element when no name is supplied by [jakarta.persistence.Column#name()].
	///
	/// @param input Collection attribute information
	/// @param context Naming defaults and helpers
	/// @return The implicit element column name
	LogicalName determineCollectionElementColumnName(CollectionElementColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit join-column name for a [jakarta.persistence.ManyToOne]
	/// or [jakarta.persistence.OneToOne] association, including association identifiers
	/// and derived identity. Explicit names are supplied by [jakarta.persistence.JoinColumn#name()],
	/// directly or within [jakarta.persistence.JoinColumns].
	///
	/// @param input Association information and selected referenced-column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit join-column name
	/// @see jakarta.persistence.Id
	/// @see jakarta.persistence.MapsId
	LogicalName determineJoinColumnName(JoinColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit name of a column referencing the owner of a collection
	/// or association table. Explicit names are supplied by [jakarta.persistence.JoinColumn#name()]
	/// within [jakarta.persistence.CollectionTable#joinColumns()] or
	/// [jakarta.persistence.JoinTable#joinColumns()], or directly on a
	/// [jakarta.persistence.OneToMany] association using a foreign key.
	///
	/// @param input Owner, collection/association role, and referenced-column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit owner-key column name
	LogicalName determineCollectionKeyColumnName(CollectionKeyNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit name of a column referencing the association target
	/// from a join table. Explicit names are supplied by [jakarta.persistence.JoinColumn#name()]
	/// within [jakarta.persistence.JoinTable#inverseJoinColumns()].
	///
	/// @param input Association information and target-column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit target-key column name
	LogicalName determineAssociationKeyColumnName(AssociationKeyNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit join-column name for an entity-valued map key when no
	/// name is supplied by [jakarta.persistence.MapKeyJoinColumn#name()], directly or
	/// within [jakarta.persistence.MapKeyJoinColumns].
	///
	/// @param input Map attribute information and key-entity column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit map-key join-column name
	LogicalName determineMapKeyJoinColumnName(MapKeyJoinColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the implicit column name for a dependent table's primary-key join,
	/// including [secondary tables][jakarta.persistence.SecondaryTable] and
	/// [joined inheritance][jakarta.persistence.InheritanceType#JOINED]. Explicit names
	/// are supplied by [jakarta.persistence.PrimaryKeyJoinColumn#name()], including
	/// declarations within [jakarta.persistence.PrimaryKeyJoinColumns] or
	/// [jakarta.persistence.SecondaryTable#pkJoinColumns()].
	///
	/// @param input Dependent table and referenced primary-key column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit primary-key join-column name
	LogicalName determinePrimaryKeyJoinColumnName(PrimaryKeyJoinColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the column name related to the discriminator portion of an
	/// [org.hibernate.annotations.Any] or [org.hibernate.annotations.ManyToAny] mapping
	/// when no explicit column
	/// name is given using [jakarta.persistence.Column#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The determined column name
	LogicalName determineAnyDiscriminatorColumnName(AnyColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the join column name related to the key/id portion of an
	/// [org.hibernate.annotations.Any] or [org.hibernate.annotations.ManyToAny] mapping
	/// when no explicit join column
	/// name is given using [jakarta.persistence.JoinColumn#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The determined identifier column name
	LogicalName determineAnyKeyColumnName(AnyColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the column name for a basic map key when it is not explicitly specified using
	/// [jakarta.persistence.MapKeyColumn#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit column name.
	LogicalName determineMapKeyColumnName(MapKeyColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the list index column name when it is not explicitly specified using
	/// [jakarta.persistence.OrderColumn#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit column name.
	LogicalName determineListIndexColumnName(ListIndexColumnNamingInput input, ImplicitNamingContext context);

	/// Determine the foreign key name when it is not explicitly specified using
	/// [jakarta.persistence.ForeignKey#name()].
	///
	/// @param source The source information
	///
	/// @return The determined foreign key name
	Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source);

	/// Determine the unique key name when it is not explicitly specified using
	/// [jakarta.persistence.UniqueConstraint#name()]. This also covers generated
	/// unique keys requested by [jakarta.persistence.Column#unique()] or
	/// [jakarta.persistence.JoinColumn#unique()].
	///
	/// @param source The source information
	///
	/// @return The implicit unique-key name
	Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source);

	/// Determine the index name when it is not explicitly specified using
	/// [jakarta.persistence.Index#name()].
	///
	/// @param source The source information
	///
	/// @return The implicit index name
	Identifier determineIndexName(ImplicitIndexNameSource source);
}
