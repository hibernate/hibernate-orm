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
import org.hibernate.sql.results.internal.SqlSelectionImpl;
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

	public QuerySpec getQuerySpec() {
		return querySpec;
	}

	@Override
	public SqlExpressableType getType() {
		// todo (6.0) : this makes it difficult to support tuple subqueries as that would be multiple SqlExpressable as the type
		//		for now we just support single-selection SubQuery
		return expressableType;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl( jdbcPosition, valuesArrayPosition, this, expressableType );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSubQuery( this );
	}
}
