/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service;
import org.hibernate.HibernateException;

/**
 * Indicates that an unkown service was requested from the registry.
 *
 * @author Steve Ebersole
 */
public class UnknownServiceException extends HibernateException {
	public final Class serviceRole;

	public UnknownServiceException(Class serviceRole) {
		super( "Unknown service requested [" + serviceRole.getName() + "]" );
		this.serviceRole = serviceRole;
	}

	public Class getServiceRole() {
		return serviceRole;
	}
}
