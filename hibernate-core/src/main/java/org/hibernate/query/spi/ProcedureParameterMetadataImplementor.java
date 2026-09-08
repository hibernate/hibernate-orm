/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Parameter;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;

public interface ProcedureParameterMetadataImplementor extends ParameterMetadataImplementor {

	@Nonnull
	ParameterStrategy getParameterStrategy();

	@Override
	@Nonnull
	ProcedureParameterImplementor<?> getQueryParameter(@Nonnull String name);

	@Override
	@Nonnull
	ProcedureParameterImplementor<?> getQueryParameter(int positionLabel);

	@Override
	@Nonnull
	<P> ProcedureParameterImplementor<P> resolve(@Nonnull Parameter<P> parameter);

	void registerParameter(@Nonnull ProcedureParameterImplementor<?> parameter);

	@Nonnull
	List<? extends ProcedureParameterImplementor<?>> getRegistrationsAsList();

}
