/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
