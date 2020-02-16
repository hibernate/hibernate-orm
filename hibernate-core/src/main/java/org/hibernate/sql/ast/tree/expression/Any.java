/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * @author Gavin King
 */
public class Any implements Expression {

	private QuerySpec subquery;
	private MappingModelExpressable<?> type;

	public Any(QuerySpec subquery, MappingModelExpressable<?> type) {
		this.subquery = subquery;
		this.type = type;
	}

	public QuerySpec getSubquery() {
		return subquery;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitAny( this );
	}
}
