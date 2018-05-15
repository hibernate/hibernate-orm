/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Chris Cranford
 */
public class SubQuery implements Expression {
	private final QuerySpec querySpec;
	private final SqlExpressableType expressableType;

	public SubQuery(QuerySpec querySpec, SqlExpressableType expressableType) {
		this.querySpec = querySpec;
		this.expressableType = expressableType;
	}

	@Override
	public SqlExpressableType getType() {
		return expressableType;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition, BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitQuerySpec( querySpec );
	}
}
