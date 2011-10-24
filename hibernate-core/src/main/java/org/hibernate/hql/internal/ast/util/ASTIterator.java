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
import java.util.Iterator;
import java.util.LinkedList;

import antlr.collections.AST;

/**
 * Depth first iteration of an ANTLR AST.
 *
 * @author josh
 */
public class ASTIterator implements Iterator {
	private AST next, current;
	private LinkedList parents = new LinkedList();

	public void remove() {
		throw new UnsupportedOperationException( "remove() is not supported" );
	}

	public boolean hasNext() {
		return next != null;
	}

	public Object next() {
		return nextNode();
	}

	public ASTIterator(AST tree) {
		next = tree;
		down();
	}

	public AST nextNode() {
		current = next;
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
			return ( AST ) parents.removeFirst();
		}
	}

}
