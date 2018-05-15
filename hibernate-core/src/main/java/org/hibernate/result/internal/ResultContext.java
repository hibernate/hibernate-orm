/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.result.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.hibernate.JDBCException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

/**
 * The context for the results
 *
 * @author Steve Ebersole
 */
public interface ResultContext extends ExecutionContext {
	@Override
	SharedSessionContractImplementor getSession();

	@Override
	QueryOptions getQueryOptions();

	@Override
	ParameterBindingContext getParameterBindingContext();

	@Override
	Callback getCallback();

	List<ResultSetMappingDescriptor> getResultSetMappings();

	Set<String> getSynchronizedQuerySpaces();

	JDBCException convertException(SQLException e, String message);
}
