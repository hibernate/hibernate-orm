/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

/**
 * Describes the allowable ways in which entity references
 * can be handled as dynamic-instantiation (ctor result) argument.
 * <p/>
 * NOTE that this only applies to Hibernate extension to JPA.  JPA
 * does not allow ctor-result arguments to be anything other than
 * scalar values (column result).
 *
 * @author Steve Ebersole
 */
public enum ConstructorEntityArgumentMode {
	/**
	 * The id of the entity will be used as the ctor arg.  This
	 * is the legacy Hibernate behavior.
	 */
	SCALAR,
	/**
	 * The entity reference will be passed as the ctor arg.  Whether
	 * the entity ref is initialized or not depends on whether
	 * the entity is fetched in the query or is otherwise already
	 * part of the persistence context.
	 */
	ENTITY,
	/**
	 * This mode says to chose based on what ctors are available on
	 * the target class.
	 */
	CHOOSE
}
