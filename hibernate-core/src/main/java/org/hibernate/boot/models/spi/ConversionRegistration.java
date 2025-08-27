/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import jakarta.persistence.AttributeConverter;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.models.spi.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * A registered conversion.
 *
 * @see org.hibernate.annotations.ConverterRegistration
 *
 * @apiNote Largely a copy of {@linkplain RegisteredConversion} to avoid early creation of
 * {@linkplain ConverterDescriptor}. Technically the conversion from ClassDetails to Class
 * should be fine since conversions are only valid for basic types which we will never enhance.
 *
 * @author Steve Ebersole
 */
public class ConversionRegistration {
	private final Class<?> explicitDomainType;
	private final Class<? extends AttributeConverter<?,?>> converterType;
	private final boolean autoApply;
	private final AnnotationDescriptor<? extends Annotation> source;

	public ConversionRegistration(
			Class<?> explicitDomainType,
			Class<? extends AttributeConverter<?,?>> converterType,
			boolean autoApply,
			AnnotationDescriptor<? extends Annotation> source) {
		assert converterType != null;

		this.explicitDomainType = explicitDomainType;
		this.converterType = converterType;
		this.autoApply = autoApply;
		this.source = source;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( !(object instanceof ConversionRegistration that) ) {
			return false;
		}
		return autoApply == that.autoApply
			&& Objects.equals( explicitDomainType, that.explicitDomainType )
			&& converterType.equals( that.converterType );
	}

	@Override
	public int hashCode() {
		return Objects.hash( explicitDomainType, converterType );
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

	public AnnotationDescriptor<? extends Annotation> getSource() {
		return source;
	}

	@Override
	public String toString() {
		return "ConversionRegistration( " + converterType.getName() + ", " + source.getAnnotationType().getSimpleName() + ", " + autoApply + ")";
	}
}
