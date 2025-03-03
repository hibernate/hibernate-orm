/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

/**
 * Handles {@link DiscriminatorOptions} annotations.
 *
 * @author Gavin King
 *
 * @since 6.5
 */
public class DiscriminatorOptionsBinder implements TypeBinder<DiscriminatorOptions> {
	@Override
	public void bind(DiscriminatorOptions options, MetadataBuildingContext context, PersistentClass persistentClass) {
		if ( persistentClass instanceof RootClass rootClass ) {
			if ( !rootClass.hasDiscriminator() ) {
				throw new AnnotationException( "Root entity '" + rootClass.getEntityName()
						+ "' is annotated '@DiscriminatorOptions' but has no discriminator column" );
			}
			rootClass.setForceDiscriminator( options.force() );
			rootClass.setDiscriminatorInsertable( options.insert() );
		}
		else {
			throw new AnnotationException("Class '" + persistentClass.getClassName()
					+ "' is not the root class of an entity inheritance hierarchy and may not be annotated '@DiscriminatorOptions'");
		}
	}

	@Override
	public void bind(DiscriminatorOptions options, MetadataBuildingContext context, Component embeddableClass) {
		throw new AnnotationException("Class '" + embeddableClass.getComponentClassName()
				+ "' is an '@Embeddable' type and may not be annotated '@DiscriminatorOptions'");
	}
}
