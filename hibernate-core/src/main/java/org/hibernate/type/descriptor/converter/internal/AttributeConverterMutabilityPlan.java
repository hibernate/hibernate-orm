/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * The default {@link org.hibernate.type.descriptor.java.MutabilityPlan} for a
 * {@linkplain jakarta.persistence.AttributeConverter converted value} assumes,
 * in the absence of additional evidence, that the value is <em>mutable</em>, so
 * that dirty checking, deep copying, and second-level caching all work correctly
 * in the case where it really is mutable.
 * <p>
 * As an exception to this, Java primitive types and {@code enum}s are inferred
 * to be <em>immutable</em>.
 * <p>
 * To explicitly mark a converted value is immutable and avoid the extra processing
 * required for a mutable value, either:
 * <ul>
 * <li>mark the Java type of the attribute, the type passed to the converter, as
 *     {@link org.hibernate.annotations.Immutable @Immutable},
 * <li>explicitly specify a {@code MutabilityPlan} using
 *     {@link org.hibernate.annotations.Mutability @Mutability}, or
 * <li>explicitly specify its {@link org.hibernate.boot.model.JavaTypeDescriptor}
 *     using {@link org.hibernate.annotations.JavaType @JavaType}.
 * </ul>
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-10111">HHH-10111</a>
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-10127">HHH-10127</a>
 *
 * @see org.hibernate.type.descriptor.java.spi.RegistryHelper#determineMutabilityPlan(Type, TypeConfiguration)
 *
 * @author Steve Ebersole
 */
public final class AttributeConverterMutabilityPlan<T,S> extends MutableMutabilityPlan<T> {
	private final JpaAttributeConverter<T,S> converter;
	private final boolean mutable;

	public AttributeConverterMutabilityPlan(JpaAttributeConverter<T,S> converter, boolean mutable) {
		this.converter = converter;
		this.mutable = mutable;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	protected T deepCopyNotNull(T value) {
		return converter.toDomainValue( converter.toRelationalValue( value ) );
	}

	@Override
	public Serializable disassemble(T value, SharedSessionContract session) {
		return mutable ? (Serializable) converter.toRelationalValue( value ) : (Serializable) value;
	}

	@Override
	public T assemble(Serializable cached, SharedSessionContract session) {
		return mutable ? converter.toDomainValue( (S) cached) : (T) cached;
	}
}
