/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.models.spi.AnnotationTarget;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;

/**
 * Describes a {@linkplain AttributeConverter conversion}
 *
 * @see AttributeConverter
 * @see Convert
 *
 * @author Steve Ebersole
 */
public class AttributeConversionInfo {
	private final Class<? extends AttributeConverter<?,?>> converterClass;
	private final boolean conversionDisabled;
	private final String attributeName;

	private final AnnotationTarget source;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AttributeConversionInfo(
			Class<? extends AttributeConverter> converterClass,
			boolean conversionDisabled,
			String attributeName,
			AnnotationTarget source) {
		this.converterClass = (Class<? extends AttributeConverter<?, ?>>) converterClass;
		this.conversionDisabled = conversionDisabled;
		this.attributeName = attributeName;
		this.source = source;
	}

	public AttributeConversionInfo(Convert convertAnnotation, AnnotationTarget source) {
		this(
				convertAnnotation.converter(),
				convertAnnotation.disableConversion(),
				convertAnnotation.attributeName(),
				source
		);
	}

	/**
	 * The name of the attribute to which the conversion applies.
	 *
	 * @see Convert#attributeName()
	 */
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * The {@linkplain AttributeConverter converter} implementation class
	 */
	public Class<? extends AttributeConverter<?,?>> getConverterClass() {
		return converterClass;
	}

	/**
	 * Whether conversion is explicitly disabled for this {@linkplain #getAttributeName() attribute}
	 */
	public boolean isConversionDisabled() {
		return conversionDisabled;
	}

	/**
	 * The annotated element
	 */
	public AnnotationTarget getSource() {
		return source;
	}
}
