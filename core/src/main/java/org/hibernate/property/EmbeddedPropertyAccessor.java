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

import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * @author Gavin King
 */
public class EmbeddedPropertyAccessor implements PropertyAccessor {
	
	public static final class EmbeddedGetter implements Getter {
		private final Class clazz;
		
		EmbeddedGetter(Class clazz) {
			this.clazz = clazz;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public Object get(Object target) throws HibernateException {
			return target;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			return get( target );
		}

		/**
		 * {@inheritDoc}
		 */
		public Member getMember() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public Method getMethod() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getMethodName() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public Class getReturnType() {
			return clazz;
		}
		
		public String toString() {
			return "EmbeddedGetter(" + clazz.getName() + ')';
		}
	}

	public static final class EmbeddedSetter implements Setter {
		private final Class clazz;
		
		EmbeddedSetter(Class clazz) {
			this.clazz = clazz;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public Method getMethod() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getMethodName() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public void set(Object target, Object value, SessionFactoryImplementor factory) {
		}

		/**
		 * {@inheritDoc}
		 */
		public String toString() {
			return "EmbeddedSetter(" + clazz.getName() + ')';
		}
	}

	public Getter getGetter(Class theClass, String propertyName)
	throws PropertyNotFoundException {
		return new EmbeddedGetter(theClass);
	}

	public Setter getSetter(Class theClass, String propertyName)
	throws PropertyNotFoundException {
		return new EmbeddedSetter(theClass);
	}

}
