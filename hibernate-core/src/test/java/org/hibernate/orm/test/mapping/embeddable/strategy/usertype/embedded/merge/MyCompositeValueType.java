/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.merge;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

public class MyCompositeValueType implements CompositeUserType<MyCompositeValue> {
	public static class EmbeddableMapper {
		Long longValue;
		String stringValue;
	}

	@Override
	public Object getPropertyValue(MyCompositeValue component, int property) throws HibernateException {
		switch ( property ) {
			case 0:
				return component.longValue();
			case 1:
				return component.stringValue();
			default:
				return null;
		}
	}

	@Override
	public MyCompositeValue instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
		final Long id = values.getValue( 0, Long.class );
		final String hash = values.getValue( 1, String.class );
		return new MyCompositeValue( id, hash );
	}

	@Override
	public Class<?> embeddable() {
		return EmbeddableMapper.class;
	}

	@Override
	public Class<MyCompositeValue> returnedClass() {
		return MyCompositeValue.class;
	}

	@Override
	public boolean equals(MyCompositeValue x, MyCompositeValue y) {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(MyCompositeValue x) {
		return Objects.hashCode( x );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public MyCompositeValue deepCopy(MyCompositeValue value) {
		if ( value == null ) {
			return null;
		}
		return new MyCompositeValue( value.longValue(), value.stringValue() );
	}

	@Override
	public Serializable disassemble(MyCompositeValue value) {
		return new Object[] { value.longValue(), value.stringValue() };
	}

	@Override
	public MyCompositeValue assemble(Serializable cached, Object owner) {
		final Object[] parts = (Object[]) cached;
		return new MyCompositeValue( (Long) parts[0], (String) parts[1] );
	}

	@Override
	public MyCompositeValue replace(MyCompositeValue detached, MyCompositeValue managed, Object owner) {
		return deepCopy( detached );
	}
}
