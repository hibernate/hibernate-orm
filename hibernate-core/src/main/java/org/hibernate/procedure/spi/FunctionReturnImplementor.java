/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.FunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * @author Steve Ebersole
 */
public interface FunctionReturnImplementor<T> extends FunctionReturn<T>, ProcedureParameterImplementor<T> {
	@Override
	default JdbcCallParameterRegistration toJdbcParameterRegistration(
			int startIndex,
			ProcedureCallImplementor<?> procedureCall) {
		return toJdbcFunctionReturn( procedureCall.getSession() );
	}

	JdbcCallFunctionReturn toJdbcFunctionReturn(SharedSessionContractImplementor session);
}
