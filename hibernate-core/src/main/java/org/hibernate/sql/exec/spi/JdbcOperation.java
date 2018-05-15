/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.List;
import java.util.Set;

/**
 * Unifying contract for any SQL statement we want to execute via JDBC.
 *
 * todo (6.0) : good idea to have a single `#execute(...)` method here imo...
 * 		defines a nice singular access point for execution of an operation,
 * 		defined on each direct subclass
 *
 * @author Steve Ebersole
 */
public interface JdbcOperation {
	/**
	 * Get the SQL command we will be executing through JDBC PreparedStatement
	 * or CallableStatement
	 */
	String getSql();

	/**
	 * Get the list of parameter binders for the generated PreparedStatement
	 */
	List<JdbcParameterBinder> getParameterBinders();

	Set<String> getAffectedTableNames();
}
