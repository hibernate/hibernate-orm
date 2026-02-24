/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;

/// Indicates either -
/// * a call to [Output#asResultSetOutput] for a [UpdateCountOutput]
/// * a call to [Output#asUpdateCountOutput] for a [ResultSetOutput].
///
/// @author Steve Ebersole
public class IllegalOutputTypeException extends HibernateException {
	public IllegalOutputTypeException(String message) {
		super( message );
	}

	public IllegalOutputTypeException(String message, @Nullable Throwable cause) {
		super( message, cause );
	}
}
