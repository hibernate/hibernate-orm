/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Represents a "memento" (disconnected, externalizable form) of a ProcedureCall
 *
 * @author Steve Ebersole
 */
public interface ProcedureCallMemento {
	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	public ProcedureCall makeProcedureCall(Session session);

	/**
	 * Convert the memento back into an executable (connected) form.
	 *
	 * @param session The session to connect the procedure call to
	 *
	 * @return The executable call
	 */
	public ProcedureCall makeProcedureCall(SessionImplementor session);

	/**
	 * Access to any hints associated with the memento.
	 * <p/>
	 * IMPL NOTE : exposed separately because only HEM needs access to this.
	 *
	 * @return The hints.
	 */
	public Map<String, Object> getHintsMap();
}
