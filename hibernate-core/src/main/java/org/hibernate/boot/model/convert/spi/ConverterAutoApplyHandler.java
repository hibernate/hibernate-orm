/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;

import jakarta.persistence.Converter;

/**
 * Manages resolution of auto-applied {@link jakarta.persistence.AttributeConverter}
 * references for specific mappings
 *
 * @see Converter#autoApply()
 *
 * @author Steve Ebersole
 */
public interface ConverterAutoApplyHandler {
	/**
	 * Resolve the auto-applied converter to be applied to a basic attribute described
	 * by the passed property descriptor.  {@code null} indicates that no auto-applied
	 * converter matched
	 *
	 * @param property The HCANN descriptor for the basic attribute
	 */
	ConverterDescriptor findAutoApplyConverterForAttribute(XProperty property, MetadataBuildingContext context);

	/**
	 * Resolve the auto-applied converter to be applied to the elements of a plural attribute
	 * described by the passed property descriptor.  {@code null} indicates that no auto-applied
	 * converter matched
	 *
	 * @param property The HCANN descriptor for the plural attribute
	 */
	ConverterDescriptor findAutoApplyConverterForCollectionElement(XProperty property, MetadataBuildingContext context);

	/**
	 * Resolve the auto-applied converter to be applied to the keys of a plural Map attribute
	 * described by the passed property descriptor.  {@code null} indicates that no auto-applied
	 * converter matched
	 *
	 * @param property The HCANN descriptor for the plural Map attribute
	 */
	ConverterDescriptor findAutoApplyConverterForMapKey(XProperty property, MetadataBuildingContext context);
}
