/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.java.AbstractArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import static java.lang.Character.toUpperCase;

/**
 * A type that maps between {@link java.sql.Types#ARRAY ARRAY} and {@code T[]}
 *
 * @author Jordan Gigov
 * @author Christian Beikov
 */
public final class BasicArrayType<T,E>
		extends AbstractSingleColumnStandardBasicType<T>
		implements AdjustableBasicType<T>, BasicPluralType<T, E> {

	private final BasicType<E> baseDescriptor;
	private final String name;
	private final AbstractArrayJavaType<T,?> arrayTypeDescriptor;

	public BasicArrayType(BasicType<E> baseDescriptor, JdbcType arrayJdbcType, JavaType<T> arrayTypeDescriptor) {
		super( arrayJdbcType, arrayTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = determineArrayTypeName( baseDescriptor );
		this.arrayTypeDescriptor = (AbstractArrayJavaType<T, ?>) arrayTypeDescriptor;
	}

	static String determineElementTypeName(BasicType<?> baseDescriptor) {
		final String elementName = baseDescriptor.getName();
		return switch ( elementName ) {
			case "boolean", "byte", "char", "short", "int", "long", "float", "double" ->
					toUpperCase( elementName.charAt( 0 ) )
							+ elementName.substring( 1 );
			default -> elementName;
		};
	}

	static String determineArrayTypeName(BasicType<?> baseDescriptor) {
		return determineElementTypeName( baseDescriptor ) + "[]";
	}

	@Override
	public BasicType<E> getElementType() {
		return baseDescriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> domainJtd) {
		// TODO: maybe fall back to some encoding by default if
		//      the database doesn't support arrays natively?
		//      also, maybe move that logic into the ArrayJdbcType
		//noinspection unchecked
		return (BasicType<X>) this;
	}

	@Override
	public boolean equals(Object object) {
		return object == this
			|| object instanceof BasicArrayType<?,?> arrayType // no subtypes
			&& Objects.equals( baseDescriptor, arrayType.baseDescriptor );
	}

	@Override
	public int hashCode() {
		return baseDescriptor.hashCode();
	}

	// Methods required to support Horrible hack around the fact
	// that java.sql.Timestamps in an array can be represented as
	// instances of java.util.Date (Why do we even allow this?)

	@Override
	public boolean isEqual(Object one, Object another) {
		if ( one == another ) {
			return true;
		}
		else if ( one == null || another == null ) {
			return false;
		}
		else {
			return arrayTypeDescriptor.isEqual( one, another );
		}
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return arrayTypeDescriptor.deepCopy( value );
	}
}
