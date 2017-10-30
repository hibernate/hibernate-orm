/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for the mapping of a JDBC ResultSet providing
 * support for delayed resolution if needed (mainly in the
 * case of {@link org.hibernate.query.NativeQuery}).
 *
 * @author Steve Ebersole
 */
public interface ResultSetMappingDescriptor {
	/**
	 * Access to information about the underlying JDBC values
	 * such as type, position, column name, etc
	 */
	interface JdbcValuesMetadata {
		/**
		 * Number of values in the underlying result
		 */
		int getColumnCount();

		/**
		 * Position of a particular result value by name
		 */
		int resolveColumnPosition(String columnName);

		/**
		 * Name of a particular result value by position
		 */
		String resolveColumnName(int position);

		/**
		 * Descriptor of the JDBC/SQL type of a particular result value by
		 * position
		 */
		SqlTypeDescriptor resolveSqlTypeDescriptor(int position);

	}

	interface ResolutionContext {
		SharedSessionContractImplementor getPersistenceContext();
	}

	/**
	 * Resolve the selections (both at the JDBC and object level) for this
	 * mapping.  Acts as delayed access to this resolution process to support
	 * "auto discovery" as needed for "undefined scalar" results as defined by
	 * JPA.
	 *
	 * @param jdbcResultsMetadata Access to information about the underlying results
	 * @param resolutionContext Access to information needed for resolution (param object)
	 *
	 * @return The resolved result references
	 */
	ResultSetMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			ResolutionContext resolutionContext);
}
