/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction;

import org.hibernate.HibernateException;

/**
 * Wraps an exception thrown from a "local synchronization" (one registered in the SynchronizationRegistry).
 *
 * @author Steve Ebersole
 */
public class LocalSynchronizationException extends HibernateException {
	public LocalSynchronizationException(String message, Throwable cause) {
		super( message, cause );
	}
}
