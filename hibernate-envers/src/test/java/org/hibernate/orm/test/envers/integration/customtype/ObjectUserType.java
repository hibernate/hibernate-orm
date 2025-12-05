/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.Lob;

/**
 * Custom type used to persist binary representation of Java object in the database.
 * Spans over two columns - one storing text representation of Java class name and the second one
 * containing binary data.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ObjectUserType implements CompositeUserType<Object> {

	@Override
	public Object getPropertyValue(Object component, int property) throws HibernateException {
		return switch ( property ) {
			case 0 -> component;
			case 1 -> component.getClass().getName();
			default -> null;
		};
	}

	@Override
	public Object instantiate(ValueAccess valueAccess) {
		return valueAccess.getValue( 0, Object.class );
	}

	@Override
	public Class<?> embeddable() {
		return TaggedObject.class;
	}

	@Override
	public Class<Object> returnedClass() {
		return Object.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value; // Persisting only immutable types.
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	public static class TaggedObject {
		String type;
		@Lob
		Serializable object;
	}
}
