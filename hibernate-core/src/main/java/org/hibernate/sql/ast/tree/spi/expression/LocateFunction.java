/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * The standard locate function SQL AST expression
 *
 * @author Steve Ebersole
 */
public class LocateFunction extends AbstractStandardFunction implements StandardFunction {
	private final Expression patternString;
	private final Expression stringToSearch;
	private final Expression startPosition;

	public LocateFunction(
			Expression patternString,
			Expression stringToSearch,
			Expression startPosition) {
		this.patternString = patternString;
		this.stringToSearch = stringToSearch;
		this.startPosition = startPosition;
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
	public BasicValuedExpressableType getType() {
		return (BasicValuedExpressableType) stringToSearch.getType();
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return new SqlSelectionImpl(
				jdbcPosition,
				this,
				getType().getBasicType().getSqlSelectionReader()
		);
	}
}
