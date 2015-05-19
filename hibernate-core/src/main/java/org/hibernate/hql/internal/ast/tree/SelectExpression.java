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
 * Represents an element of a projection list, i.e. a select expression.
 *
 * @author josh
 */
public interface SelectExpression {
	/**
	 * Returns the data type of the select expression.
	 *
	 * @return The data type of the select expression.
	 */
	Type getDataType();

	/**
	 * Appends AST nodes that represent the columns after the current AST node.
	 * (e.g. 'as col0_O_')
	 *
	 * @param i The index of the select expression in the projection list.
	 *
	 * @throws antlr.SemanticException if a semantic error occurs
	 */
	void setScalarColumnText(int i) throws SemanticException;

	/**
	 * Sets the index and text for select expression in the projection list.
	 *  
	 * @param i The index of the select expression in the projection list.
	 *
	 * @throws SemanticException if a semantic error occurs
	 */
	void setScalarColumn(int i) throws SemanticException;

	/**
	 * Gets index of the select expression in the projection list.
	 *
	 * @return The index of the select expression in the projection list.
	 */
	int getScalarColumnIndex();
	
	/**
	 * Returns the FROM element that this expression refers to.
	 *
	 * @return The FROM element.
	 */
	FromElement getFromElement();

	/**
	 * Returns true if the element is a constructor (e.g. new Foo).
	 *
	 * @return true if the element is a constructor (e.g. new Foo).
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	boolean isConstructor();

	/**
	 * Returns true if this select expression represents an entity that can be returned.
	 *
	 * @return true if this select expression represents an entity that can be returned.
	 *
	 * @throws SemanticException if a semantic error occurs
	 */
	boolean isReturnableEntity() throws SemanticException;

	/**
	 * Sets the text of the node.
	 *
	 * @param text the new node text.
	 */
	void setText(String text);

	boolean isScalar() throws SemanticException;
	
	void setAlias(String alias);
	String getAlias();
}
