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
package org.hibernate.hql.ast.util;

import antlr.collections.AST;

/**
 * A visitor for traversing an AST tree.
 *
 * @author Steve Ebersole
 */
public class NodeTraverser {
	public static interface VisitationStrategy {
		public void visit(AST node);
	}

	private final VisitationStrategy strategy;

	public NodeTraverser(VisitationStrategy strategy) {
		this.strategy = strategy;
	}

	/**
	 * Traverse the AST tree depth first.
	 * <p/>
	 * Note that the AST passed in is not visited itself.  Visitation starts
	 * with its children.
	 *
	 * @param ast
	 */
	public void traverseDepthFirst(AST ast) {
		if ( ast == null ) {
			throw new IllegalArgumentException( "node to traverse cannot be null!" );
		}
		visitDepthFirst( ast.getFirstChild() );
	}

	private void visitDepthFirst(AST ast) {
		if ( ast == null ) {
			return;
		}
		strategy.visit( ast );
		visitDepthFirst( ast.getFirstChild() );
		visitDepthFirst( ast.getNextSibling() );
	}
}
