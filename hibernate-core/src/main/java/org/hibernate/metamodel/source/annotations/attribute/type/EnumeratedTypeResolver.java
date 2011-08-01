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

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.attribute.MappedAttribute;
import org.hibernate.type.EnumType;

/**
 * @author Strong Liu
 */
public class EnumeratedTypeResolver extends AbstractAttributeTypeResolver {
	private final MappedAttribute mappedAttribute;
	private final boolean isMapKey;

	public EnumeratedTypeResolver(MappedAttribute mappedAttribute) {
		if ( mappedAttribute == null ) {
			throw new AssertionFailure( "MappedAttribute is null" );
		}
		this.mappedAttribute = mappedAttribute;
		this.isMapKey = false;//todo
	}

	@Override
	protected AnnotationInstance getTypeDeterminingAnnotationInstance() {
		return JandexHelper.getSingleAnnotation(
				mappedAttribute.annotations(),
				JPADotNames.ENUMERATED
		);
	}

	@Override
	public String resolveHibernateTypeName(AnnotationInstance enumeratedAnnotation) {
		boolean isEnum = mappedAttribute.getAttributeType().isEnum();
		if ( !isEnum ) {
			if ( enumeratedAnnotation != null ) {
				throw new AnnotationException( "Attribute " + mappedAttribute.getName() + " is not a Enumerated type, but has a @Enumerated annotation." );
			}
			else {
				return null;
			}
		}
		return EnumType.class.getName();
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance annotationInstance) {
		HashMap<String, String> typeParameters = new HashMap<String, String>();
		typeParameters.put( EnumType.ENUM, mappedAttribute.getAttributeType().getName() );
		if ( annotationInstance != null ) {
			javax.persistence.EnumType enumType = JandexHelper.getEnumValue(
					annotationInstance,
					"value",
					javax.persistence.EnumType.class
			);
			if ( javax.persistence.EnumType.ORDINAL.equals( enumType ) ) {
				typeParameters.put( EnumType.TYPE, String.valueOf( Types.INTEGER ) );
			}
			else if ( javax.persistence.EnumType.STRING.equals( enumType ) ) {
				typeParameters.put( EnumType.TYPE, String.valueOf( Types.VARCHAR ) );
			}
			else {
				throw new AssertionFailure( "Unknown EnumType: " + enumType );
			}
		}
		else {
			typeParameters.put( EnumType.TYPE, String.valueOf( Types.INTEGER ) );
		}
		return typeParameters;
	}
}
