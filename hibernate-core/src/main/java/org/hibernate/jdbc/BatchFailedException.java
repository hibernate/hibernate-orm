/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jdbc;
import org.hibernate.HibernateException;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Indicates a failed batch entry (-3 return).
 *
 * @author Steve Ebersole
 */
public class BatchFailedException extends HibernateException {
	public BatchFailedException(String s) {
		super( s );
	}

	public BatchFailedException(String string, @Nullable Throwable root) {
		super( string, root );
	}
}
