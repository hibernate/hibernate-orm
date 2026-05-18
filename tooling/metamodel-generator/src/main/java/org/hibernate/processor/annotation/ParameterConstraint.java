/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.processor.util.Constants.JD_AT_LEAST;
import static org.hibernate.processor.util.Constants.JD_AT_MOST;
import static org.hibernate.processor.util.Constants.JD_EQUAL_TO;
import static org.hibernate.processor.util.Constants.JD_GREATER_THAN;
import static org.hibernate.processor.util.Constants.JD_IN;
import static org.hibernate.processor.util.Constants.JD_LESS_THAN;
import static org.hibernate.processor.util.Constants.JD_LIKE;
import static org.hibernate.processor.util.Constants.JD_NOT_EQUAL_TO;
import static org.hibernate.processor.util.Constants.JD_NOT_IN;
import static org.hibernate.processor.util.Constants.JD_NOT_LIKE;

enum ParameterConstraint {
	EQUAL,
	NOT_EQUAL,
	GREATER_THAN,
	AT_LEAST,
	LESS_THAN,
	AT_MOST,
	LIKE,
	NOT_LIKE,
	IN,
	NOT_IN,
	RUNTIME;

	static @Nullable ParameterConstraint fromConstraintType(String constraintType) {
		return switch ( constraintType ) {
			case JD_EQUAL_TO -> EQUAL;
			case JD_NOT_EQUAL_TO -> NOT_EQUAL;
			case JD_GREATER_THAN -> GREATER_THAN;
			case JD_AT_LEAST -> AT_LEAST;
			case JD_LESS_THAN -> LESS_THAN;
			case JD_AT_MOST -> AT_MOST;
			case JD_LIKE -> LIKE;
			case JD_NOT_LIKE -> NOT_LIKE;
			case JD_IN -> IN;
			case JD_NOT_IN -> NOT_IN;
			default -> null;
		};
	}

	boolean isMultivalued() {
		return this == IN || this == NOT_IN;
	}

	boolean isStringPattern() {
		return this == LIKE || this == NOT_LIKE;
	}

	boolean isComparison() {
		return switch ( this ) {
			case GREATER_THAN, AT_LEAST, LESS_THAN, AT_MOST -> true;
			default -> false;
		};
	}
}
