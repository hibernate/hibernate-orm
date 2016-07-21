/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class CaseSimpleExpression extends SelfReadingExpressionSupport implements Expression {
	private final Type type;
	private final Expression fixture;

	private List<WhenFragment> whenFragments = new ArrayList<WhenFragment>();
	private Expression otherwise;

	public CaseSimpleExpression(Type type, Expression fixture) {
		this.type = type;
		this.fixture = fixture;
	}

	public Expression getFixture() {
		return fixture;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitCaseSimpleExpression( this );
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
