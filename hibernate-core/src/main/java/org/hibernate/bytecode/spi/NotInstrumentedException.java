/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a condition where an instrumented/enhanced class was expected, but the class was not
 * instrumented/enhanced.
 *
 * @author Steve Ebersole
 */
public class NotInstrumentedException extends HibernateException {
	/**
	 * Constructs a NotInstrumentedException
	 *
	 * @param message Message explaining the exception condition
	 */
	public NotInstrumentedException(String message) {
		super( message );
	}
}
