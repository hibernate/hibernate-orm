/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
