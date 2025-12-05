/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.List;

import jakarta.persistence.Parameter;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;

public interface ProcedureParameterMetadataImplementor extends ParameterMetadataImplementor {

	ParameterStrategy getParameterStrategy();

	@Override
	ProcedureParameterImplementor<?> getQueryParameter(String name);

	@Override
	ProcedureParameterImplementor<?> getQueryParameter(int positionLabel);

	@Override
	<P> ProcedureParameterImplementor<P> resolve(Parameter<P> parameter);

	void registerParameter(ProcedureParameterImplementor<?> parameter);

	List<? extends ProcedureParameterImplementor<?>> getRegistrationsAsList();

}
