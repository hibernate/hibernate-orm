/*
 * SPDX-License-Identifier: Apache-2.0
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
