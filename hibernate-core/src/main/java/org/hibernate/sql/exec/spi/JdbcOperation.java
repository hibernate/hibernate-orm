/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.spi;

import java.util.List;

/**
 * A JDBC operation to perform.  This always equates to
 * some form of JDBC {@link java.sql.PreparedStatement} or
 * {@link java.sql.CallableStatement} execution
 *
 * @author Steve Ebersole
 */
public interface JdbcOperation {
	/**
	 * Get the SQL command we will be executing through JDBC PreparedStatement
	 * or CallableStatement
	 */
	String getSqlString();

	/**
	 * Get the list of parameter binders for the generated PreparedStatement
	 */
	List<JdbcParameterBinder> getParameterBinders();
}
