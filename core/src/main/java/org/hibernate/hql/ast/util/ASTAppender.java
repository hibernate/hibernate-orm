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

import antlr.ASTFactory;
import antlr.collections.AST;

/**
 * Appends child nodes to a parent efficiently.
 *
 * @author Joshua Davis
 */
public class ASTAppender {
	private AST parent;
	private AST last;
	private ASTFactory factory;

	public ASTAppender(ASTFactory factory, AST parent) {
		this( parent );
		this.factory = factory;
	}

	public ASTAppender(AST parent) {
		this.parent = parent;
		this.last = ASTUtil.getLastChild( parent );
	}

	public AST append(int type, String text, boolean appendIfEmpty) {
		if ( text != null && ( appendIfEmpty || text.length() > 0 ) ) {
			return append( factory.create( type, text ) );
		}
		else {
			return null;
		}
	}

	public AST append(AST child) {
		if ( last == null ) {
			parent.setFirstChild( child );
		}
		else {
			last.setNextSibling( child );
		}
		last = child;
		return last;
	}
}
