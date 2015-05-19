/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception;
import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * Generic, non-specific JDBCException.
 *
 * @author Steve Ebersole
 */
public class GenericJDBCException extends JDBCException {
	public GenericJDBCException(String string, SQLException root) {
		super( string, root );
	}

	public GenericJDBCException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
