/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.spi.DiscriminatorSource;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.DISCRIMINATOR_OPTIONS;

/**
 * @author Steve Ebersole
 */
public abstract class DiscriminatorSourceSupport implements DiscriminatorSource {
	private final boolean forced;
	private final boolean includedInInsert;

	public DiscriminatorSourceSupport(EntityTypeMetadata entityTypeMetadata) {
		final AnnotationInstance discriminatorOptions = entityTypeMetadata.getJavaTypeDescriptor()
				.findTypeAnnotation( DISCRIMINATOR_OPTIONS );

		this.forced = determineWhetherForced( discriminatorOptions );
		this.includedInInsert = determineWhetherToIncludeInInsert( discriminatorOptions );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private static boolean determineWhetherForced(AnnotationInstance discriminatorOptions) {
		if ( discriminatorOptions == null ) {
			return false;
		}

		final AnnotationValue forcedValue = discriminatorOptions.value( "force" );
		if ( forcedValue == null ) {
			return false;
		}

		return forcedValue.asBoolean();
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private static boolean determineWhetherToIncludeInInsert(AnnotationInstance discriminatorOptions) {
		if ( discriminatorOptions == null ) {
			return true;
		}

		final AnnotationValue insertValue = discriminatorOptions.value( "insert" );
		if ( insertValue == null ) {
			return true;
		}

		return insertValue.asBoolean();
	}

	@Override
	public boolean isForced() {
		return forced;
	}

	@Override
	public boolean isInserted() {
		return includedInInsert;
	}
}
