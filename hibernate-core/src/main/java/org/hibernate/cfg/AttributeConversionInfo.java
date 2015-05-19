/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;

import org.hibernate.annotations.common.reflection.XAnnotatedElement;

/**
 * Describes a {@link javax.persistence.Convert} conversion
 *
 * @author Steve Ebersole
 */
public class AttributeConversionInfo {
	private final Class<? extends AttributeConverter> converterClass;
	private final boolean conversionDisabled;

	private final String attributeName;

	private final XAnnotatedElement source;

	public AttributeConversionInfo(
			Class<? extends AttributeConverter> converterClass,
			boolean conversionDisabled,
			String attributeName,
			XAnnotatedElement source) {
		this.converterClass = converterClass;
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

	public Class<? extends AttributeConverter> getConverterClass() {
		return converterClass;
	}

	public boolean isConversionDisabled() {
		return conversionDisabled;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public XAnnotatedElement getSource() {
		return source;
	}
}
