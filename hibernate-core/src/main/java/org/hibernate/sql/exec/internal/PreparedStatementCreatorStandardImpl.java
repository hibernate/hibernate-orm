/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.sql.exec.spi.PreparedStatementCreator;

/**
 * @author Steve Ebersole
 */
public class PreparedStatementCreatorStandardImpl implements PreparedStatementCreator {
	/**
	 * Singleton access
	 */
	public static final PreparedStatementCreatorStandardImpl INSTANCE = new PreparedStatementCreatorStandardImpl();

	@Override
	public PreparedStatement create(Connection connection, String sql) throws SQLException {
		return connection.prepareStatement( sql );
	}
}
