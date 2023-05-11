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
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

/**
 * @author Christian Beikov
 */
public class ChainedPropertyAccessImpl implements PropertyAccess, Getter, Setter {

	private final PropertyAccess[] propertyAccesses;

	public ChainedPropertyAccessImpl(PropertyAccess... propertyAccesses) {
		this.propertyAccesses = propertyAccesses;
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return propertyAccesses[0].getPropertyAccessStrategy();
	}

	@Override
	public Getter getGetter() {
		return this;
	}

	@Override
	public Setter getSetter() {
		return this;
	}

	@Override
	public Object get(Object owner) {
		for ( int i = 0; i < propertyAccesses.length; i++ ) {
			owner = propertyAccesses[i].getGetter().get( owner );
		}
		return owner;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getForInsert(Object owner, Map mergeMap, SharedSessionContractImplementor session) {
		for ( int i = 0; i < propertyAccesses.length; i++ ) {
			owner = propertyAccesses[i].getGetter().getForInsert( owner, mergeMap, session );
		}
		return owner;
	}

	@Override
	public void set(Object target, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getReturnTypeClass() {
		return propertyAccesses[propertyAccesses.length - 1].getGetter().getReturnTypeClass();
	}

	@Override
	public Type getReturnType() {
		return propertyAccesses[propertyAccesses.length - 1].getGetter().getReturnType();
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
