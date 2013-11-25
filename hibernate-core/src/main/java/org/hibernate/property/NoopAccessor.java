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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Used to declare properties not represented at the pojo level
 * 
 * @author Michael Bartmann
 */
public class NoopAccessor implements PropertyAccessor {
	@Override
	public Getter getGetter(Class arg0, String arg1) throws PropertyNotFoundException {
		return new NoopGetter();
	}

	@Override
	public Setter getSetter(Class arg0, String arg1) throws PropertyNotFoundException {
		return new NoopSetter();
	}

	/**
	 * A Getter which will always return null. It should not be called anyway.
	 */
	private static class NoopGetter implements Getter {
		/**
		 * {@inheritDoc}
		 * <p/>
		 * Here we always return <tt>null</tt>
		 */
		@Override
		public Object get(Object target) throws HibernateException {
			return null;
		}

		@Override
		public Object getForInsert(Object target, Map map, SessionImplementor arg1)
				throws HibernateException {
			return null;
		}

		@Override
		public Class getReturnType() {
			return Object.class;
		}

		@Override
		public Member getMember() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}
	}

	/**
	 * A Setter which will just do nothing.
	 */
	private static class NoopSetter implements Setter {
		@Override
		public void set(Object target, Object value, SessionFactoryImplementor arg2) {
			// nothing to do
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}

	}
}
