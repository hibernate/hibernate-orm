/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;

/**
 * Boot-time descriptor of a JPA AttributeConverter
 *
 * @author Steve Ebersole
 */
public interface ConverterDescriptor {
	String TYPE_NAME_PREFIX = "converted::";

	/**
	 * The AttributeConverter class
	 */
	Class<? extends AttributeConverter<?,?>> getAttributeConverterClass();

	/**
	 * The resolved Classmate type descriptor for the conversion's domain type
	 */
	ResolvedType getDomainValueResolvedType();

	/**
	 * The resolved Classmate type descriptor for the conversion's relational type
	 */
	ResolvedType getRelationalValueResolvedType();

	/**
	 * Get the auto-apply checker for this converter.  Should never return `null` - prefer
	 * {@link org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl#INSTANCE}
	 * instead.
	 */
	AutoApplicableConverterDescriptor getAutoApplyDescriptor();

	/**
	 * Factory for the runtime representation of the converter
	 */
	JpaAttributeConverter<?,?> createJpaAttributeConverter(JpaAttributeConverterCreationContext context);

	/**
	 * Can this converter be overridden by other competing converters?
	 */
	default boolean overrideable() {
		return false;
	}
}
