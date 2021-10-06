/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class CaseSimpleExpression implements Expression, DomainResultProducer {
	private final MappingModelExpressable type;
	private final Expression fixture;

	private List<WhenFragment> whenFragments = new ArrayList<>();
	private Expression otherwise;

	public CaseSimpleExpression(MappingModelExpressable type, Expression fixture) {
		this.type = type;
		this.fixture = fixture;
	}

	public CaseSimpleExpression(MappingModelExpressable type, Expression fixture, List<WhenFragment> whenFragments, Expression otherwise) {
		this.type = type;
		this.fixture = fixture;
		this.whenFragments = whenFragments;
		this.otherwise = otherwise;
	}

	public Expression getFixture() {
		return fixture;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCaseSimpleExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final JavaType javaTypeDescriptor = type.getJdbcMappings().get( 0 ).getJavaTypeDescriptor();
		return new BasicResult(
				creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
						this,
						javaTypeDescriptor,
						creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
				).getValuesArrayPosition(),
				resultVariable,
				javaTypeDescriptor
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		sqlExpressionResolver.resolveSqlSelection(
				this,
				type.getJdbcMappings().get( 0 ).getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	public List<WhenFragment> getWhenFragments() {
		return whenFragments;
	}

	public Expression getOtherwise() {
		return otherwise;
	}

	public void otherwise(Expression otherwiseExpression) {
		this.otherwise = otherwiseExpression;
	}

	public void when(Expression test, Expression result) {
		whenFragments.add( new WhenFragment( test, result ) );
	}

	public static class WhenFragment implements Serializable {
		private final Expression checkValue;
		private final Expression result;

		public WhenFragment(Expression checkValue, Expression result) {
			this.checkValue = checkValue;
			this.result = result;
		}

		public Expression getCheckValue() {
			return checkValue;
		}

		public Expression getResult() {
			return result;
		}
	}
}
