/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.property;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Sets values to a particular property.
 * 
 * @author Gavin King
 */
public interface Setter extends Serializable {
	/**
	 * Set the property value from the given instance
	 *
	 * @param target The instance upon which to set the given value.
	 * @param value The value to be set on the target.
	 * @param factory The session factory from which this request originated.
	 * @throws HibernateException
	 */
	public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException;
	/**
	 * Optional operation (return null)
	 */
	public String getMethodName();
	/**
	 * Optional operation (return null)
	 */
	public Method getMethod();
}
