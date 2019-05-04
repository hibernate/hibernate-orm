/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

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
public class ReplaceFunction extends AbstractStandardFunction implements StandardFunction {
	private final Expression stringToSearch;
	private final Expression patternString;
	private final Expression replacementString;
	private final SqlExpressableType sqlExpressableType;

	public ReplaceFunction(
			Expression stringToSearch,
			Expression patternString,
			Expression replacementString,
			SqlExpressableType sqlExpressableType) {
		this.patternString = patternString;
		this.stringToSearch = stringToSearch;
		this.replacementString = replacementString;
		this.sqlExpressableType = sqlExpressableType;
	}

	public Expression getPatternString() {
		return patternString;
	}

	public Expression getStringToSearch() {
		return stringToSearch;
	}

	public Expression getReplacementString() {
		return replacementString;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitReplaceFunction( this );
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return sqlExpressableType;
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
