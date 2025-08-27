/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.beans;

import org.hibernate.HibernateException;

/**
 * Indicates a problem dealing with {@link java.beans.BeanInfo} via the {@link BeanInfoHelper} delegate.
 *
 * @author Steve Ebersole
 */
public class BeanIntrospectionException extends HibernateException {
	public BeanIntrospectionException(String string, Throwable root) {
		super( string, root );
	}

	public BeanIntrospectionException(String s) {
		super( s );
	}
}
