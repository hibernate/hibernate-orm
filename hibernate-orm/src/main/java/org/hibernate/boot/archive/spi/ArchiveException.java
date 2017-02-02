/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
