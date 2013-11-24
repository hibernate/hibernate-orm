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
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Partial implementation of SelectExpression for all the nodes that aren't constructors.
 *
 * @author Joshua Davis
 */
public abstract class AbstractSelectExpression extends HqlSqlWalkerNode implements SelectExpression {
	
	private String alias;
	private int scalarColumnIndex = -1;
	
	public final void setAlias(String alias) {
		this.alias = alias;
	}
	
	public final String getAlias() {
		return alias;
	}

	public boolean isConstructor() {
		return false;
	}

	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	public FromElement getFromElement() {
		return null;
	}

	public boolean isScalar() throws SemanticException {
		// Default implementation:
		// If this node has a data type, and that data type is not an association, then this is scalar.
		Type type = getDataType();
		return type != null && !type.isAssociationType();	// Moved here from SelectClause [jsd]
	}

	public void setScalarColumn(int i) throws SemanticException {
		this.scalarColumnIndex = i;
		setScalarColumnText( i );
	}

	public int getScalarColumnIndex() {
		return scalarColumnIndex;
	}
}
