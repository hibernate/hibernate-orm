/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.models.Copied;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.AttributeConverter;

/// Global registration for an attribute converter.
///
/// @param explicitDomainType The explicit domain type handled by the converter, or
/// {@code null} when the domain type is inferred from the converter declaration
/// @param converterType The converter implementation class
/// @param autoApply Whether the converter should be applied automatically
/// @param source The annotation descriptor that declared the registration
///
/// @see ConverterRegistration
///
/// @since 9.0
/// @author Steve Ebersole
@Copied(RegisteredConversion.class)
public record ConversionRegistration(
		ClassDetails explicitDomainType,
		ClassDetails converterType,
		boolean autoApply,
		AnnotationDescriptor<? extends Annotation> source) {

	// @todo copied from RegisteredConversion because of the "early" creation of `ConverterDescriptor`
	// upstream. Technically the conversion from ClassDetails to Class should be fine since
	// conversions are only valid for basic types which we will never enhance.

	public ConversionRegistration {
		assert converterType != null;

	}

	public ConverterDescriptor<?, ?> makeConverterDescriptor() {
		final Class<Object> explicitDomainType =
				this.explicitDomainType == null ? null : this.explicitDomainType.toJavaClass();
		final Class<? extends AttributeConverter<?, ?>> converterType = this.converterType.toJavaClass();
		return new RegisteredConversion( explicitDomainType, converterType, autoApply ).getConverterDescriptor();
	}

}
