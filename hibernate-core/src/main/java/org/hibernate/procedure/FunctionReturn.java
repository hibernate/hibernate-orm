/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.query.procedure.ProcedureParameter;

/**
 * Describes the function return for ProcedureCalls that represent calls to
 * a function ({@code "{? = call ...}} syntax) rather that a proc ({@code {call ...}} syntax)
 *
 * @author Steve Ebersole
 */
public interface FunctionReturn<T> extends ProcedureParameter<T> {
	int getJdbcTypeCode();
}
