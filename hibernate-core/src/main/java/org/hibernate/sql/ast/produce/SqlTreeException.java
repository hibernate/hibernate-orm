/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce;

import org.hibernate.HibernateException;

/**
 * Base exception type for problems building the SQL tree.
 *
 * @author Steve Ebersole
 */
public class SqlTreeException extends HibernateException {
	public SqlTreeException(String message) {
		super( message );
	}

	public SqlTreeException(String message, Throwable cause) {
		super( message, cause );
	}
}
