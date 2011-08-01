/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.metamodel.source.annotations.attribute.type;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.attribute.MappedAttribute;

/**
 * @author Strong Liu
 */
public class AttributeTypeResolverImpl extends AbstractAttributeTypeResolver {
	private final MappedAttribute mappedAttribute;

	public AttributeTypeResolverImpl(MappedAttribute mappedAttribute) {
		this.mappedAttribute = mappedAttribute;
	}

	@Override
	protected String resolveHibernateTypeName(AnnotationInstance typeAnnotation) {
		String typeName = null;
		if ( typeAnnotation != null ) {
			typeName = JandexHelper.getValue( typeAnnotation, "type", String.class );
		}
		return typeName;
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance typeAnnotation) {
		HashMap<String, String> typeParameters = new HashMap<String, String>();
		AnnotationValue parameterAnnotationValue = typeAnnotation.value( "parameters" );
		if ( parameterAnnotationValue != null ) {
			AnnotationInstance[] parameterAnnotations = parameterAnnotationValue.asNestedArray();
			for ( AnnotationInstance parameterAnnotationInstance : parameterAnnotations ) {
				typeParameters.put(
						JandexHelper.getValue( parameterAnnotationInstance, "name", String.class ),
						JandexHelper.getValue( parameterAnnotationInstance, "value", String.class )
				);
			}
		}
		return typeParameters;
	}

	@Override
	protected AnnotationInstance getTypeDeterminingAnnotationInstance() {
		return JandexHelper.getSingleAnnotation(
				mappedAttribute.annotations(),
				HibernateDotNames.TYPE
		);
	}
}
