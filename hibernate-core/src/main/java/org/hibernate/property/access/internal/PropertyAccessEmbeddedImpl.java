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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

/**
 * PropertyAccess for handling non-aggregated composites.
 * <p/>
 * IMPL NOTE : We actually use a singleton for the Setter; we cannot for the getter mainly
 * because we need to differentiate {@link Getter#getReturnType()}.  Ultimately I'd prefer to
 * model that "common information" on PropertyAccess itself.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertyAccessEmbeddedImpl implements PropertyAccess {
	private final PropertyAccessStrategyEmbeddedImpl strategy;
	private final GetterImpl getter;

	@SuppressWarnings("UnusedParameters")
	public PropertyAccessEmbeddedImpl(
			PropertyAccessStrategyEmbeddedImpl strategy,
			Class containerType,
			String propertyName) {
		this.strategy = strategy;
		this.getter = new GetterImpl( containerType );
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
		return SetterImpl.INSTANCE;
	}

	private static class GetterImpl implements Getter {
		private final Class containerType;

		public GetterImpl(Class containerType) {
			this.containerType = containerType;
		}

		@Override
		public Object get(Object owner) {
			return owner;
		}

		@Override
		public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) {
			return owner;
		}

		@Override
		public Class getReturnType() {
			return containerType;
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
		 * Singleton access - we can actually use a singleton for the setter
		 */
		public static final SetterImpl INSTANCE = new SetterImpl();

		@Override
		public void set(Object target, Object value, SessionFactoryImplementor factory) {
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
