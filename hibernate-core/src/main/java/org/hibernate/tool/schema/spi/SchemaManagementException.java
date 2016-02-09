/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem in performing schema management.
 * <p/>
 * Specifically this represents a a problem of an infrastructural-nature.  For
 * problems applying a specific command see {@link CommandAcceptanceException}
 *
 * @author Steve Ebersole
 */
public class SchemaManagementException extends HibernateException {
	public SchemaManagementException(String message) {
		super( message );
	}

	public SchemaManagementException(String message, Throwable root) {
		super( message, root );
	}
}
