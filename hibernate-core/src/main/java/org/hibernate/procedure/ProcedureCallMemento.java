/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
