/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.Iterator;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;

/**
 * A source of data on persistent properties of a class or component.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface PersistentPropertiesSource {
	Iterator<Property> getPropertyIterator();

	Property getProperty(String propertyName);

	ClassDetails getClassDetails();

	boolean isDynamicComponent();

	boolean hasCompositeUserType();

	/**
	 * Get a persistent properties source for a persistent class.
	 *
	 * @param persistentClass the persistent class
	 * @param classDetails the class details
	 * @return the properties source
	 */
	static PersistentPropertiesSource forClass(PersistentClass persistentClass, ClassDetails classDetails) {
		return new PersistentPropertiesSource() {
			@Override
			public Iterator<Property> getPropertyIterator() {
				return persistentClass.getProperties().iterator();
			}

			@Override
			public Property getProperty(String propertyName) {
				return persistentClass.getProperty( propertyName );
			}

			@Override
			public ClassDetails getClassDetails() {
				return classDetails;
			}

			@Override
			public boolean isDynamicComponent() {
				return false;
			}

			@Override
			public boolean hasCompositeUserType() {
				return false;
			}
		};
	}

	/**
	 * Get a persistent properties source for a component that needs its class resolved.
	 *
	 * @param context the metadata building context
	 * @param component the component
	 * @param dynamic whether the component is dynamic or not
	 * @return the properties source
	 */
	static PersistentPropertiesSource forComponent(EnversMetadataBuildingContext context, Component component, boolean dynamic) {
		try {
			final ClassDetails classDetails = context.getClassDetailsRegistry()
					.resolveClassDetails( component.getComponentClassName() );
			return forComponent( component, classDetails, dynamic );
		}
		catch (ClassLoadingException e) {
			throw new EnversMappingException( e );
		}
	}

	static PersistentPropertiesSource forComponent(EnversMetadataBuildingContext context, Component component) {
		return forComponent( context, component, false );
	}

	/**
	 * Get a persistent properties source for a component with its class already resolved.
	 *
	 * @param component the component
	 * @param classDetails the class details
	 * @param dynamic whether the component is dynamic or not
	 * @return the properties source
	 */
	static PersistentPropertiesSource forComponent(Component component, ClassDetails classDetails, boolean dynamic) {
		return new PersistentPropertiesSource() {
			@Override
			public Iterator<Property> getPropertyIterator() {
				return component.getProperties().iterator();
			}

			@Override
			public Property getProperty(String propertyName) {
				return component.getProperty( propertyName );
			}

			@Override
			public ClassDetails getClassDetails() {
				return classDetails;
			}

			@Override
			public boolean isDynamicComponent() {
				return dynamic;
			}

			@Override
			public boolean hasCompositeUserType() {
				return component.getTypeName() != null;
			}
		};
	}
}
