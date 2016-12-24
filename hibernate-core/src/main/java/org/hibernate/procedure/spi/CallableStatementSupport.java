/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import java.sql.CallableStatement;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * @author Steve Ebersole
 */
public interface CallableStatementSupport {
	boolean shouldUseFunctionSyntax(ParameterRegistry parameterRegistry);

	String renderCallableStatement(
			String callableName,
			ParameterStrategy parameterStrategy,
			JdbcCallFunctionReturn functionReturn,
			List<JdbcCallParameterRegistration> parameterRegistrations,
			SharedSessionContractImplementor session);

	void registerParameters(
			String procedureName,
			CallableStatement statement,
			ParameterStrategy parameterStrategy,
			JdbcCallFunctionReturn functionReturn,
			List<JdbcCallParameterRegistration > parameterRegistrations,
			SharedSessionContractImplementor session);
}
