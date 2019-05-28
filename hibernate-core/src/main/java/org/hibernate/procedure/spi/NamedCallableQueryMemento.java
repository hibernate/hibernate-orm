/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.spi;

import java.util.Map;

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
	@Override
	default String getQueryString() {
		return getCallableName();
	}

	/**
	 * The name of the database callable to execute.  Whereas {@link #getName()}
	 * describes the name under which the named query is registered with the
	 * SessionFactory, the callable name is the actual database procedure/function
	 * name
	 */
	String getCallableName();

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

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	ProcedureCall makeProcedureCall(SharedSessionContractImplementor session);

	/**
	 * Access to any hints associated with the memento.
	 * <p/>
	 * IMPL NOTE : exposed separately because only HEM needs access to this.
	 *
	 * @return The hints.
	 */
	Map<String, Object> getHintsMap();
}
