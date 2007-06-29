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
