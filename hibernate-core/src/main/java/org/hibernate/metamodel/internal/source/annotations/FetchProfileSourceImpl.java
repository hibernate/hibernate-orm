/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.source.FetchProfileSource;

/**
 * @author Steve Ebersole
 */
public class FetchProfileSourceImpl implements FetchProfileSource {
	private final String name;
	private final List<AssociationOverrideSource> associationOverrideSources;

	public FetchProfileSourceImpl(AnnotationInstance fetchProfileAnnotation) {
		this.name = JandexHelper.getValue( fetchProfileAnnotation, "name", String.class );
		this.associationOverrideSources = buildAssociationOverrideSources( fetchProfileAnnotation );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Iterable<AssociationOverrideSource> getAssociationOverrides() {
		return associationOverrideSources;
	}

	private static List<AssociationOverrideSource> buildAssociationOverrideSources(AnnotationInstance fetchProfileAnnotation) {
		final List<AssociationOverrideSource> associationOverrideSources = new ArrayList<AssociationOverrideSource>();
		AnnotationInstance[] overrideAnnotations = JandexHelper.getValue(
				fetchProfileAnnotation,
				"fetchOverrides",
				AnnotationInstance[].class
		);
		for ( AnnotationInstance overrideAnnotation : overrideAnnotations ) {
			associationOverrideSources.add( new AssociationOverrideSourceImpl( overrideAnnotation ) );
		}
		return associationOverrideSources;
	}

	private static class AssociationOverrideSourceImpl implements AssociationOverrideSource {
		private final String entityName;
		private final String attributeName;
		private final String fetchMode;

		private AssociationOverrideSourceImpl(AnnotationInstance overrideAnnotation) {
			this.entityName = JandexHelper.getValue( overrideAnnotation, "entity", String.class );
			this.attributeName = JandexHelper.getValue( overrideAnnotation, "association", String.class );
			FetchMode fetchMode = JandexHelper.getEnumValue( overrideAnnotation, "mode", FetchMode.class );
			if ( !fetchMode.equals( org.hibernate.annotations.FetchMode.JOIN ) ) {
				throw new MappingException( "Only FetchMode.JOIN is currently supported" );
			}
			this.fetchMode = "join";
		}

		@Override
		public String getEntityName() {
			return entityName;
		}

		@Override
		public String getAttributeName() {
			return attributeName;
		}

		@Override
		public String getFetchModeName() {
			return fetchMode;
		}
	}
}
