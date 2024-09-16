/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class ParameterStrategyException extends HibernateException {
	public ParameterStrategyException(String message) {
		super( message );
	}
}
