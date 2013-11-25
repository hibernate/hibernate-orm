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
 * @author Gavin King
 */
public class MapAccessor implements PropertyAccessor {
	@Override
	public Getter getGetter(Class theClass, String propertyName)
		throws PropertyNotFoundException {
		return new MapGetter(propertyName);
	}

	@Override
	public Setter getSetter(Class theClass, String propertyName)
		throws PropertyNotFoundException {
		return new MapSetter(propertyName);
	}

	public static final class MapSetter implements Setter {
		private String name;

		MapSetter(String name) {
			this.name = name;
		}

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void set(Object target, Object value, SessionFactoryImplementor factory)
			throws HibernateException {
			( (Map) target ).put( name, value );
		}

	}

	public static final class MapGetter implements Getter {
		private String name;

		MapGetter(String name) {
			this.name = name;
		}

		@Override
		public Member getMember() {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public Object get(Object target) throws HibernateException {
			return ( (Map) target ).get(name);
		}

		@Override
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			return get( target );
		}

		@Override
		public Class getReturnType() {
			return Object.class;
		}
	}

}
