// $Id: AbstractSelectExpression.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Partial implementation of SelectExpression for all the nodes that aren't constructors.
 *
 * @author josh Nov 11, 2004 7:09:11 AM
 */
public abstract class AbstractSelectExpression extends HqlSqlWalkerNode implements SelectExpression {
	
	private String alias;
	
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
}
