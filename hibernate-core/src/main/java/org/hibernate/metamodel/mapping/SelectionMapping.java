/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * A column mapping.
 *
 * @author Christian Beikov
 */
public interface SelectionMapping {
	String getContainingTableExpression();
	String getSelectionExpression();
	String getCustomReadExpression();
	String getCustomWriteExpression();
	boolean isFormula();
	JdbcMapping getJdbcMapping();
}
