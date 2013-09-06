/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
