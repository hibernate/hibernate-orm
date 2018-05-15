/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.sort;

import org.hibernate.query.sqm.tree.order.SqmSortOrder;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;

/**
 * @author Steve Ebersole
 */
public class SortSpecification implements SqlAstNode {
	private final Expression sortExpression;
	private final String collation;
	private final SqmSortOrder sortOrder;

	public SortSpecification(Expression sortExpression, String collation, SqmSortOrder sortOrder) {
		this.sortExpression = sortExpression;
		this.collation = collation;
		this.sortOrder = sortOrder;
	}

	public Expression getSortExpression() {
		return sortExpression;
	}

	public String getCollation() {
		return collation;
	}

	public SqmSortOrder getSortOrder() {
		return sortOrder;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSortSpecification( this );
	}
}
