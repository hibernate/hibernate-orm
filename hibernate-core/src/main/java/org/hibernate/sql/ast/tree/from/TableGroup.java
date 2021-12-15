/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
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
		return getPrimaryTableReference() instanceof DerivedTableReference
				&& ( (DerivedTableReference) getPrimaryTableReference() ).isLateral();
	}

	void addTableGroupJoin(TableGroupJoin join);

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

	default ColumnReference locateColumnReferenceByName(String name) {
		throw new UnsupportedOperationException(
				"Cannot call #locateColumnReferenceByName on this type of TableGroup"
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
}
