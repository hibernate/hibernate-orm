/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

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
	public SqlExpressableType getExpressableType() {
		return stringToSearch.getExpressable().getExpressableType();
	}

	@Override
	public SqlExpressableType getType() {
		return getExpressableType();
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
}
