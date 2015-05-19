/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
