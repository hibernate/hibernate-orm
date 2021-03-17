/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;
import org.hibernate.HibernateException;

/**
 * Indicates a problem processing service dependencies.
 *
 * @author Steve Ebersole
 */
public class ServiceDependencyException extends HibernateException {
	public ServiceDependencyException(String string, Throwable root) {
		super( string, root );
	}

	public ServiceDependencyException(String s) {
		super( s );
	}
}
