/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.PersistenceException;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Gavin King
 * @since 7.0
 */
public class AttributeConverterWrapper<O,R> implements BasicValueConverter<O,R> {
	private final AttributeConverter<O,R> converter;
	private final JavaType<? extends AttributeConverter<O, R>> converterJtd;
	private final JavaType<O> domainJtd;
	private final JavaType<R> jdbcJtd;

	public AttributeConverterWrapper(
			AttributeConverter<O, R> converter,
			JavaType<? extends AttributeConverter<O,R>> converterJtd,
			JavaType<O> domainJtd,
			JavaType<R> jdbcJtd) {
		this.converter = converter;
		this.converterJtd = converterJtd;
		this.domainJtd = domainJtd;
		this.jdbcJtd = jdbcJtd;
	}

	@Override
	public O toDomainValue(R relationalForm) {
		try {
			return converter.convertToEntityAttribute( relationalForm );
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (RuntimeException re) {
			throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
		}
	}

	@Override
	public R toRelationalValue(O domainForm) {
		try {
			return converter.convertToDatabaseColumn( domainForm );
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (RuntimeException re) {
			throw new PersistenceException( "Error attempting to apply AttributeConverter: " + re.getMessage(), re );
		}
	}

	@Override
	public JavaType<O> getDomainJavaType() {
		return domainJtd;
	}

	@Override
	public JavaType<R> getRelationalJavaType() {
		return jdbcJtd;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AttributeConverterWrapper<?, ?> that = (AttributeConverterWrapper<?, ?>) o;

		if ( !converter.equals( that.converter ) ) {
			return false;
		}
		if ( !converterJtd.equals( that.converterJtd ) ) {
			return false;
		}
		if ( !domainJtd.equals( that.domainJtd ) ) {
			return false;
		}
		return jdbcJtd.equals( that.jdbcJtd );
	}

	@Override
	public int hashCode() {
		int result = converter.hashCode();
		result = 31 * result + converterJtd.hashCode();
		result = 31 * result + domainJtd.hashCode();
		result = 31 * result + jdbcJtd.hashCode();
		return result;
	}
}
