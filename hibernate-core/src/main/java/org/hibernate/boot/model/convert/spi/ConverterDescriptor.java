/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.spi;

import javax.persistence.AttributeConverter;

import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;

import com.fasterxml.classmate.ResolvedType;

/**
 * Boot-time descriptor of a JPA AttributeConverter
 *
 * @author Steve Ebersole
 */
public interface ConverterDescriptor {
	/**
	 * The AttributeConverter class
	 */
	Class<? extends AttributeConverter> getAttributeConverterClass();

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
	JpaAttributeConverter createJpaAttributeConverter(JpaAttributeConverterCreationContext context);
}
