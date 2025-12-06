/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.ClassmateContext;

/**
 * Factory for {@link org.hibernate.boot.model.convert.spi.ConverterDescriptor}.
 *
 * @author Gavin King
 */
public class ConverterDescriptors {

	public static <X,Y> ConverterDescriptor<X,Y> of(
			AttributeConverter<X,Y> converterInstance, ClassmateContext classmateContext) {
		return new InstanceBasedConverterDescriptor<>( converterInstance, null, classmateContext );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			AttributeConverter<X,Y> converterInstance, boolean autoApply, ClassmateContext classmateContext) {
		return new InstanceBasedConverterDescriptor<>( converterInstance, autoApply, classmateContext );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			Class<? extends AttributeConverter<? extends X, ? extends Y>> converterClass,
			Boolean autoApply, boolean overrideable, ClassmateContext classmateContext) {
		@SuppressWarnings("unchecked") // work around weird fussiness in wildcard capture
		final var converterType = (Class<? extends AttributeConverter<X, Y>>) converterClass;
		return new ClassBasedConverterDescriptor<>( converterType, autoApply, classmateContext, overrideable );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			Class<? extends AttributeConverter<? extends X, ? extends Y>> converterClass,
			ClassmateContext classmateContext) {
		@SuppressWarnings("unchecked") // work around weird fussiness in wildcard capture
		final var converterType = (Class<? extends AttributeConverter<X, Y>>) converterClass;
		return new ClassBasedConverterDescriptor<>( converterType, null, classmateContext, false );
	}

	public static <X,Y> ConverterDescriptor<X,Y> of(
			Class<? extends AttributeConverter<? extends X, ? extends Y>> converterType,
			ResolvedType domainTypeToMatch, ResolvedType relationalType, boolean autoApply) {
		@SuppressWarnings("unchecked") // work around weird fussiness in wildcard capture
		final var converterClass = (Class<? extends AttributeConverter<X, Y>>) converterType;
		return new ConverterDescriptorImpl<>( converterClass, domainTypeToMatch, relationalType, autoApply );
	}
}
