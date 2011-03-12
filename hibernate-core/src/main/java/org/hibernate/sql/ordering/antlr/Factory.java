/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.sql.ordering.antlr;

import antlr.ASTFactory;

/**
 * Acts as a {@link ASTFactory} for injecting our specific AST node classes into the Antlr generated trees.
 *
 * @author Steve Ebersole
 */
public class Factory extends ASTFactory implements OrderByTemplateTokenTypes {
	/**
	 * {@inheritDoc}
	 */
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
