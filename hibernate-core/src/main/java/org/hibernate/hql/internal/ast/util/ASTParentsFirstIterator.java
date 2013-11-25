/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
