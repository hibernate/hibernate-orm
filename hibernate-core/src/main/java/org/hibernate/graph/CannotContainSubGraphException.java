/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt was made to add a (key)? sub-graph to an
 * attribute type that does not support (key)? sub-graphs.
 *
 * @author Steve Ebersole
 */
public class CannotContainSubGraphException extends HibernateException {
	public CannotContainSubGraphException(String message) {
		super( message );
	}
}
