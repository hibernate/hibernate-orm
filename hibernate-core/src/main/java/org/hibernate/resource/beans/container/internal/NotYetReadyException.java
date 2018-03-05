/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import org.hibernate.HibernateException;

/**
 * Exception indicating an attempt to access the CDI BeanManager before it is ready for use
 *
 * @author Steve Ebersole
 */
public class NotYetReadyException extends HibernateException {
	public NotYetReadyException(Throwable cause) {
		super( "CDI BeanManager not (yet) ready to use", cause );
	}
}
