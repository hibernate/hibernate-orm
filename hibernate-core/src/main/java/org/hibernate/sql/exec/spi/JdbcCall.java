/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.List;

import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

/**
 * @author Steve Ebersole
 */
public interface JdbcCall extends JdbcOperation {
	JdbcCallFunctionReturn getFunctionReturn();

	/**
	 * Get the list of any parameter registrations we need to register
	 * against the generated CallableStatement
	 */
	List<JdbcCallParameterRegistration> getParameterRegistrations();

	List<JdbcCallParameterExtractor> getParameterExtractors();

	List<JdbcCallRefCursorExtractor> getCallRefCursorExtractors();

	/**
	 * Retrieve the "result set mappings" for the JDBC call.  We expose multiple
	 * because JPA allows for an application to define multiple such mappings
	 * which are (unclearly) intended to describe the mapping for each
	 * ResultSet (in order) returned from the call.
	 */
	List<ResultSetMappingDescriptor> getResultSetMappings();
}
