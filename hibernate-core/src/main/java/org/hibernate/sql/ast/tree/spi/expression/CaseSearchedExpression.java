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
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class CaseSearchedExpression implements Expression, SqlExpressable, DomainResultProducer {
	private final SqlExpressableType type;

	private List<WhenFragment> whenFragments = new ArrayList<>();
	private Expression otherwise;

	public CaseSearchedExpression(SqlExpressableType type) {
		this.type = type;
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
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlExpressableType getType() {
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

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitCaseSearchedExpression( this );
	}

	public static class WhenFragment {
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
