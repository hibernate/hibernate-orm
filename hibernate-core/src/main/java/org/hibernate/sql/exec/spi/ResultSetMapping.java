/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.tree.spi.select.ResolvedResultSetMapping;
import org.hibernate.sql.ast.tree.spi.select.ResultSetAccess;

/**
 * Descriptor for the mapping of a JDBC ResultSet providing
 * support for delayed resolution if needed (mainly in the
 * case of {@link org.hibernate.query.NativeQuery}).
 *
 * @author Steve Ebersole
 */
public interface ResultSetMapping {
	/**
	 * Resolve the selections (both at the JDBC and object level) for this
	 * producer.  Intended to allow some sources of these (mainly NativeQuery)
	 * to delay building them until we have access to the ResultSet in order
	 * to resolve types, positions, etc.
	 *
	 * @param jdbcResultsAccess Access to the JDBC ResultSet and related info
	 * @param persistenceContext Access to the session, mainly for access
	 * 		to the SessionFactory and JdbcServices.
	 *
	 * @return The resolved selections :)
	 */
	ResolvedResultSetMapping resolve(
			ResultSetAccess jdbcResultsAccess,
			SharedSessionContractImplementor persistenceContext);
}
