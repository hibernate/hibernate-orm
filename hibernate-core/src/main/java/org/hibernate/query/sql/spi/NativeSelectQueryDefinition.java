/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * Access the values defining a native select query
 *
 * @author Steve Ebersole
 */
public interface NativeSelectQueryDefinition<R> {
	String getSqlString();
	List<JdbcParameterBinder> getParameterBinders();
	ResultSetMappingDescriptor getResultSetMapping();

	Set<String> getAffectedTableNames();

	RowTransformer<R> getRowTransformer();


	// todo (6.0) : drop support for executing callables via NativeQuery
	boolean isCallable();

}
