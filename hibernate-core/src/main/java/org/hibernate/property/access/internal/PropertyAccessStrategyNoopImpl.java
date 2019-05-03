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
import org.hibernate.metamodel.model.domain.internal.NoopMember;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

/**
 * Describes a strategy for property access which is intentionally ignored (no-op). This is used to describe properties for DB fields which can be used in queries but which will
 * not be represented at the POJO level.
 * <p>
 * Take, for example, a field used for ordering which is updated nightly by some SQL batch job. If this field is a no-op field, then it may be used for ordering in queries without
 * needing to exist in the mapped DTO.
 *
 * @author Michael Bartmann
 * @author Gavin King
 * @author Steve Ebersole
 * @author Mike Hill
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
			return NoopMember.INSTANCE;
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
