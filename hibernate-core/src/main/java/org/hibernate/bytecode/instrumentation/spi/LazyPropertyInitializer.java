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

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Contract for controlling how lazy properties get initialized.
 * 
 * @author Gavin King
 */
public interface LazyPropertyInitializer {

	/**
	 * Marker value for uninitialized properties
	 */
	public static final Serializable UNFETCHED_PROPERTY = new Serializable() {
		public String toString() {
			return "<lazy>";
		}
		public Object readResolve() {
			return UNFETCHED_PROPERTY;
		}
	};

	/**
	 * Initialize the property, and return its new value
	 *
	 * @param fieldName The name of the field being initialized
	 * @param entity The entity on which the initialization is occurring
	 * @param session The session from which the initialization originated.
	 *
	 * @return ?
	 */
	public Object initializeLazyProperty(String fieldName, Object entity, SessionImplementor session);

}
