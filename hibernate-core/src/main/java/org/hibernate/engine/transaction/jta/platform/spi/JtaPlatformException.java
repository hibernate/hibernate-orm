/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
