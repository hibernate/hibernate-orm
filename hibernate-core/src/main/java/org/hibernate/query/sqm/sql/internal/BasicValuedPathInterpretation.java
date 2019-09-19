/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqlExpressionResolver;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class BasicValuedPathInterpretation<T> implements AssignableSqmPathInterpretation<T>, DomainResultProducer<T> {
	/**
	 * Static factory
	 */
	public static <T> BasicValuedPathInterpretation<T> from(
			SqmBasicValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState,
			SemanticQueryWalker sqmWalker) {
		final TableGroup tableGroup = SqmMappingModelHelper.resolveLhs( sqmPath.getNavigablePath(), sqlAstCreationState );
		tableGroup.getModelPart().prepareAsLhs( sqmPath.getNavigablePath(), sqlAstCreationState );

		final BasicValuedModelPart mapping = (BasicValuedModelPart) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				null
		);

		final ColumnReference columnReference = (ColumnReference) sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey(
						mapping.getContainingTableExpression(),
						mapping.getMappedColumnExpression()
				),
				sacs -> new ColumnReference(
						mapping.getMappedColumnExpression(),
						tableGroup.resolveTableReference( mapping.getContainingTableExpression() )
								.getIdentificationVariable(),
						mapping.getJdbcMapping(),
						sqlAstCreationState.getCreationContext().getSessionFactory()
				)
		);

		return new BasicValuedPathInterpretation<>( columnReference, sqmPath, mapping, tableGroup );
	}

	private final ColumnReference columnReference;

	private final SqmBasicValuedSimplePath<T> sqmPath;
	private final BasicValuedModelPart mapping;
	private final TableGroup tableGroup;

	private BasicValuedPathInterpretation(
			ColumnReference columnReference,
			SqmBasicValuedSimplePath<T> sqmPath,
			BasicValuedModelPart mapping,
			TableGroup tableGroup) {
		assert columnReference != null;
		this.columnReference = columnReference;

		assert sqmPath != null;
		this.sqmPath = sqmPath;

		assert mapping != null;
		this.mapping = mapping;

		assert tableGroup != null;
		this.tableGroup = tableGroup;


	}

	@Override
	public SqmPath<T> getInterpretedSqmPath() {
		return sqmPath;
	}

	@Override
	public ModelPart getExpressionType() {
		return mapping;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return mapping.createDomainResult( getNavigablePath(), tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		mapping.applySqlSelections( getNavigablePath(), tableGroup, creationState );
	}

	@Override
	public void applySqlAssignments(
			Expression newValueExpression,
			AssignmentContext assignmentProcessingState,
			Consumer<Assignment> assignmentConsumer,
			SqlAstCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		columnReference.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "BasicValuedPathInterpretation(" + sqmPath.getNavigablePath().getFullPath() + ')';
	}
}
