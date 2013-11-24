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
package org.hibernate.hql.internal.ast.util;

import java.util.ArrayDeque;
import java.util.Deque;

import antlr.collections.AST;

/**
 * A visitor for traversing an AST tree.
 * 
 * @author Steve Ebersole
 * @author Philip R. "Pib" Burns.
 * @author Strong Liu
 * 
 */
public class NodeTraverser {
	public static interface VisitationStrategy {
		public void visit( AST node );
	}

	private final VisitationStrategy strategy;

	public NodeTraverser( VisitationStrategy strategy ) {
		this.strategy = strategy;
	}

	/**
	 * Traverse the AST tree depth first.
	 * 
	 * @param ast
	 *            Root node of subtree to traverse.
	 * 
	 *            <p>
	 *            Note that the AST passed in is not visited itself. Visitation
	 *            starts with its children.
	 *            </p>
	 */
	public void traverseDepthFirst( AST ast ) {
		if ( ast == null ) {
			throw new IllegalArgumentException(
					"node to traverse cannot be null!" );
		}
		visitDepthFirst( ast.getFirstChild() );
	}

	private void visitDepthFirst(AST ast) {
		if ( ast == null ) {
			return;
		}
		Deque<AST> stack = new ArrayDeque<AST>();
		stack.addLast( ast );
		while ( !stack.isEmpty() ) {
			ast = stack.removeLast();
			strategy.visit( ast );
			if ( ast.getNextSibling() != null ) {
				stack.addLast( ast.getNextSibling() );
			}
			if ( ast.getFirstChild() != null ) {
				stack.addLast( ast.getFirstChild() );
			}
		}
	}


}
