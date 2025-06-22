/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.spi;

import org.hibernate.HibernateException;

/**
 * Indicates that the persister to use is not known and could not be determined.
 *
 * @author Steve Ebersole
 */
public class UnknownPersisterException extends HibernateException {
	public UnknownPersisterException(String s) {
		super( s );
	}

	public UnknownPersisterException(String string, Throwable root) {
		super( string, root );
	}
}
