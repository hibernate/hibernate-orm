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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;

/**
 * Unified mapping contract for state management strategies (soft-delete, temporal, audit).
 *
 * @author Gavin King
 *
 * @since 7.4
 */
@Incubating
public interface AuxiliaryMapping {
	/**
	 * The name of the primary auxiliary table.
	 * For multi-table strategies (e.g. audit), use {@link #resolveTableName(String)} to resolve per-table instead.
	 */
	String getTableName();

	/**
	 * Resolve the auxiliary table name for the given original table.
	 * For multi-table inheritance or {@code @SecondaryTable}, each table
	 * may have its own auxiliary table. Defaults to {@link #getTableName()}.
	 */
	default String resolveTableName(String originalTableName) {
		return getTableName();
	}

	default void addToInsertGroup(MutationGroupBuilder insertGroupBuilder, EntityPersister persister) {}

	void applyPredicate(
			EntityMappingType associatedEntityMappingType,
			Consumer<Predicate> predicateConsumer,
			LazyTableGroup lazyTableGroup,
			NavigablePath navigablePath,
			SqlAstCreationState creationState);

	void applyPredicate(
			EntityMappingType associatedEntityDescriptor,
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers);

	void applyPredicate(
			PluralAttributeMapping associatedEntityDescriptor,
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers);

	void applyPredicate(TableGroupJoin tableGroupJoin, LoadQueryInfluencers loadQueryInfluencers);

	void applyPredicate(
			Supplier<Consumer<Predicate>> predicateCollector,
			SqlAstCreationState creationState,
			TableGroup tableGroup,
			NamedTableReference rootTableReference, EntityMappingType entityMappingType);

	/**
	 * Apply the auxiliary restriction to a joined table reference.
	 * Used for JOINED inheritance where each table in the hierarchy
	 * has its own auxiliary table.
	 *
	 * @param originalTableName the original (non-auxiliary) table name
	 */
	default void applyPredicate(
			TableReferenceJoin tableReferenceJoin,
			NamedTableReference primaryTableReference,
			String originalTableName,
			EntityMappingType entityMappingType,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers) {
	}

	/**
	 * Additional column expressions to include in each SELECT of a
	 * TABLE_PER_CLASS union subquery (e.g. REV, REVTYPE for audit).
	 */
	default List<String> getExtraSelectExpressions() {
		return List.of();
	}

	JdbcMapping getJdbcMapping();

	boolean useAuxiliaryTable(LoadQueryInfluencers influencers);

	boolean isAffectedByInfluencers(LoadQueryInfluencers influencers);
}
