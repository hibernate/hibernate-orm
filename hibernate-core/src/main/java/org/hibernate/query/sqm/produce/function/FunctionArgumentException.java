/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.SemanticException;

/**
 * Represents a problem with the argument list of a function in HQL/JPQL.
 *
 * @author Gavin King
 *
 * @since 6.3
 */
public class FunctionArgumentException extends SemanticException {
	public FunctionArgumentException(String message) {
		super(message);
	}
}
