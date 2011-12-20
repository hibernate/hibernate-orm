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
import antlr.SemanticException;

import org.hibernate.type.Type;

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
