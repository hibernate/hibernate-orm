/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.spi.PrimaryKeyNamingInput;
import org.hibernate.sql.Alias;
import org.hibernate.boot.model.naming.internal.ImplicitNamingHelper;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.naming.spi.AggregateColumnNamingInput;
import org.hibernate.boot.model.naming.spi.EmbeddableDiscriminatorColumnNamingInput;
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

import org.hibernate.boot.model.naming.spi.TimeZoneColumnNamingInput;
import org.hibernate.SPI;
import org.hibernate.boot.model.naming.spi.CollectionIdColumnNamingInput;
import org.hibernate.boot.model.naming.spi.SoftDeleteColumnNamingInput;
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
/// Every callback requires non-null inputs and must return a non-null name.
/// Methods returning [LogicalName] must return a name with
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
	@Nonnull
	LogicalName determinePrimaryTableName(@Nonnull PrimaryTableNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the name of an association join table given the source naming
	/// information, when a name is not explicitly given. This method is called
	/// for any sort of association with a join table, no matter what the logical
	/// cardinality. The explicit name is supplied by [jakarta.persistence.JoinTable#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit table name.
	@Nonnull
	LogicalName determineAssociationTableName(@Nonnull AssociationTableNamingInput input, @Nonnull ImplicitNamingContext context);

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
	@Nonnull
	LogicalName determineCollectionTableName(@Nonnull CollectionTableNamingInput input, @Nonnull ImplicitNamingContext context);

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
	@Nonnull
	LogicalName determineIdentifierColumnName(@Nonnull IdentifierColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit column name for an [org.hibernate.annotations.TenantId]
	/// attribute when no name is supplied by [jakarta.persistence.Column#name()].
	/// Supplied strategies use the terminal attribute name, including for embedded attributes.
	/// Explicit column names bypass this callback; existing identifier columns are reused.
	///
	/// @param input entity naming information and the tenant attribute path
	/// @param context naming defaults and helpers
	/// @return a non-null implicit logical column name
	@Nonnull
	LogicalName determineTenantColumnName(@Nonnull TenantColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit entity discriminator-column name. The default implementation returns `DTYPE`.
	/// A present [jakarta.persistence.DiscriminatorColumn] with a nonempty
	/// [name][jakarta.persistence.DiscriminatorColumn#name()],
	/// including its annotation default `DTYPE`, bypasses this callback.
	///
	/// @param input Root-entity naming information
	/// @param context Focused naming defaults and helpers
	/// @return A non-null implicit logical name
	@Nonnull
	default LogicalName determineDiscriminatorColumnName(@Nonnull DiscriminatorColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( "DTYPE" );
	}

	/// Determine the implicit discriminator-column name for a polymorphic embeddable.
	/// The default implementation uses `DTYPE` for an empty discriminator-column declaration,
	/// `element_DTYPE` for collection elements otherwise, or the terminal attribute name
	/// suffixed with `_DTYPE`. It does not invoke basic or entity discriminator naming.
	/// A nonempty [jakarta.persistence.DiscriminatorColumn#name()] or a column name
	/// supplied through [jakarta.persistence.AttributeOverride#column()] for the
	/// special `{discriminator}` role bypasses this callback. A present discriminator
	/// annotation supplies its default `DTYPE` unless its name is explicitly empty.
	///
	/// @param input Owner, embeddable type, full attribute path, role, and effective declaration origin
	/// @param context Naming defaults and helpers
	/// @return A non-null implicit logical column name
	@Nonnull
	default LogicalName determineEmbeddableDiscriminatorColumnName(@Nonnull EmbeddableDiscriminatorColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( input.declaration() == EmbeddableDiscriminatorColumnNamingInput.Declaration.DISCRIMINATOR_COLUMN
				? "DTYPE"
				: input.kind() == EmbeddableDiscriminatorColumnNamingInput.Kind.COLLECTION_ELEMENT
						? "element_DTYPE"
						: AttributePath.parse( input.attributePath() ).getProperty() + "_DTYPE" );
	}

	/// Determine the column name for a [basic][jakarta.persistence.Basic] or
	/// [version][jakarta.persistence.Version] attribute when it is not explicitly specified using
	/// [jakarta.persistence.Column#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit column name.
	@Nonnull
	LogicalName determineBasicColumnName(@Nonnull BasicColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit name of an aggregate container column or nested member.
	/// Supplied strategies use the terminal attribute name. Explicit names from
	/// [jakarta.persistence.Column#name()], [jakarta.persistence.MapKeyColumn#name()],
	/// or an applicable [jakarta.persistence.AttributeOverride#column()] bypass this callback.
	/// Aggregate storage may be selected using [org.hibernate.annotations.JdbcTypeCode],
	/// [org.hibernate.annotations.MapKeyJdbcTypeCode], or [org.hibernate.annotations.Struct].
	/// The [SQL type name][org.hibernate.annotations.Struct#name()] is separate from this name.
	///
	/// @param input Owner, type, path, usage, storage kind, plurality, and naming scope
	/// @param context Naming defaults and helpers
	/// @return A non-null implicit logical name
	@Nonnull
	LogicalName determineAggregateColumnName(@Nonnull AggregateColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit column name for a basic [jakarta.persistence.ElementCollection]
	/// element when no name is supplied by [jakarta.persistence.Column#name()].
	///
	/// @param input Collection attribute information
	/// @param context Naming defaults and helpers
	/// @return The implicit element column name
	@Nonnull
	LogicalName determineCollectionElementColumnName(@Nonnull CollectionElementColumnNamingInput input, @Nonnull ImplicitNamingContext context);

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
	@Nonnull
	LogicalName determineJoinColumnName(@Nonnull JoinColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit name of a column referencing the owner of a collection
	/// or association table. Explicit names are supplied by [jakarta.persistence.JoinColumn#name()]
	/// within [jakarta.persistence.CollectionTable#joinColumns()] or
	/// [jakarta.persistence.JoinTable#joinColumns()], or directly on a
	/// [jakarta.persistence.OneToMany] association using a foreign key.
	///
	/// @param input Owner, collection/association role, and referenced-column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit owner-key column name
	@Nonnull
	LogicalName determineCollectionKeyColumnName(@Nonnull CollectionKeyNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit name of a column referencing the association target
	/// from a join table. Explicit names are supplied by [jakarta.persistence.JoinColumn#name()]
	/// within [jakarta.persistence.JoinTable#inverseJoinColumns()].
	///
	/// @param input Association information and target-column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit target-key column name
	@Nonnull
	LogicalName determineAssociationKeyColumnName(@Nonnull AssociationKeyNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit join-column name for an entity-valued map key when no
	/// name is supplied by [jakarta.persistence.MapKeyJoinColumn#name()], directly or
	/// within [jakarta.persistence.MapKeyJoinColumns].
	///
	/// @param input Map attribute information and key-entity column dependencies
	/// @param context Naming defaults and helpers
	/// @return The implicit map-key join-column name
	@Nonnull
	LogicalName determineMapKeyJoinColumnName(@Nonnull MapKeyJoinColumnNamingInput input, @Nonnull ImplicitNamingContext context);

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
	@Nonnull
	LogicalName determinePrimaryKeyJoinColumnName(@Nonnull PrimaryKeyJoinColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the column name related to the discriminator portion of an
	/// [org.hibernate.annotations.Any] or [org.hibernate.annotations.ManyToAny] mapping
	/// when no explicit column
	/// name is given using [jakarta.persistence.Column#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The determined column name
	@Nonnull
	LogicalName determineAnyDiscriminatorColumnName(@Nonnull AnyColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the join column name related to the key/id portion of an
	/// [org.hibernate.annotations.Any] or [org.hibernate.annotations.ManyToAny] mapping
	/// when no explicit join column
	/// name is given using [jakarta.persistence.JoinColumn#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The determined identifier column name
	@Nonnull
	LogicalName determineAnyKeyColumnName(@Nonnull AnyColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the column name for a basic map key when it is not explicitly specified using
	/// [jakarta.persistence.MapKeyColumn#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit column name.
	@Nonnull
	LogicalName determineMapKeyColumnName(@Nonnull MapKeyColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the list index column name when it is not explicitly specified using
	/// [jakarta.persistence.OrderColumn#name()].
	///
	/// @param input Immutable facts for the naming decision
	/// @param context Focused naming defaults and helpers
	///
	/// @return The implicit column name.
	@Nonnull
	LogicalName determineListIndexColumnName(@Nonnull ListIndexColumnNamingInput input, @Nonnull ImplicitNamingContext context);

	/// Determine the implicit collection-row identifier column name when
	/// [org.hibernate.annotations.CollectionId#column()] has no explicit
	/// [jakarta.persistence.Column#name()]. The default implementation returns `id`.
	@Nonnull
	default LogicalName determineCollectionIdColumnName(@Nonnull CollectionIdColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( "id" );
	}

	/// Determine the implicit indicator name when
	/// [org.hibernate.annotations.SoftDelete#columnName()] is empty.
	/// The effective [org.hibernate.annotations.SoftDelete#strategy()] is supplied
	/// as [org.hibernate.annotations.SoftDeleteType]. The default implementation uses
	/// [its default column name][org.hibernate.annotations.SoftDeleteType#getDefaultColumnName()].
	@Nonnull
	default LogicalName determineSoftDeleteColumnName(@Nonnull SoftDeleteColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( input.strategy().getDefaultColumnName() );
	}

	/// Determine the offset companion name when [org.hibernate.annotations.TimeZoneColumn#name()]
	/// is absent or empty. With no companion declaration, the default implementation appends
	/// `_tz` to the original temporal source name, independently of temporal overrides.
	/// With an empty declaration, it applies basic naming to the synthetic `zoneOffset`
	/// member path. An already-resolved source name is reused; otherwise basic naming
	/// is invoked only when the selected algorithm needs it, through
	/// [#determineBasicColumnName(BasicColumnNamingInput, ImplicitNamingContext)].
	/// Storage selection through [org.hibernate.annotations.TimeZoneStorage] is separate:
	/// this callback is used only for an actual companion column under
	/// [org.hibernate.annotations.TimeZoneStorageType#COLUMN] or
	/// [org.hibernate.annotations.TimeZoneStorageType#AUTO].
	///
	/// @param input The owner, user attribute path, settled temporal name pair, destination table, and original source facts
	/// @param context Focused naming defaults and helpers
	/// @return A non-null implicit logical column name
	@Nonnull
	default LogicalName determineTimeZoneColumnName(@Nonnull TimeZoneColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		if ( input.companionDeclared() ) {
			return determineBasicColumnName( new BasicColumnNamingInput( input.attributePath() + ".zoneOffset" ), context );
		}
		final var sourceName = input.sourceColumnName().orElseGet( () -> {
			final var name = determineBasicColumnName( new BasicColumnNamingInput( input.attributePath() ), context );
			ImplicitNamingHelper.columnName( name, "basic column" );
			return name;
		} );
		return context.implicitName( sourceName.getText() + "_tz", sourceName.isQuoted() );
	}

	/// Determine the implicit primary-key constraint name for a mapped table.
	/// The default uses the physical table name, truncated to twelve characters,
	/// followed by `_pk`, preserving the table's quoting.
	/// Naming the mapping constraint does not itself cause its name to be emitted in DDL.
	///
	/// @param input The table's logical and physical names
	/// @param context Focused naming defaults and helpers
	/// @return A non-null implicit logical constraint name
	@Nonnull
	default LogicalName determinePrimaryKeyName(@Nonnull PrimaryKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
		final var tableName = input.table().names().physicalName();
		// HHH-20915: use the Dialect's maximum identifier length instead of the fixed limit.
		final var name = new Alias( 15, "_pk" ).toUnquotedAliasString( tableName.toString() );
		return context.implicitName( name, tableName.isQuoted() );
	}

	/// Determine the foreign key name when it is not explicitly specified using
	/// [jakarta.persistence.ForeignKey#name()].
	///
	/// @param source The source information
	///
	/// @return The determined foreign key name
	@Nonnull
	Identifier determineForeignKeyName(@Nonnull ImplicitForeignKeyNameSource source);

	/// Determine the unique key name when it is not explicitly specified using
	/// [jakarta.persistence.UniqueConstraint#name()]. This also covers generated
	/// unique keys requested by [jakarta.persistence.Column#unique()] or
	/// [jakarta.persistence.JoinColumn#unique()].
	///
	/// @param source The source information
	///
	/// @return The implicit unique-key name
	@Nonnull
	Identifier determineUniqueKeyName(@Nonnull ImplicitUniqueKeyNameSource source);

	/// Determine the index name when it is not explicitly specified using
	/// [jakarta.persistence.Index#name()].
	///
	/// @param source The source information
	///
	/// @return The implicit index name
	@Nonnull
	Identifier determineIndexName(@Nonnull ImplicitIndexNameSource source);
}
