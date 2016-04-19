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
 * Yeah, right, so....  No idea...
 *
 * @author Michael Bartmann
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyNoopImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyNoopImpl INSTANCE = new PropertyAccessStrategyNoopImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName) {
		return PropertyAccessNoopImpl.INSTANCE;
	}

	private static class PropertyAccessNoopImpl implements PropertyAccess {
		/**
		 * Singleton access
		 */
		public static final PropertyAccessNoopImpl INSTANCE = new PropertyAccessNoopImpl();

		@Override
		public PropertyAccessStrategy getPropertyAccessStrategy() {
			return PropertyAccessStrategyNoopImpl.INSTANCE;
		}

		@Override
		public Getter getGetter() {
			return GetterImpl.INSTANCE;
		}

		@Override
		public Setter getSetter() {
			return SetterImpl.INSTANCE;
		}
	}

	private static class GetterImpl implements Getter {
		/**
		 * Singleton access
		 */
		public static final GetterImpl INSTANCE = new GetterImpl();

		@Override
		public Object get(Object owner) {
			return null;
		}

		@Override
		public Object getForInsert(Object owner, Map mergeMap, SharedSessionContractImplementor session) {
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

	private static class SetterImpl implements Setter {
		/**
		 * Singleton access
		 */
		public static final SetterImpl INSTANCE = new SetterImpl();

		@Override
		public void set(Object target, Object value, SessionFactoryImplementor factory) {
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
