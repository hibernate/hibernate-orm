/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.spi;

import java.sql.CallableStatement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.procedure.internal.ProcedureParamBindings;
import org.hibernate.query.procedure.internal.ProcedureParameterMetadata;

/**
 * @author Steve Ebersole
 */
public interface CallableStatementSupport {
	default String renderCallableStatement(
			String name,
			ParameterStrategy parameterStrategy,
			List<ParameterRegistrationImplementor<?>> parameterRegistrations,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(
				"Legacy #renderCallableStatement called but implementation does not support that call."
		);
	}

	default String renderCallableStatement(
			String procedureName,
			ProcedureParameterMetadata parameterMetadata,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session) {
		return renderCallableStatement(
				procedureName,
				parameterMetadata.getParameterStrategy(),
				new ArrayList( parameterMetadata.collectAllParameters() ),
				session
		);
	}

	void registerParameters(
			String procedureName,
			CallableStatement statement,
			ParameterStrategy parameterStrategy,
			List<ParameterRegistrationImplementor<?>> parameterRegistrations,
			SharedSessionContractImplementor session);
}
