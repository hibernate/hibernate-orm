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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Gets values of a particular property
 *
 * @author Gavin King
 */
public interface Getter extends Serializable {
	/**
	 * Get the property value from the given instance .
	 * @param owner The instance containing the value to be retreived.
	 * @return The extracted value.
	 * @throws HibernateException
	 */
	public Object get(Object owner) throws HibernateException;

	/**
	 * Get the property value from the given owner instance.
	 *
	 * @param owner The instance containing the value to be retreived.
	 * @param mergeMap a map of merged persistent instances to detached instances
	 * @param session The session from which this request originated.
	 * @return The extracted value.
	 * @throws HibernateException
	 */
	public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) 
	throws HibernateException;

	/**
	 * Retrieve the member to which this property maps.  This might be the
	 * field or it might be the getter method.
	 *
	 * @return The mapped member.
	 */
	public Member getMember();

	/**
	 * Retrieve the declared Java type
	 *
	 * @return The declared java type.
	 */
	public Class getReturnType();

	/**
	 * Retrieve the getter-method name.
	 * <p/>
	 * Optional operation (return null)
	 *
	 * @return The name of the getter method, or null.
	 */
	public String getMethodName();

	/**
	 * Retrieve the getter-method.
	 * <p/>
	 * Optional operation (return null)
	 *
	 * @return The getter method, or null.
	 */
	public Method getMethod();
}
