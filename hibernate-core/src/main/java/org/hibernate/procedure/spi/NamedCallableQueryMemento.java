/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.named.spi.NamedQueryMemento;

/**
 * Represents a "memento" (disconnected, externalizable form) of a {@link ProcedureCall}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NamedCallableQueryMemento extends NamedQueryMemento<Object> {
	/**
	 * Informational access to the name of the database function or procedure
	 */
	@Nonnull
	String getCallableName();

	@Nonnull
	List<ParameterMemento> getParameterMementos();

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	@Nonnull
	default ProcedureCall makeProcedureCall(@Nonnull Session session) {
		return makeProcedureCall( (SharedSessionContractImplementor) session );
	}

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	@Nonnull
	default ProcedureCall makeProcedureCall(@Nonnull SessionImplementor session) {
		return makeProcedureCall( (SharedSessionContractImplementor) session );
	}

	@Nonnull
	ParameterStrategy getParameterStrategy();

	@Nullable
	String[] getResultSetMappingNames();

	@Nullable
	Class<?>[] getResultSetMappingClasses();

	@Nullable
	Set<String> getQuerySpaces();

	@Nonnull
	@Override
	ProcedureCallImplementor toQuery(@Nonnull SharedSessionContractImplementor session);

	@Nonnull
	@Override
	<X> ProcedureCallImplementor<X> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType);

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	@Nonnull
	ProcedureCallImplementor makeProcedureCall(@Nonnull SharedSessionContractImplementor session);

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	@Nonnull
	ProcedureCallImplementor makeProcedureCall(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull String... resultSetMappingNames);

	interface ParameterMemento extends NamedQueryMemento.ParameterMemento {
		@Nonnull
		ProcedureParameterImplementor<?> resolve(@Nonnull SharedSessionContractImplementor session);
	}
}
