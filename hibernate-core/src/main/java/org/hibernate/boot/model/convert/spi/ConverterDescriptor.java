/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;

/**
 * Boot-time descriptor of a JPA {@linkplain AttributeConverter converter}.
 *
 * @author Steve Ebersole
 *
 * @param <X> The entity attribute type
 * @param <Y> The converted type
 *
 * @see AttributeConverter
 * @see ConverterRegistry
 */
public interface ConverterDescriptor<X,Y> {
	String TYPE_NAME_PREFIX = "converted::";

	/**
	 * The class of the JPA {@link AttributeConverter}.
	 */
	Class<? extends AttributeConverter<X,Y>> getAttributeConverterClass();

	/**
	 * The resolved Classmate type descriptor for the conversion's domain type
	 */
	ResolvedType getDomainValueResolvedType();

	/**
	 * The resolved Classmate type descriptor for the conversion's relational type
	 */
	ResolvedType getRelationalValueResolvedType();

	/**
	 * Get the auto-apply checker for this converter.
	 * <p>
	 * Should never return {@code null}. If the converter is not auto-applied, return
	 * {@link org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl#INSTANCE}
	 * instead.
	 */
	AutoApplicableConverterDescriptor getAutoApplyDescriptor();

	/**
	 * Factory for the runtime representation of the converter
	 */
	JpaAttributeConverter<X,Y> createJpaAttributeConverter(JpaAttributeConverterCreationContext context);

	/**
	 * Can this converter be overridden by other competing converters?
	 */
	default boolean overrideable() {
		return false;
	}
}
