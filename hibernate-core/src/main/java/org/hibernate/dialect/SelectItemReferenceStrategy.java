/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Strategies for referring to a select item.
 *
 * @author Christian Beikov
 */
public enum SelectItemReferenceStrategy {
	/**
	 * The default strategy i.e. render the expression again.
	 */
	EXPRESSION,
	/**
	 * Refer to the item via its alias.
	 */
	ALIAS,
	/**
	 * Refer to the item via its position.
	 */
	POSITION;
}
