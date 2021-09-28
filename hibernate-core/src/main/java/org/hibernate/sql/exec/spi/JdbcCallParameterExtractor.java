/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.sql.CallableStatement;

import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;

/**
 * Controls extracting values from OUT/INOUT parameters.
 * <p/>
 * For extracting REF_CURSOR results, see {@link JdbcCallRefCursorExtractorImpl} instead.
 *
 * @author Steve Ebersole
 */
public interface JdbcCallParameterExtractor<T> {
	String getParameterName();
	int getParameterPosition();

	T extractValue(
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			ExecutionContext executionContext);
}
