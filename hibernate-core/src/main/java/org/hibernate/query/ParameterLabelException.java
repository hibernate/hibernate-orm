/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

/**
 * Indicates a problem with the labelling of query parameters.
 *
 * @author Gavin King
 *
 * @since 6.3
 */
public class ParameterLabelException extends SemanticException {
	public ParameterLabelException(String message) {
		super(message);
	}
}
