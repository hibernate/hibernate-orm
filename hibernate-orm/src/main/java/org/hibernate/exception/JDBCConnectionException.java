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
 * Implementation of JDBCException indicating problems with communicating with the
 * database (can also include incorrect JDBC setup).
 *
 * @author Steve Ebersole
 */
public class JDBCConnectionException extends JDBCException {
	public JDBCConnectionException(String string, SQLException root) {
		super( string, root );
	}

	public JDBCConnectionException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
