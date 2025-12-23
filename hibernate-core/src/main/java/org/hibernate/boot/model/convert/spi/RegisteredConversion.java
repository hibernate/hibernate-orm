/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.spi;

import java.util.Objects;

import org.hibernate.boot.model.convert.internal.ConverterDescriptors;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.boot.model.convert.internal.GenericTypeResolver.resolveInterfaceTypeArguments;

/**
 * A registered conversion.
 *
 * @see org.hibernate.annotations.ConverterRegistration
 *
 * @author Steve Ebersole
 */
public record RegisteredConversion(
		Class<?> explicitDomainType,
		Class<? extends AttributeConverter<?,?>> converterType,
		boolean autoApply,
		ConverterDescriptor<?,?> converterDescriptor) {

	public RegisteredConversion {
		assert converterType != null;
	}

	public RegisteredConversion(
			Class<?> explicitDomainType,
			Class<? extends AttributeConverter<?,?>> converterType,
			boolean autoApply) {
		this( explicitDomainType, converterType, autoApply,
				determineConverterDescriptor( explicitDomainType, converterType, autoApply ) );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof RegisteredConversion that) ) {
			return false;
		}
		return this.autoApply == that.autoApply
			&& Objects.equals( this.explicitDomainType, that.explicitDomainType )
			&& Objects.equals( this.converterType, that.converterType );
	}

	@Override
	public int hashCode() {
		return Objects.hash( explicitDomainType, converterType );
	}

	private static ConverterDescriptor<?,?> determineConverterDescriptor(
			Class<?> explicitDomainType,
			Class<? extends AttributeConverter<?, ?>> converterType,
			boolean autoApply) {
		final var resolvedParamTypes =
				resolveInterfaceTypeArguments( AttributeConverter.class, converterType );
		final var relationalType = resolvedParamTypes[1];
		final var domainTypeToMatch =
				void.class.equals( explicitDomainType )
						? resolvedParamTypes[0]
						: explicitDomainType;
		return ConverterDescriptors.of( converterType, domainTypeToMatch, relationalType, autoApply );
	}

	public Class<?> getExplicitDomainType() {
		return explicitDomainType;
	}

	public Class<? extends AttributeConverter<?,?>> getConverterType() {
		return converterType;
	}

	public boolean isAutoApply() {
		return autoApply;
	}

	public ConverterDescriptor<?,?> getConverterDescriptor() {
		return converterDescriptor;
	}
}
