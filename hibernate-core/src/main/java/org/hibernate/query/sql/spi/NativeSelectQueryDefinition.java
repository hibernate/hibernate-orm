/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.query.results.ResultSetMapping;

/**
 * Access the values defining a native select query
 *
 * @author Steve Ebersole
 */
public interface NativeSelectQueryDefinition<R> {
	String getSqlString();

	/**
	 * @apiNote This returns query parameters in the order they were
	 * encountered - potentially including "duplicate references" to a single parameter
	 */
	List<ParameterOccurrence> getQueryParameterOccurrences();

	ResultSetMapping getResultSetMapping();

	Set<String> getAffectedTableNames();

	// todo (6.0) : drop support for executing callables via NativeQuery
	boolean isCallable();

}
