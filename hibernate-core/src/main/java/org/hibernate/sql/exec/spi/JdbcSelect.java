/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

/**
 * Represents the {@link SqlAstSelectToJdbcSelectConverter}'s interpretation of a select query
 *
 * @author Steve Ebersole
 */
public interface JdbcSelect extends JdbcOperation {
	/**
	 * Retrieve the descriptor for performing the mapping
	 * of the JDBC ResultSet back to object query results.
	 */
	ResultSetMappingDescriptor getResultSetMapping();
}
