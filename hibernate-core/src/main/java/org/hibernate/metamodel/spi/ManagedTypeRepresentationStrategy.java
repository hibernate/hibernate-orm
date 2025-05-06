/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Defines a singular extension point for capabilities pertaining to
 * a representation mode.  Acts as a factory for delegates encapsulating
 * these capabilities.
 *
 * @see org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver
 */
@Incubating
public interface ManagedTypeRepresentationStrategy {
	/**
	 * The mode represented
	 */
	RepresentationMode getMode();

	/**
	 * The reflection optimizer to use for this embeddable.
	 */
	ReflectionOptimizer getReflectionOptimizer();

	/**
	 * The Java type descriptor for the concrete type.  For dynamic-map models
	 * this will return the JTD for java.util.Map
	 */
	JavaType<?> getMappedJavaType();

	/**
	 * Create the property accessor object for the specified attribute
	 */
	PropertyAccess resolvePropertyAccess(Property bootAttributeDescriptor);
}
