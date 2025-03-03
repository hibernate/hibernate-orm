/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.property.access.spi.Getter;

/**
 * @author Gavin King
 */
public class EmbeddedComponentType extends ComponentType {

	public EmbeddedComponentType(Component component, int[] originalPropertyOrder) {
		super( component, originalPropertyOrder );
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}

	@Override
	public boolean isMethodOf(Method method) {
		if ( mappingModelPart() == null ) {
			throw new IllegalStateException( "EmbeddableValuedModelPart not known yet" );
		}

		final EmbeddableMappingType embeddable = mappingModelPart().getEmbeddableTypeDescriptor();
		for ( int i = 0; i < embeddable.getNumberOfAttributeMappings(); i++ ) {
			final AttributeMapping attributeMapping = embeddable.getAttributeMapping( i );
			final Getter getter = attributeMapping.getPropertyAccess().getGetter();
			final Method getterMethod = getter.getMethod();
			if ( getterMethod != null && getterMethod.equals( method ) ) {
				return true;
			}
		}

		return false;
	}
}
