/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

/**
 * SqmPath specialization for an SqmPath that wraps another SqmPath
 *
 * @author Steve Ebersole
 */
public interface SqmPathWrapper extends SqmPath {
	/**
	 * Access the wrapped SqmPath.
	 */
	SqmPath getWrappedPath();
}
