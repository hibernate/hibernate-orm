/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

import antlr.ASTFactory;

/**
 * Acts as a {@link ASTFactory} for injecting our specific AST node classes into the Antlr generated trees.
 *
 * @author Steve Ebersole
 */
public class Factory extends ASTFactory implements OrderByTemplateTokenTypes {
	@Override
	public Class getASTNodeType(int i) {
		switch ( i ) {
			case ORDER_BY:
				return OrderByFragment.class;
			case SORT_SPEC:
				return SortSpecification.class;
			case ORDER_SPEC:
				return OrderingSpecification.class;
			case COLLATE:
				return CollationSpecification.class;
			case SORT_KEY:
				return SortKey.class;
			default:
				return NodeSupport.class;
		}
	}
}
