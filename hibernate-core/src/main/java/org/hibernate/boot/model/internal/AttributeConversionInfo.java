/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.common.reflection.XAnnotatedElement;

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

	private final XAnnotatedElement source;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AttributeConversionInfo(
			Class<? extends AttributeConverter> converterClass,
			boolean conversionDisabled,
			String attributeName,
			XAnnotatedElement source) {
		this.converterClass = (Class<? extends AttributeConverter<?, ?>>) converterClass;
		this.conversionDisabled = conversionDisabled;
		this.attributeName = attributeName;
		this.source = source;
	}

	@SuppressWarnings("unchecked")
	public AttributeConversionInfo(Convert convertAnnotation, XAnnotatedElement xAnnotatedElement) {
		this(
				convertAnnotation.converter(),
				convertAnnotation.disableConversion(),
				convertAnnotation.attributeName(),
				xAnnotatedElement
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
	public XAnnotatedElement getSource() {
		return source;
	}
}
