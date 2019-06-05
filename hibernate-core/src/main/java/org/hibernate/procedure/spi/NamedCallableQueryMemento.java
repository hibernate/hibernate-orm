/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.spi.NamedQueryMemento;

/**
 * Represents a "memento" (disconnected, externalizable form) of a ProcedureCall
 *
 * @author Steve Ebersole
 */
@Incubating
public interface NamedCallableQueryMemento extends NamedQueryMemento {
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

	Class[] getResultSetMappingClasses();

	Set<String> getQuerySpaces();

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	ProcedureCall makeProcedureCall(SharedSessionContractImplementor session);

	interface ParameterMemento extends NamedQueryMemento.ParameterMemento {
		ProcedureParameterImplementor resolve(SharedSessionContractImplementor session);
	}
}
