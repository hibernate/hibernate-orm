/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Unified mapping contract for state management strategies (soft-delete, temporal, audit).
 *
 * @author Gavin King
 */
public interface AuxiliaryMapping {
	/**
	 * The name of the table to which this auxiliary mapping applies.
	 */
	String getTableName();

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
			StandardTableGroup tableGroup,
			NamedTableReference rootTableReference, EntityMappingType entityMappingType);

	JdbcMapping getJdbcMapping();

	boolean useAuxiliaryTable(LoadQueryInfluencers influencers);

	boolean isAffectedByInfluencers(LoadQueryInfluencers influencers);
}
