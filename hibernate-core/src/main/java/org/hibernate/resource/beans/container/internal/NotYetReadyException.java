/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
