/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem interacting with the underlying JTA platform.
 *
 * @author Steve Ebersole
 */
public class JtaPlatformException extends HibernateException {
	public JtaPlatformException(String s) {
		super( s );
	}

	public JtaPlatformException(String string, Throwable root) {
		super( string, root );
	}
}
