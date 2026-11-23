package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.Property;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.LazyTableGroup;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

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
@Incubating(since = "5.4")
public interface AuxiliaryMapping {
	/// Called during mapping initialization for the inverse side of a shared collection table.
	/// Returns the auxiliary mapping to use for reads of that property, or null if none applies.
	@Nullable
	default AuxiliaryMapping forInverseCollection(@Nonnull Property inverseProperty) {
		return null;
	}

	/// The primary auxiliary table name.
	///
	/// Single-table state-management strategies generally use this table for all
	/// auxiliary state.  Multi-table strategies, such as audit mappings for
	/// joined inheritance or secondary tables, may use this as the primary table
	/// and resolve table-specific auxiliary names through
	/// [#resolveTableName(String)].
	@Nonnull
	String getTableName();

	/// Resolves the auxiliary table name corresponding to an original mapped
	/// table.
	///
	/// For multi-table inheritance and secondary-table mappings, each source
	/// table may have its own auxiliary table.  Single-table strategies usually
	/// return [#getTableName()].
	@Nonnull
	default String resolveTableName(@Nonnull String originalTableName) {
		return getTableName();
	}

	/// Applies the auxiliary row restriction while resolving a lazy table group.
	///
	/// This form is used when table references may be created lazily as SQL AST
	/// walking discovers which tables are needed for an entity-valued path.
	void applyPredicate(
			@Nonnull EntityMappingType associatedEntityMappingType,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull LazyTableGroup lazyTableGroup,
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqlAstCreationState creationState);

	/// Applies the auxiliary row restriction to an entity table group.
	///
	/// Implementations should add any predicates required to restrict the table
	/// group to rows visible under the supplied [LoadQueryInfluencers], such as
	/// non-deleted rows or rows valid for the requested temporal identifier.
	void applyPredicate(
			@Nonnull EntityMappingType associatedEntityDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers);

	/// Applies the auxiliary row restriction to a collection table group.
	///
	/// This is the collection counterpart to the entity table-group overload and
	/// is used for state-management rules attached to plural attributes.
	void applyPredicate(
			@Nonnull PluralAttributeMapping associatedEntityDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers);

	/// Restricts the rows of an entity which is the target of an association. A
	/// predicate on the row alone is passed to `rowRestrictions`, and may be applied
	/// inside a preserved join operand. A predicate which also refers to the owner of
	/// the association is passed to `ownerRestrictions`, and must be applied where
	/// both are in scope.
	///
	/// In an all-revisions query, `ownerRevision` is the revision of the audit snapshot
	/// which owns the association, and the rows are read as of that revision.
	default void applyAssociationRestrictions(
			@Nonnull EntityMappingType associatedEntityDescriptor, @Nonnull TableGroup tableGroup,
			@Nullable Expression ownerRevision,
			@Nonnull Consumer<Predicate> rowRestrictions, @Nonnull Consumer<Predicate> ownerRestrictions,
			@Nonnull SqlAliasBaseGenerator aliases, @Nonnull LoadQueryInfluencers influencers) {
		applyPredicate( associatedEntityDescriptor, rowRestrictions, tableGroup, aliases, influencers );
	}

	/// Restricts the rows of a collection, in the same way as the overload for the
	/// target of an association restricts an entity. For an inverse collection,
	/// `collectionDescriptor` is the inverse attribute being read.
	default void applyAssociationRestrictions(
			@Nonnull PluralAttributeMapping collectionDescriptor, @Nonnull TableGroup tableGroup,
			@Nullable Expression ownerRevision,
			@Nonnull Consumer<Predicate> rowRestrictions, @Nonnull Consumer<Predicate> ownerRestrictions,
			@Nonnull SqlAliasBaseGenerator aliases, @Nonnull LoadQueryInfluencers influencers) {
		applyPredicate( collectionDescriptor, rowRestrictions, tableGroup, aliases, influencers );
	}

	/// Applies the auxiliary row restriction directly to a table-group join.
	///
	/// This form is used when the restriction belongs on the join predicate
	/// rather than on a surrounding query predicate collector.
	void applyPredicate(
			@Nonnull TableGroupJoin tableGroupJoin,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers);

