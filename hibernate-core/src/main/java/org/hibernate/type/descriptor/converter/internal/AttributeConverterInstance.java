/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.PersistenceException;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Objects;

/**
 * Implementation of {@link BasicValueConverter} backed by an instance of
 * the JPA-standard {@link AttributeConverter}.
 * <p>
 * This is used as an adaptor for the {@code AttributeConverter} returned
 * by {@link org.hibernate.usertype.UserType#getValueConverter()}
 *
 * @author Gavin King
 * @since 7.0
 */
public final class AttributeConverterInstance<O,R> implements BasicValueConverter<O,R> {
	private final AttributeConverter<O,R> converter;
	private final JavaType<O> domainJavaType;
	private final JavaType<R> jdbcJavaType;

	public AttributeConverterInstance(
			AttributeConverter<O, R> converter,
			JavaType<O> domainJavaType,
			JavaType<R> jdbcJavaType) {
		this.converter = converter;
		this.domainJavaType = domainJavaType;
		this.jdbcJavaType = jdbcJavaType;
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
		return domainJavaType;
	}

	@Override
	public JavaType<R> getRelationalJavaType() {
		return jdbcJavaType;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else {
			return object instanceof AttributeConverterInstance<?, ?> that
				&& Objects.equals( this.converter, that.converter )
				&& Objects.equals( this.domainJavaType, that.domainJavaType )
				&& Objects.equals( this.jdbcJavaType, that.jdbcJavaType );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( converter, domainJavaType, jdbcJavaType );
	}
}
