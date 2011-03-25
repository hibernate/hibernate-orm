/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.tool.hbm2ddl;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for delegates responsible for managing connection used by the
 * hbm2ddl tools.
 *
 * @author Steve Ebersole
 */
public interface ConnectionHelper {
	/**
	 * Prepare the helper for use.
	 *
	 * @param needsAutoCommit Should connection be forced to auto-commit
	 * if not already.
	 * @throws SQLException
	 */
	public void prepare(boolean needsAutoCommit) throws SQLException;

	/**
	 * Get a reference to the connection we are using.
	 *
	 * @return The JDBC connection.
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException;

	/**
	 * Release any resources held by this helper.
	 *
	 * @throws SQLException
	 */
	public void release() throws SQLException;
}
