/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.HibernateException;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Indicates a problem with a TREAT usage
 *
 * @author Steve Ebersole
 */
public class TreatException extends HibernateException {
	public TreatException(String message) {
		super( message );
	}

	public TreatException(String message, @Nullable Throwable cause) {
		super( message, cause );
	}
}
