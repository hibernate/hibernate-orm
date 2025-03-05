/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.query.SemanticException;

/**
 * @author Steve Ebersole
 */
public class LiteralNumberFormatException extends SemanticException {
	public LiteralNumberFormatException(String message) {
		super( message );
	}

	public LiteralNumberFormatException(String message, Exception cause) {
		super( message, cause );
	}
}
