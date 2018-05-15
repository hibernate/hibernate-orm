/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.internal.FunctionReturnImpl;
import org.hibernate.sql.exec.spi.JdbcCall;

/**
 * @author Steve Ebersole
 */
public interface CallableStatementSupport {
	JdbcCall interpretCall(
			String procedureName,
			FunctionReturnImpl functionReturn,
			ProcedureParameterMetadata parameterMetadata,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session);

//	boolean shouldUseFunctionSyntax(
//			ProcedureCallImplementor procedureCall,
//			ProcedureParameterMetadata parameterMetadata,
//			ProcedureParamBindings paramBindings,
//			SessionFactoryImplementor sessionFactory);
//
	String renderCallableStatement(
			String procedureName,
			JdbcCall jdbcCall,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session);
//
//	void registerParameters(
//			String procedureName,
//			CallableStatement statement,
//			ParameterStrategy parameterStrategy,
//			JdbcCallFunctionReturn functionReturn,
//			List<JdbcCallParameterRegistration > parameterRegistrations,
//			SharedSessionContractImplementor session);
}
