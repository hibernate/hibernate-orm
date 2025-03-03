/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.named.NamedQueryMemento;

/**
 * Represents a "memento" (disconnected, externalizable form) of a ProcedureCall
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NamedCallableQueryMemento extends NamedQueryMemento<Object> {
	/**
	 * Informational access to the name of the database function or procedure
	 */
	String getCallableName();

	List<ParameterMemento> getParameterMementos();

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	default ProcedureCall makeProcedureCall(Session session) {
		return makeProcedureCall( (SharedSessionContractImplementor) session );
	}

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	default ProcedureCall makeProcedureCall(SessionImplementor session) {
		return makeProcedureCall( (SharedSessionContractImplementor) session );
	}

	ParameterStrategy getParameterStrategy();

	String[] getResultSetMappingNames();

	Class<?>[] getResultSetMappingClasses();

	Set<String> getQuerySpaces();

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	ProcedureCall makeProcedureCall(SharedSessionContractImplementor session);

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	ProcedureCall makeProcedureCall(SharedSessionContractImplementor session, String... resultSetMappingNames);

	interface ParameterMemento extends NamedQueryMemento.ParameterMemento {
		ProcedureParameterImplementor<?> resolve(SharedSessionContractImplementor session);
	}

	@Override
	default Class<Object> getResultType() {
		return Object.class;
	}
}
