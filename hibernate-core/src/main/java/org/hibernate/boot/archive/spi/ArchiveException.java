/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem accessing or visiting the archive
 *
 * @author Steve Ebersole
 */
public class ArchiveException extends HibernateException {
	/**
	 * Constructs an ArchiveException
	 *
	 * @param message Message explaining the exception condition
	 */
	public ArchiveException(String message) {
		super( message );
	}

	/**
	 * Constructs an ArchiveException
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public ArchiveException(String message, Throwable cause) {
		super( message, cause );
	}
}
