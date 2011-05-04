/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.bytecode.instrumentation.spi;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Contract for field interception handlers.
 *
 * @author Steve Ebersole
 */
public interface FieldInterceptor {

	/**
	 * Use to associate the entity to which we are bound to the given session.
	 *
	 * @param session The session to which we are now associated.
	 */
	public void setSession(SessionImplementor session);

	/**
	 * Is the entity to which we are bound completely initialized?
	 *
	 * @return True if the entity is initialized; otherwise false.
	 */
	public boolean isInitialized();

	/**
	 * The the given field initialized for the entity to which we are bound?
	 *
	 * @param field The name of the field to check
	 * @return True if the given field is initialized; otherwise false.
	 */
	public boolean isInitialized(String field);

	/**
	 * Forcefully mark the entity as being dirty.
	 */
	public void dirty();

	/**
	 * Is the entity considered dirty?
	 *
	 * @return True if the entity is dirty; otherwise false.
	 */
	public boolean isDirty();

	/**
	 * Clear the internal dirty flag.
	 */
	public void clearDirty();
}
