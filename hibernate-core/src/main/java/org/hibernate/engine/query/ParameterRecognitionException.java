/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query;

import org.hibernate.HibernateException;
import org.hibernate.query.sql.spi.ParameterRecognizer;

/**
 * Indicates a problem during parameter recognition via {@link ParameterRecognizer}
 *
 * @author Steve Ebersole
 */
public class ParameterRecognitionException extends HibernateException {
	public ParameterRecognitionException(String message) {
		super( message );
	}

	public ParameterRecognitionException(String message, Throwable cause) {
		super( message, cause );
	}
}
