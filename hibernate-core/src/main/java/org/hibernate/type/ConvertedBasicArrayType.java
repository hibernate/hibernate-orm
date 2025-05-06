/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.Objects;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Given a {@link BasicValueConverter} for an array type,
 *
 * @param <E> the unconverted element type
 * @param <T> the unconverted array type
 * @param <S> the converted array type
 *
 * @author Christian Beikov
 */
public class ConvertedBasicArrayType<T,S,E>
		extends AbstractSingleColumnStandardBasicType<T>
		implements AdjustableBasicType<T>, BasicPluralType<T, E> {

	private final BasicType<E> baseDescriptor;
	private final String name;

	private final BasicValueConverter<T, S> converter;
	private final ValueExtractor<T> jdbcValueExtractor;
	private final ValueBinder<T> jdbcValueBinder;
	private final JdbcLiteralFormatter<T> jdbcLiteralFormatter;

	@SuppressWarnings("unchecked")
	public ConvertedBasicArrayType(
			BasicType<E> baseDescriptor,
			JdbcType arrayJdbcType,
			JavaType<T> arrayTypeDescriptor,
			BasicValueConverter<T, S> converter) {
		super( arrayJdbcType, arrayTypeDescriptor );
		this.converter = converter;
		//TODO: these type casts look completely bogus (T==E[] and S are distinct array types)
		this.jdbcValueBinder = (ValueBinder<T>) arrayJdbcType.getBinder( converter.getRelationalJavaType() );
		this.jdbcValueExtractor = (ValueExtractor<T>) arrayJdbcType.getExtractor( converter.getRelationalJavaType() );
		this.jdbcLiteralFormatter = (JdbcLiteralFormatter<T>) arrayJdbcType.getJdbcLiteralFormatter( converter.getRelationalJavaType() );
		this.baseDescriptor = baseDescriptor;
		this.name = baseDescriptor.getName() + "[]";
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
		// TODO: maybe fallback to some encoding by default if the DB doesn't support arrays natively?
		//  also, maybe move that logic into the ArrayJdbcType
		//noinspection unchecked
		return (BasicType<X>) this;
	}

	@Override
	public BasicValueConverter<T, ?> getValueConverter() {
		return converter;
	}

	@Override
	public JavaType<?> getJdbcJavaType() {
		return converter.getRelationalJavaType();
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	@Override
	public ValueBinder<T> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public boolean equals(Object o) {
		return o == this || super.equals( o )
			&& o instanceof ConvertedBasicArrayType<?, ?, ?> that
			&& Objects.equals( converter, that.converter );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + converter.hashCode();
		return result;
	}
}
