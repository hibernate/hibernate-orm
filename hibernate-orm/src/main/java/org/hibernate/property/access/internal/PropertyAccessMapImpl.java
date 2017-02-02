/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

/**
 * PropertyAccess implementation that deal with an underlying Map as the container using
 * {@link Map#get} and {@link Map#put}
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessMapImpl implements PropertyAccess {
	private final Getter getter;
	private final Setter setter;
	private final PropertyAccessStrategyMapImpl strategy;

	public PropertyAccessMapImpl(PropertyAccessStrategyMapImpl strategy, final String propertyName) {
		this.strategy = strategy;
		this.getter = new GetterImpl( propertyName );
		this.setter = new SetterImpl( propertyName );
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return strategy;
	}

	@Override
	public Getter getGetter() {
		return getter;
	}

	@Override
	public Setter getSetter() {
		return setter;
	}

	public static class GetterImpl implements Getter {
		private final String propertyName;

		public GetterImpl(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		public Object get(Object owner) {
			return ( (Map) owner ).get( propertyName );
		}

		@Override
		public Object getForInsert(Object owner, Map mergeMap, SharedSessionContractImplementor session) {
			return get( owner );
		}

		@Override
		public Class getReturnType() {
			// we just don't know...
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

	public static class SetterImpl implements Setter {
		private final String propertyName;

		public SetterImpl(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void set(Object target, Object value, SessionFactoryImplementor factory) {
			( (Map) target ).put( propertyName, value );
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
