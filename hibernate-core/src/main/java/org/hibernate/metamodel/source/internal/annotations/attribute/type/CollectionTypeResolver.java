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
package org.hibernate.metamodel.source.internal.annotations.attribute.type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

import org.jboss.jandex.AnnotationInstance;

/**
 * Resolver for finding the Hibernate Type information as indicated for a plural attribute
 * via the {@link org.hibernate.annotations.CollectionType} annotation
 *
 * @author Steve Ebersole
 */
public class CollectionTypeResolver implements AttributeTypeResolver {
	private final String explicitTypeName;
	private final Map<String,String> explicitTypeParameters;

	public CollectionTypeResolver(PluralAttribute pluralAttribute) {
		final AnnotationInstance collectionTypeAnnotation = pluralAttribute.getBackingMember()
				.getAnnotations()
				.get( HibernateDotNames.COLLECTION_TYPE );
		if ( collectionTypeAnnotation == null ) {
			this.explicitTypeName = null;
			this.explicitTypeParameters = Collections.emptyMap();
		}
		else {
			this.explicitTypeName = StringHelper.nullIfEmpty( collectionTypeAnnotation.value( "type" ).asString() );
			if ( explicitTypeName == null ) {
				throw pluralAttribute.getContext().makeMappingException(
						"Encountered @CollectionType annotation that did not specify type : "
								+ pluralAttribute.getBackingMember().toString()
				);
			}

			final AnnotationInstance[] parameterAnnotations = JandexHelper.extractAnnotationsValue(
					collectionTypeAnnotation,
					"parameters"
			);
			if ( parameterAnnotations == null || parameterAnnotations.length == 0 ) {
				this.explicitTypeParameters = Collections.emptyMap();
			}
			else {
				this.explicitTypeParameters = new HashMap<String, String>();
				for ( AnnotationInstance parameterAnnotation : parameterAnnotations ) {
					this.explicitTypeParameters.put(
							parameterAnnotation.value( "name" ).asString(),
							parameterAnnotation.value( "value" ).asString()
					);
				}
			}
		}
	}

	@Override
	public String getExplicitHibernateTypeName() {
		return explicitTypeName;
	}

	@Override
	public String getExplicitAnnotatedHibernateTypeName() {
		return explicitTypeName;
	}

	@Override
	public Map<String, String> getExplicitHibernateTypeParameters() {
		return explicitTypeParameters;
	}
}
