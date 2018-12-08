/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Path;

import org.hibernate.query.NavigablePath;

/**
 * API extension to the JPA {@link Path} contract
 *
 * @author Steve Ebersole
 */
public interface JpaPath<T> extends JpaExpression<T>, Path<T> {
	/**
	 * Get this path's NavigablePath
	 */
	NavigablePath getNavigablePath();

	/**
	 * The source (think "left hand side") of this path
	 */
	<X> JpaPathSource<X> getSource();

	/**
	 * Support for JPA's explicit (TREAT) down-casting.
	 */
	<S extends T> JpaPath<S> treatAs(Class<S> treatJavaType) throws PathException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	default JpaPathSource<?> getParentPath() {
		return getSource();
	}
}
