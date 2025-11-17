/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
