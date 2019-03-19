/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.sort;

import org.hibernate.SortOrder;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Steve Ebersole
 */
public class SortSpecification implements SqlAstNode {
	private final Expression sortExpression;
	private final String collation;
	private final SortOrder sortOrder;

	public SortSpecification(Expression sortExpression, String collation, SortOrder sortOrder) {
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

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSortSpecification( this );
	}
}
