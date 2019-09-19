/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class EmbeddableValuedPathInterpretation<T> implements AssignableSqmPathInterpretation<T>, DomainResultProducer<T> {

	/**
	 * Static factory
	 */
	public static <T> EmbeddableValuedPathInterpretation<T> from(
			SqmEmbeddedValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState,
			SemanticQueryWalker sqmWalker) {
		final TableGroup tableGroup = SqmMappingModelHelper.resolveLhs( sqmPath.getNavigablePath(), sqlAstCreationState );
		tableGroup.getModelPart().prepareAsLhs( sqmPath.getNavigablePath(), sqlAstCreationState );

		final EmbeddableValuedModelPart mapping = (EmbeddableValuedModelPart) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				null
		);

		return new EmbeddableValuedPathInterpretation<>( sqmPath, mapping, tableGroup );
	}


	private final SqmEmbeddedValuedSimplePath<T> sqmPath;
	private final EmbeddableValuedModelPart mapping;
	private final TableGroup tableGroup;

	public EmbeddableValuedPathInterpretation(
			SqmEmbeddedValuedSimplePath<T> sqmPath,
			EmbeddableValuedModelPart mapping,
			TableGroup tableGroup) {
		this.sqmPath = sqmPath;
		this.mapping = mapping;
		this.tableGroup = tableGroup;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return sqmPath.getNavigablePath();
	}

	@Override
	public SqmPathSource<T> getSqmPathSource() {
		return sqmPath.getReferencedPathSource();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public DomainResultProducer<T> getDomainResultProducer(
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		return this;
	}

	@Override
	public DomainResult<T> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		return mapping.createDomainResult( getNavigablePath(), tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		mapping.applySqlSelections( getNavigablePath(), tableGroup, creationState );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPathInterpretation

	@Override
	public Expression toSqlExpression(
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		return mapping.toSqlExpression( tableGroup, clause, walker, sqlAstCreationState );
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
	public String toString() {
		return "EmbeddableValuedPathInterpretation(" + sqmPath.getNavigablePath().getFullPath() + ')';
	}
}
