/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem accepting/executing a schema management command.
 *
 * @author Steve Ebersole
 */
public class CommandAcceptanceException extends HibernateException {
	public CommandAcceptanceException(String message) {
		super( message );
	}

	public CommandAcceptanceException(String message, Throwable cause) {
		super( message, cause );
	}
}
