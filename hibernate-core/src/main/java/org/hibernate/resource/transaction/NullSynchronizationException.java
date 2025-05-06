/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to register a null synchronization.  Basically a glorified {@link NullPointerException}
 *
 * @author Steve Ebersole
 */
public class NullSynchronizationException extends HibernateException {
	public NullSynchronizationException() {
		this( "Synchronization to register cannot be null" );
	}

	public NullSynchronizationException(String s) {
		super( s );
	}
}
