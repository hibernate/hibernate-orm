/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/// Runtime mapping contract for state-management state associated with an
/// entity or collection.
///
/// An auxiliary mapping describes how a state-management model participates in
/// SQL AST creation.  Examples include soft-delete indicators, temporal row
/// validity columns, history tables, and audit tables.  The contract covers
/// table-name resolution, load-query restrictions, temporal/as-of table
/// decoration, and query-plan influencer sensitivity.
///
/// Mutation execution is intentionally not part of this contract.  Legacy
/// mutation support for auxiliary mappings is exposed separately through
/// [LegacyAuxiliaryMutationSupport], while graph queue mutation support is
/// provided by state-management graph contributors.
///
/// @author Gavin King
/// @since 7.4
@Incubating
public interface AuxiliaryMapping {
	/// The primary auxiliary table name.
	///
	/// Single-table state-management strategies generally use this table for all
	/// auxiliary state.  Multi-table strategies, such as audit mappings for
	/// joined inheritance or secondary tables, may use this as the primary table
	/// and resolve table-specific auxiliary names through
	/// [#resolveTableName(String)].
	String getTableName();

	/// Resolves the auxiliary table name corresponding to an original mapped
	/// table.
	///
	/// For multi-table inheritance and secondary-table mappings, each source
	/// table may have its own auxiliary table.  Single-table strategies usually
	/// return [#getTableName()].
	default String resolveTableName(String originalTableName) {
		return getTableName();
	}

	/// Applies the auxiliary row restriction while resolving a lazy table group.
	///
	/// This form is used when table references may be created lazily as SQL AST
	/// walking discovers which tables are needed for an entity-valued path.
	void applyPredicate(
			EntityMappingType associatedEntityMappingType,
			Consumer<Predicate> predicateConsumer,
			LazyTableGroup lazyTableGroup,
			NavigablePath navigablePath,
			SqlAstCreationState creationState);

	/// Applies the auxiliary row restriction to an entity table group.
	///
	/// Implementations should add any predicates required to restrict the table
	/// group to rows visible under the supplied [LoadQueryInfluencers], such as
	/// non-deleted rows or rows valid for the requested temporal identifier.
	void applyPredicate(
			EntityMappingType associatedEntityDescriptor,
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers);

	/// Applies the auxiliary row restriction to a collection table group.
	///
	/// This is the collection counterpart to the entity table-group overload and
	/// is used for state-management rules attached to plural attributes.
	void applyPredicate(
			PluralAttributeMapping associatedEntityDescriptor,
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers);

	/// Applies the auxiliary row restriction directly to a table-group join.
	///
	/// This form is used when the restriction belongs on the join predicate
	/// rather than on a surrounding query predicate collector.
	void applyPredicate(TableGroupJoin tableGroupJoin, LoadQueryInfluencers loadQueryInfluencers);

	/// Applies the auxiliary row restriction for a resolved root table
	/// reference.
	///
	/// The predicate collector is supplied lazily because some callers only need
	/// to allocate a predicate collection when the auxiliary mapping actually
	/// contributes a restriction.
	void applyPredicate(
			Supplier<Consumer<Predicate>> predicateCollector,
			SqlAstCreationState creationState,
			TableGroup tableGroup,
			NamedTableReference rootTableReference, EntityMappingType entityMappingType);

	/// Applies the auxiliary restriction to a joined table reference.
	///
	/// Used for joined inheritance and other multi-table shapes where each
	/// joined source table may have a corresponding auxiliary table.
	///
	/// @param originalTableName the original non-auxiliary table name
	default void applyPredicate(
			TableReferenceJoin tableReferenceJoin,
			NamedTableReference primaryTableReference,
			String originalTableName,
			EntityMappingType entityMappingType,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers) {
	}

	/// Additional select expressions to include in each branch of a
	/// table-per-class union subquery.
	///
	/// Audit mappings use this to expose revision-related columns from union
	/// branches.  Strategies that do not need extra branch selections return an
	/// empty list.
	default List<String> getExtraSelectExpressions() {
		return List.of();
	}

	/// The JDBC mapping for temporal/as-of comparison values used by this
	/// auxiliary mapping.
	///
	/// This is used when SQL AST table references need to carry an as-of value
	/// for dialects that support native temporal table syntax.
	JdbcMapping getJdbcMapping();

	/// Whether this mapping should use its auxiliary table for the supplied
	/// query influencers.
	///
	/// Some strategies use the primary table for current-state queries and an
	/// auxiliary table only for historical or audit views.
	boolean useAuxiliaryTable(LoadQueryInfluencers influencers);

	/// Whether query plans involving this mapping are affected by the supplied
	/// influencers.
	///
	/// Returning `true` tells load-plan caching that influencer-dependent SQL may
	/// be required, for example when temporal identifiers or audit revision
	/// options alter table selection or restrictions.
	boolean isAffectedByInfluencers(LoadQueryInfluencers influencers);
}
