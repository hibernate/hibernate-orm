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
package org.hibernate.hql.ast.tree;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents 'elements()' or 'indices()'.
 *
 * @author josh
 */
public class CollectionFunction extends MethodNode implements DisplayableNode {
	public void resolve(boolean inSelect) throws SemanticException {
		initializeMethodNode( this, inSelect );
		if ( !isCollectionPropertyMethod() ) {
			throw new SemanticException( this.getText() + " is not a collection property name!" );
		}
		AST expr = getFirstChild();
		if ( expr == null ) {
			throw new SemanticException( this.getText() + " requires a path!" );
		}
		resolveCollectionProperty( expr );
	}

	protected void prepareSelectColumns(String[] selectColumns) {
		// we need to strip off the embedded parens so that sql-gen does not double these up
		String subselect = selectColumns[0].trim();
		if ( subselect.startsWith( "(") && subselect.endsWith( ")" ) ) {
			subselect = subselect.substring( 1, subselect.length() -1 );
		}
		selectColumns[0] = subselect;
	}
}
