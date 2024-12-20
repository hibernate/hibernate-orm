/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Group together {@link TableReference} references related to a single entity or
 * collection, along with joins to other TableGroups
 *
 * @author Steve Ebersole
 */
public interface TableGroup extends SqlAstNode, ColumnReferenceQualifier, SqmPathInterpretation, DomainResultProducer {
	NavigablePath getNavigablePath();

	/**
	 * If we want to use CTE for TableGroup rendering we will need to know the
	 * alias we can use for the group
	 */
	String getGroupAlias();

	ModelPartContainer getModelPart();

	String getSourceAlias();

	List<TableGroupJoin> getTableGroupJoins();

	List<TableGroupJoin> getNestedTableGroupJoins();

	boolean canUseInnerJoins();

	default boolean isLateral() {
		return false;
	}

	void addTableGroupJoin(TableGroupJoin join);

	/**
	 * Adds the given table group join before a join as found via the given navigable path.
	 */
	void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join);

	/**
	 * A nested table group join is a join against a table group,
	 * that is ensured to be joined against the primary table reference and table reference joins in isolation,
	 * prior to doing other table group joins e.g.
	 *
	 * <code>
	 * select *
	 * from entity1 e
	 * left join (
	 * 	 collection_table c1
	 * 	 join association a on a.id = c1.target_id
	 * ) on c1.entity_id = e.id and c1.key = 1
	 * </code>
	 *
	 * is modeled as
	 *
	 * <code>
	 * TableGroup(
	 *     primaryTableReference = TableReference(entity1, e),
	 *     tableGroupJoins = [
	 *         TableGroupJoin(
	 *             TableGroup(
	 *                 primaryTableReference = TableReference(collection_table, c1),
	 *                 nestedTableGroupJoins = [
	 *                     TableGroupJoin(
	 *                         TableGroup(
	 *                             primaryTableReference = TableReference(association, a)
	 *                         )
	 *                     )
	 *                 ]
	 *             )
	 *         )
	 *     ]
	 * )
	 * </code>
	 *
	 * This is necessary to correctly retain the cardinality of an HQL join like e.g.
	 *
	 * <code>
	 *     from Entity1 e left join e.collectionAssociation c on key(c) = 1
	 * </code>
	 */
	void addNestedTableGroupJoin(TableGroupJoin join);

	void visitTableGroupJoins(Consumer<TableGroupJoin> consumer);

	void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer);

	void applyAffectedTableNames(Consumer<String> nameCollector);

	TableReference getPrimaryTableReference();

	List<TableReferenceJoin> getTableReferenceJoins();

	@Override
	default DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return getModelPart().createDomainResult(
				getNavigablePath(),
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	default void applySqlSelections(DomainResultCreationState creationState) {
		final ModelPartContainer modelPart = getModelPart();
		final ModelPart modelPartToApply;
		if ( modelPart instanceof EntityValuedModelPart ) {
			modelPartToApply = ( (EntityValuedModelPart) modelPart ).getEntityMappingType();
		}
		else {
			modelPartToApply = modelPart;
		}
		modelPartToApply.applySqlSelections(
				getNavigablePath(),
				creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( getNavigablePath() ),
				creationState
		);
	}

	@Override
	default void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTableGroup( this );
	}

	default boolean isRealTableGroup() {
		return false;
	}

	default boolean isFetched() {
		return false;
	}

	/**
	 * If this is a lazy table group, it may report that it is not initialized,
	 * which would also mean that a join referring to this table group should not be rendered.
	 */
	default boolean isInitialized() {
		return true;
	}

	default TableGroupJoin findCompatibleJoin(
			TableGroupJoinProducer joinProducer,
			SqlAstJoinType requestedJoinType) {
		// We don't look into nested table group joins as that wouldn't be "compatible"
		for ( TableGroupJoin join : getTableGroupJoins() ) {
			// Compatibility obviously requires the same model part but also join type compatibility
			// Note that if the requested join type is left, we can also use an existing inner join
			// The other case, when the requested join type is inner and there is an existing left join,
			// is not compatible though because the cardinality is different.
			// We could reuse the join though if we alter the join type to INNER, but that's an optimization for later
			final SqlAstJoinType joinType = join.getJoinType();
			if ( join.getJoinedGroup().getModelPart() == joinProducer
					&& ( requestedJoinType == joinType || requestedJoinType == SqlAstJoinType.LEFT && joinType == SqlAstJoinType.INNER ) ) {
				// If there is an existing inner join, we can always use that as a new join can never produce results
				// regardless of the join type or predicate since the LHS is the same table group
				// If this is a left join though, we have to check if the predicate is simply the association predicate
				if ( joinType == SqlAstJoinType.INNER || joinProducer.isSimpleJoinPredicate( join.getPredicate() ) ) {
					return join;
				}
			}
		}
		return null;
	}

	default TableGroup findCompatibleJoinedGroup(
			TableGroupJoinProducer joinProducer,
			SqlAstJoinType requestedJoinType) {
		final TableGroupJoin compatibleJoin = findCompatibleJoin( joinProducer, requestedJoinType );
		return compatibleJoin != null ? compatibleJoin.getJoinedGroup() : null;
	}

	default TableGroupJoin findTableGroupJoin(TableGroup tableGroup) {
		for ( TableGroupJoin join : getTableGroupJoins() ) {
			if ( join.getJoinedGroup() == tableGroup ) {
				return join;
			}
		}
		for ( TableGroupJoin join : getNestedTableGroupJoins() ) {
			if ( join.getJoinedGroup() == tableGroup ) {
				return join;
			}
		}
		return null;
	}

	default boolean hasRealJoins() {
		for ( TableGroupJoin join : getTableGroupJoins() ) {
			final TableGroup joinedGroup = join.getJoinedGroup();
			if ( joinedGroup.isInitialized() && !joinedGroup.isVirtual() || joinedGroup.hasRealJoins() ) {
				return true;
			}
		}
		for ( TableGroupJoin join : getNestedTableGroupJoins() ) {
			final TableGroup joinedGroup = join.getJoinedGroup();
			if ( joinedGroup.isInitialized() && !joinedGroup.isVirtual() || joinedGroup.hasRealJoins() ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Utility method that indicates weather this table group is {@linkplain VirtualTableGroup virtual} or not
	 */
	default boolean isVirtual() {
		return false;
	}

	default TableReference findTableReference(String identificationVariable) {
		final TableReference primaryTableReference = getPrimaryTableReference();
		if ( identificationVariable.equals( primaryTableReference.getIdentificationVariable() ) ) {
			return primaryTableReference;
		}
		for ( TableReferenceJoin tableReferenceJoin : getTableReferenceJoins() ) {
			final NamedTableReference joinedTableReference = tableReferenceJoin.getJoinedTableReference();
			if ( identificationVariable.equals( joinedTableReference.getIdentificationVariable() ) ) {
				return joinedTableReference;
			}
		}

		return null;
	}
}
