/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.Incubating;

import jakarta.persistence.AttributeConverter;

/**
 * A registry for JPA {@linkplain AttributeConverter converters}.
 *
 * @author Gavin King
 * @see AttributeConverter
 * @since 6.2
 */
@Incubating
public interface ConverterRegistry {
	/**
	 * Apply the descriptor for an {@link AttributeConverter}
	 */
	void addAttributeConverter(ConverterDescriptor<?,?> descriptor);

	/**
	 * Apply an {@link AttributeConverter}
	 */
	void addAttributeConverter(Class<? extends AttributeConverter<?,?>> converterClass);

	/**
	 * Apply an {@link AttributeConverter} that may be overridden by competing converters
	 */
	void addOverridableConverter(Class<? extends AttributeConverter<?,?>> converterClass);

	void addRegisteredConversion(RegisteredConversion conversion);

	ConverterAutoApplyHandler getAttributeConverterAutoApplyHandler();
}
