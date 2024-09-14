/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.HibernateException;

/**
 * Indicates a problem resolving a domain-path occurring in an order-by fragment
 *
 * @author Steve Ebersole
 */
public class PathResolutionException extends HibernateException {
	public PathResolutionException(String message) {
		super( message );
	}

	public PathResolutionException(String message, Throwable cause) {
		super( message, cause );
	}
}
