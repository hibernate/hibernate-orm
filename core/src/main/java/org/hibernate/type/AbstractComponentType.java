/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.SessionImplementor;

/**
 * Enables other Component-like types to hold collections and have cascades, etc.
 *
 * @see ComponentType
 * @see AnyType
 * @author Gavin King
 */
public interface AbstractComponentType extends Type {
	/**
	 * Get the types of the component properties
	 */
	public Type[] getSubtypes();
	/**
	 * Get the names of the component properties
	 */
	public String[] getPropertyNames();
	/**
	 * Optional operation
	 * @return nullability of component properties
	 */
	public boolean[] getPropertyNullability();
	/**
	 * Get the values of the component properties of 
	 * a component instance
	 */
	public Object[] getPropertyValues(Object component, SessionImplementor session) throws HibernateException;
	/**
	 * Optional operation
	 */
	public Object[] getPropertyValues(Object component, EntityMode entityMode) throws HibernateException;
	/**
	 * Optional operation
	 */
	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode) throws HibernateException;
	public Object getPropertyValue(Object component, int i, SessionImplementor session) throws HibernateException;
	//public Object instantiate(Object parent, SessionImplementor session) throws HibernateException;
	public CascadeStyle getCascadeStyle(int i);
	public FetchMode getFetchMode(int i);
	public boolean isMethodOf(Method method);
	public boolean isEmbedded();
}
