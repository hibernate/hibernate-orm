/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

/**
 * {@link PropertyAccess} for accessing the wrapped property via get/set pair, which may be nonpublic.
 *
 * @author Steve Ebersole
 *
 * @see PropertyAccessStrategyBasicImpl
 */
public class PropertyAccessCompositeUserTypeImpl implements PropertyAccess, Getter {

	private final PropertyAccessStrategyCompositeUserTypeImpl strategy;
	private final int propertyIndex;

	public PropertyAccessCompositeUserTypeImpl(PropertyAccessStrategyCompositeUserTypeImpl strategy, String property) {
		this.strategy = strategy;
		this.propertyIndex = strategy.sortedPropertyNames.indexOf( property );
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return strategy;
	}

	@Override
	public Getter getGetter() {
		return this;
	}

	@Override
	public Setter getSetter() {
		return null;
	}

	@Override
	public Object get(Object owner) {
		return strategy.compositeUserType.getPropertyValue( owner, propertyIndex );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getForInsert(Object owner, Map mergeMap, SharedSessionContractImplementor session) {
		return get( owner );
	}

	@Override
	public Class<?> getReturnTypeClass() {
		return ReflectHelper.getClass( strategy.sortedPropertyTypes.get(propertyIndex) );
	}

	@Override
	public Type getReturnType() {
		return strategy.sortedPropertyTypes.get(propertyIndex);
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
