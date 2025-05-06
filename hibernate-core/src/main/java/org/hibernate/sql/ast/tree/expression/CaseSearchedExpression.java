/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * @author Steve Ebersole
 */
public class CaseSearchedExpression implements Expression, DomainResultProducer {
	private final BasicValuedMapping type;

	private List<WhenFragment> whenFragments = new ArrayList<>();
	private Expression otherwise;

	public CaseSearchedExpression(MappingModelExpressible type) {
		this.type = (BasicValuedMapping) type;
	}

	public CaseSearchedExpression(MappingModelExpressible type, List<WhenFragment> whenFragments, Expression otherwise) {
		this.type = (BasicValuedMapping) type;
		this.whenFragments = whenFragments;
		this.otherwise = otherwise;
	}

	public List<WhenFragment> getWhenFragments() {
		return whenFragments;
	}

	public Expression getOtherwise() {
		return otherwise;
	}

	public void when(Predicate predicate, Expression result) {
		whenFragments.add( new WhenFragment( predicate, result ) );
	}

	public void otherwise(Expression otherwiseExpression) {
		this.otherwise = otherwiseExpression;
		// todo : inject implied type?
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {

		final SqlSelection sqlSelection = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver()
				.resolveSqlSelection(
						this,
						type.getJdbcMapping().getJdbcJavaType(),
						null,
						creationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
				);

		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				type.getJdbcMapping()
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		sqlExpressionResolver.resolveSqlSelection(
				this,
				type.getJdbcMapping().getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCaseSearchedExpression( this );
	}

	@Override
	public MappingModelExpressible getExpressionType() {
		return type;
	}

	public static class WhenFragment implements Serializable {
		private final Predicate predicate;
		private final Expression result;

		public WhenFragment(Predicate predicate, Expression result) {
			this.predicate = predicate;
			this.result = result;
		}

		public Predicate getPredicate() {
			return predicate;
		}

		public Expression getResult() {
			return result;
		}
	}
}
