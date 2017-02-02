/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal.stax;

import org.hibernate.HibernateException;

/**
 * An error using XML infrastructure (jaxp, stax, etc).
 *
 * @author Steve Ebersole
 */
public class XmlInfrastructureException extends HibernateException {
	public XmlInfrastructureException(String message) {
		super( message );
	}

	public XmlInfrastructureException(String message, Throwable root) {
		super( message, root );
	}
}
