/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot;

import javax.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Delayed information about an AttributeConverter.  Delayed until we have
 * access to {@link org.hibernate.boot.internal.ClassmateContext} during
 * the MetadataSources -> Metadata process.
 *
 * @author Steve Ebersole
 */
public interface AttributeConverterInfo {
	Class<? extends AttributeConverter> getConverterClass();
	ConverterDescriptor toConverterDescriptor(MetadataBuildingContext context);
}
