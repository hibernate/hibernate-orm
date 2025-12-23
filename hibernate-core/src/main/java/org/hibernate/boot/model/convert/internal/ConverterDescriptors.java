/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import jakarta.persistence.AttributeConverter;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;

import java.lang.reflect.Type;

/**
 * Factory for {@link org.hibernate.boot.model.convert.spi.ConverterDescriptor}.
 *
 * @author Gavin King
 */
public class ConverterDescriptors {

	public static <X,Y> ConverterDescriptor<X,Y> of(
			AttributeConverter<X,Y> converterInstance) {
		return new InstanceBasedConverterDescriptor<>( converterInstance, null );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			AttributeConverter<X,Y> converterInstance, boolean autoApply) {
		return new InstanceBasedConverterDescriptor<>( converterInstance, autoApply );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			Class<? extends AttributeConverter<? extends X, ? extends Y>> converterClass,
			Boolean autoApply, boolean overrideable) {
		@SuppressWarnings("unchecked") // work around weird fussiness in wildcard capture
		final var converterType = (Class<? extends AttributeConverter<X, Y>>) converterClass;
		return new ClassBasedConverterDescriptor<>( converterType, autoApply, overrideable );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			Class<? extends AttributeConverter<? extends X, ? extends Y>> converterClass) {
		@SuppressWarnings("unchecked") // work around weird fussiness in wildcard capture
		final var converterType = (Class<? extends AttributeConverter<X, Y>>) converterClass;
		return new ClassBasedConverterDescriptor<>( converterType, null, false );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			Class<? extends AttributeConverter<? extends X, ? extends Y>> converterType,
			Type domainTypeToMatch, Type relationalType, boolean autoApply) {
		@SuppressWarnings("unchecked") // work around weird fussiness in wildcard capture
		final var converterClass = (Class<? extends AttributeConverter<X, Y>>) converterType;
		return new ConverterDescriptorImpl<>( converterClass, domainTypeToMatch, relationalType, autoApply );
	}
}
