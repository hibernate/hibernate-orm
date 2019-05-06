/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * The standard locate function SQL AST expression
 *
 * @author Steve Ebersole
 */
public class LocateFunction extends AbstractFunction {
	private final Expression patternString;
	private final Expression stringToSearch;
	private final Expression startPosition;
	private final SqlExpressableType sqlExpressableType;

	public LocateFunction(
			Expression patternString,
			Expression stringToSearch,
			Expression startPosition,
			SqlExpressableType sqlExpressableType) {
		this.patternString = patternString;
		this.stringToSearch = stringToSearch;
		this.startPosition = startPosition;
		this.sqlExpressableType = sqlExpressableType;
	}

	public Expression getPatternString() {
		return patternString;
	}

	public Expression getStringToSearch() {
		return stringToSearch;
	}

	public Expression getStartPosition() {
		return startPosition;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitLocateFunction( this );
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return sqlExpressableType;
	}

	@Override
	public SqlExpressableType getType() {
		return getExpressableType();
	}

}
