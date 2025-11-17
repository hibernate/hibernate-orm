/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.util.List;

import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * Access the values defining a native non-select query
 *
 * @author Steve Ebersole
 */
public interface NativeNonSelectQueryDefinition {
	String getSqlString();
	List<JdbcParameterBinder> getParameterBinders();

	// todo (6.0) : affected table - either table names or Table references

	// todo (6.0) : drop support for executing callables via NativeQuery
	boolean isCallable();
}
