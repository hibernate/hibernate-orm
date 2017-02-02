/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;
import org.hibernate.HibernateException;

/**
 * Indicates a problem with a service.
 *
 * @author Steve Ebersole
 */
public class ServiceException extends HibernateException {
	public ServiceException(String message, Throwable root) {
		super( message, root );
	}

	public ServiceException(String message) {
		super( message );
	}
}
