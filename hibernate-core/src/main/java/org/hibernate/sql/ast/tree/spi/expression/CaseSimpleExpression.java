/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.Selectable;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class CaseSimpleExpression implements Expression, Selectable, SqlExpressable, DomainResultProducer {
	private final SqlExpressableType type;
	private final Expression fixture;

	private List<WhenFragment> whenFragments = new ArrayList<>();
	private Expression otherwise;

	public CaseSimpleExpression(SqlExpressableType type, Expression fixture) {
		this.type = type;
		this.fixture = fixture;
	}

	public Expression getFixture() {
		return fixture;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getExpressableType()
		);
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitCaseSimpleExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						this,
						getType().getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getType()
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

	public static class WhenFragment {
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
