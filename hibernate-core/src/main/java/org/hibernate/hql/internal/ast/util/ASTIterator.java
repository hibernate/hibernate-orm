/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.util;

import java.util.Iterator;
import java.util.LinkedList;

import antlr.collections.AST;

/**
 * Depth first iteration of an ANTLR AST.
 *
 * @author josh
 */
public class ASTIterator implements Iterator {
	private AST next;
	private LinkedList<AST> parents = new LinkedList<AST>();

	/**
	 * Constructs an Iterator for depth-first iteration of an AST
	 *
	 * @param tree THe tree whose nodes are to be iterated
	 */
	public ASTIterator(AST tree) {
		next = tree;
		down();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException( "remove() is not supported" );
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public Object next() {
		return nextNode();
	}


	/**
	 * Get the next node to be returned from iteration.
	 *
	 * @return The next node.
	 */
	public AST nextNode() {
		AST current = next;
		if ( next != null ) {
			AST nextSibling = next.getNextSibling();
			if ( nextSibling == null ) {
				next = pop();
			}
			else {
				next = nextSibling;
				down();
			}
		}
		return current;
	}

	private void down() {
		while ( next != null && next.getFirstChild() != null ) {
			push( next );
			next = next.getFirstChild();
		}
	}

	private void push(AST parent) {
		parents.addFirst( parent );
	}

	private AST pop() {
		if ( parents.size() == 0 ) {
			return null;
		}
		else {
			return parents.removeFirst();
		}
	}

}
