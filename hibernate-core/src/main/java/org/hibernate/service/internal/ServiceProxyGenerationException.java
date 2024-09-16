/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import org.hibernate.HibernateException;

/**
 * Indicates a problem generating a service proxy
 *
 * @author Steve Ebersole
 */
public class ServiceProxyGenerationException extends HibernateException {
	public ServiceProxyGenerationException(String string, Throwable root) {
		super( string, root );
	}

	public ServiceProxyGenerationException(Throwable root) {
		super( root );
	}
}
