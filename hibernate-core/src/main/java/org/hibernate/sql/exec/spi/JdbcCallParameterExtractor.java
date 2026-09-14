/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.sql.CallableStatement;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;

/**
 * Controls extracting values from OUT/INOUT parameters.
 * <p>
 * For extracting REF_CURSOR results, see {@link JdbcCallRefCursorExtractorImpl} instead.
 *
 * @author Steve Ebersole
 */
@Incubating(since = "6.0", group = "sql-execution")
public interface JdbcCallParameterExtractor<T> {
	String getParameterName();
	int getParameterPosition();

	T extractValue(
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			SharedSessionContractImplementor session);
}
