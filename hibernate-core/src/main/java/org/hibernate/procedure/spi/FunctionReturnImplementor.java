package org.hibernate.procedure.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.FunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * @author Steve Ebersole
 */
public interface FunctionReturnImplementor<T> extends FunctionReturn<T>, ProcedureParameterImplementor<T> {
	@Override
	@Nonnull
	default JdbcCallParameterRegistration toJdbcParameterRegistration(
			int startIndex,
			@Nonnull ProcedureCallImplementor<?> procedureCall) {
		return toJdbcFunctionReturn( procedureCall.getSession() );
	}

	@Nonnull
	JdbcCallFunctionReturn toJdbcFunctionReturn(@Nonnull SharedSessionContractImplementor session);
}
