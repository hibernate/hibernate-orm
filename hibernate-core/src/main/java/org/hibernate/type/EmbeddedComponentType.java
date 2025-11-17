/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.mapping.Component;

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

		final var embeddable = mappingModelPart().getEmbeddableTypeDescriptor();
		for ( int i = 0; i < embeddable.getNumberOfAttributeMappings(); i++ ) {
			final var attributeMapping = embeddable.getAttributeMapping( i );
			final var getterMethod =
					attributeMapping.getPropertyAccess()
							.getGetter().getMethod();
			if ( getterMethod != null && getterMethod.equals( method ) ) {
				return true;
			}
		}

		return false;
	}
}
