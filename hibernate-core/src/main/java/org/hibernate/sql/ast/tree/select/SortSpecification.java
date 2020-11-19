/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import org.hibernate.NullPrecedence;
import org.hibernate.SortOrder;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Collate;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class SortSpecification implements SqlAstNode {
	private final Expression sortExpression;
	private final SortOrder sortOrder;
	private final NullPrecedence nullPrecedence;

	public SortSpecification(Expression sortExpression, String collation, SortOrder sortOrder) {
		this( sortExpression, collation, sortOrder, NullPrecedence.NONE );
	}

	public SortSpecification(Expression sortExpression, String collation, SortOrder sortOrder, NullPrecedence nullPrecedence) {
		if ( collation == null ) {
			this.sortExpression = sortExpression;
		}
		else {
			this.sortExpression = new Collate( sortExpression, collation );
		}
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
	}

	public Expression getSortExpression() {
		return sortExpression;
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSortSpecification( this );
	}
}
