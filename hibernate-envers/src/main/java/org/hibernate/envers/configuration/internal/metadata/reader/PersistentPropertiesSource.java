/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.Iterator;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.registry.classloading.ClassLoaderAccessHelper;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * A source of data on persistent properties of a class or component.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface PersistentPropertiesSource {
	Iterator<Property> getPropertyIterator();

	Property getProperty(String propertyName);

	XClass getXClass();

	boolean isDynamicComponent();

	boolean hasCompositeUserType();

	/**
	 * Get a persistent properties source for a persistent class.
	 *
	 * @param persistentClass the persistent class
	 * @param clazz the class
	 * @return the properties source
	 */
	static PersistentPropertiesSource forClass(PersistentClass persistentClass, XClass clazz) {
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
			public XClass getXClass() {
				return clazz;
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
			Class<?> componentClass = ClassLoaderAccessHelper.loadClass( context, component.getComponentClassName() );
			XClass clazz = context.getReflectionManager().toXClass( componentClass );
			return forComponent( component, clazz, dynamic );
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
	 * @param clazz the class
	 * @param dynamic whether the component is dynamic or not
	 * @return the properties source
	 */
	static PersistentPropertiesSource forComponent(Component component, XClass clazz, boolean dynamic) {
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
			public XClass getXClass() {
				return clazz;
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