	/// Applies the auxiliary row restriction for a resolved root table
	/// reference.
	///
	/// The predicate collector is supplied lazily because some callers only need
	/// to allocate a predicate collection when the auxiliary mapping actually
	/// contributes a restriction.
	void applyPredicate(
			@Nonnull Supplier<Consumer<Predicate>> predicateCollector,
			@Nonnull SqlAstCreationState creationState,
			@Nonnull TableGroup tableGroup,
			@Nonnull NamedTableReference rootTableReference, @Nonnull EntityMappingType entityMappingType);

	/// Applies the auxiliary restriction to a joined table reference.
	///
	/// Used for joined inheritance and other multi-table shapes where each
	/// joined source table may have a corresponding auxiliary table.
	///
	/// @param originalTableName the original non-auxiliary table name
	default void applyPredicate(
			@Nonnull TableReferenceJoin tableReferenceJoin,
			@Nonnull NamedTableReference primaryTableReference,
			@Nonnull String originalTableName,
			@Nonnull EntityMappingType entityMappingType,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
	}

	/// Additional select expressions to include in each branch of a
	/// table-per-class union subquery.
	///
	/// Audit mappings use this to expose revision-related columns from union
	/// branches.  Strategies that do not need extra branch selections return an
	/// empty list.
	@Nonnull
	default List<String> getExtraSelectExpressions() {
		return List.of();
	}

	/// The JDBC mapping for temporal/as-of comparison values used by this
	/// auxiliary mapping.
	///
	/// This is used when SQL AST table references need to carry an as-of value
	/// for dialects that support native temporal table syntax.
	@Nonnull
	JdbcMapping getJdbcMapping();

	/// Whether this mapping can contribute a row restriction to an association join.
	default boolean hasRowRestriction(@Nonnull LoadQueryInfluencers influencers) {
		return true;
	}

	/// Whether fetching a collection must keep its owner when this mapping hides every row.
	default boolean requiresOwnerPreservationOnCollectionFetch() {
		return false;
	}

	/// Whether these session settings require a target table read for an association
	/// key that could otherwise be read from the foreign key.
	default boolean requiresTargetTableForKeyRead(@Nonnull LoadQueryInfluencers influencers) {
		return isAffectedByRowRestrictions( influencers );
	}

	/// Whether row visibility differs from a load using the default session settings.
	/// Mappings whose SQL varies for other reasons may refine the conservative default.
	default boolean isAffectedByRowRestrictions(@Nonnull LoadQueryInfluencers influencers) {
		return isAffectedByInfluencers( influencers );
	}

	/// Whether this mapping should use its auxiliary table for the supplied
	/// query influencers.
	///
	/// Some strategies use the primary table for current-state queries and an
	/// auxiliary table only for historical or audit views.
	boolean useAuxiliaryTable(@Nonnull LoadQueryInfluencers influencers);

	/// Whether a one-to-many read must join a separate membership table to the element table.
	default boolean requiresSeparateMembershipTable(@Nonnull LoadQueryInfluencers influencers) {
		return false;
	}

	/// The start of a row version when reading at an owner's revision, for testing
	/// whether the joined versions of a collection row, its key, and its element coexist.
	@Nullable
	default SelectableMapping getVersionStart(@Nonnull String originalTableName, @Nonnull LoadQueryInfluencers influencers) {
		return null;
	}

	/// Whether the rows of this mapping can be read as of the revision of the owner of
	/// an association, in which case they are read from its history, in place of its
	/// current state, when an association restriction is given such a revision.
	default boolean readsHistoryAtOwnerRevision(@Nonnull LoadQueryInfluencers influencers) {
		return false;
	}

	/// Whether query plans involving this mapping are affected by the supplied
	/// influencers.
	///
	/// Returning `true` tells load-plan caching that influencer-dependent SQL may
	/// be required, for example when temporal identifiers or audit revision
	/// options alter table selection or restrictions.
	boolean isAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers);
}
