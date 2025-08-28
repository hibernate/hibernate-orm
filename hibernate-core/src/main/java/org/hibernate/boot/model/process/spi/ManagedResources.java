/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.spi;

import java.util.Collection;
import java.util.Map;

import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;

/**
 * Represents the result of the first step of the process of building {@link org.hibernate.boot.MetadataSources}
 * reference into a {@link org.hibernate.boot.Metadata} reference.
 * <p>
 * Essentially it represents the combination of:<ol>
 *     <li>domain classes, packages and mapping files defined via MetadataSources</li>
 *     <li>attribute converters defined via MetadataBuildingOptions</li>
 *     <li>classes, converters, packages and mapping files auto-discovered as part of scanning</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public interface ManagedResources {

	/**
	 * Informational access to the AttributeConverter definitions known about.  Changes to made to
	 * the returned list have no effect.
	 *
	 * @return The AttributeConverter definitions.
	 */
	Collection<ConverterDescriptor<?,?>> getAttributeConverterDescriptors();

	/**
	 * Informational access to any entity and component classes in the user domain model known by Class
	 * reference .  Changes to made to the returned list have no effect.
	 *
	 * @return The list of entity/component classes known by Class reference.
	 */
	Collection<Class<?>> getAnnotatedClassReferences();

	/**
	 * Informational access to any entity and component classes in the user domain model known by name.
	 * Changes to made to the returned list have no effect.
	 *
	 * @return The list of entity/component classes known by name.
	 */
	Collection<String> getAnnotatedClassNames();

	/**
	 * Informational access to any known annotated package names (packages with a {@code package-info.class}
	 * file that Hibernate has been told about).  Changes to made to the returned list have no effect.
	 *
	 * @return The list of known annotated package names.
	 */
	Collection<String> getAnnotatedPackageNames();

	/**
	 * Informational access to binding for all known XML mapping files.  Changes to made to the returned
	 * list have no effect.
	 *
	 * @return The list of bindings for all known XML mapping files.
	 */
	Collection<Binding<? extends JaxbBindableMappingDescriptor>> getXmlMappingBindings();

	Map<String,Class<?>> getExtraQueryImports();
}
