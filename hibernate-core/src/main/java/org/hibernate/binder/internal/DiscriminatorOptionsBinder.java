/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		if ( persistentClass instanceof RootClass ) {
			final RootClass rootClass = (RootClass) persistentClass;
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
