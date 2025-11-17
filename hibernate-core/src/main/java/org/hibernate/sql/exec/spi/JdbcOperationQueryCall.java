/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JdbcOperationQueryCall extends JdbcOperationQueryAnonBlock {
	/**
	 * If the call is a function, returns the function return descriptor
	 */
	JdbcCallFunctionReturn getFunctionReturn();

	/**
	 * Get the list of any parameter registrations we need to register
	 * against the generated CallableStatement
	 */
	List<JdbcCallParameterRegistration> getParameterRegistrations();

	/**
	 * Extractors for reading back any OUT/INOUT parameters.
	 *
	 * @apiNote Note that REF_CURSOR parameters should be handled via
	 * {@link #getCallRefCursorExtractors()}
	 */
	List<JdbcCallParameterExtractor<?>> getParameterExtractors();

	/**
	 * Extractors for REF_CURSOR (ResultSet) parameters
	 */
	List<JdbcCallRefCursorExtractor> getCallRefCursorExtractors();
}
