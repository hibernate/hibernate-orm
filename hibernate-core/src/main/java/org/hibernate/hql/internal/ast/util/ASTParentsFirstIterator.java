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
public class ASTParentsFirstIterator implements Iterator {
	private AST next;
	private AST tree;
	private LinkedList<AST> parents = new LinkedList<AST>();

	public void remove() {
		throw new UnsupportedOperationException( "remove() is not supported" );
	}

	public boolean hasNext() {
		return next != null;
	}

	public Object next() {
		return nextNode();
	}

	public ASTParentsFirstIterator(AST tree) {
		this.tree = next = tree;
	}

	public AST nextNode() {
		AST current = next;
		if ( next != null ) {
			AST child = next.getFirstChild();
			if ( child == null ) {
				AST sibling = next.getNextSibling();
				if ( sibling == null ) {
					AST parent = pop();
					while ( parent != null && parent.getNextSibling() == null ) {
						parent = pop();
					}
					next = ( parent != null ) ? parent.getNextSibling() : null;
				}
				else {
					next = sibling;
				}
			}
			else {
				if ( next != tree ) {
					push( next );
				}
				next = child;
			}
		}
		return current;
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
