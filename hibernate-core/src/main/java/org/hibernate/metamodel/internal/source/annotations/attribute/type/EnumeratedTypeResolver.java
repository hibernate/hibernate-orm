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

package org.hibernate.metamodel.internal.source.annotations.attribute.type;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.type.EnumType;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class EnumeratedTypeResolver extends AbstractAttributeTypeResolver {
	private final boolean isMapKey;
	private final boolean isEnum;
//	private final String attributeType;

	public EnumeratedTypeResolver(MappedAttribute mappedAttribute) {
		super( mappedAttribute );
		isEnum = mappedAttribute.getAttributeType().isEnum();
		isMapKey = false;//todo
//		attributeType = mappedAttribute.getAttributeType().getName();
	}

	public EnumeratedTypeResolver(PluralAssociationAttribute pluralAssociationAttribute) {
		super( pluralAssociationAttribute );
		isEnum = pluralAssociationAttribute.getReferencedAttributeType().isEnum();
		isMapKey = false;//todo
//		attributeType = pluralAssociationAttribute.getReferencedAttributeType().getName();
	}

	@Override
	protected AnnotationInstance getTypeDeterminingAnnotationInstance() {
		return JandexHelper.getSingleAnnotation(
				mappedAttribute.annotations(),
				JPADotNames.ENUMERATED
		);
	}

	@Override
	public String resolveAnnotatedHibernateTypeName(AnnotationInstance enumeratedAnnotation) {
		if ( enumeratedAnnotation != null ) {
			if ( isEnum ) {
				return EnumType.class.getName();
			} else {
				throw new AnnotationException( "Attribute " + mappedAttribute.getName() + " is not a Enumerated type, but has a @Enumerated annotation." );
			}
		} 
		else if ( !hasEntityTypeDef() && isEnum ) {
			return EnumType.class.getName();
		}
		return null;
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance annotationInstance) {
		HashMap<String, String> typeParameters = new HashMap<String, String>();
		if ( annotationInstance != null ) {
			javax.persistence.EnumType enumType = JandexHelper.getEnumValue(
					annotationInstance,
					"value",
					javax.persistence.EnumType.class
			);
			if ( javax.persistence.EnumType.ORDINAL.equals( enumType ) ) {
				typeParameters.put( EnumType.TYPE, String.valueOf( Types.INTEGER ) );
				typeParameters.put( EnumType.NAMED, String.valueOf( false ) );
			}
			else if ( javax.persistence.EnumType.STRING.equals( enumType ) ) {
				typeParameters.put( EnumType.TYPE, String.valueOf( Types.VARCHAR ) );
				typeParameters.put( EnumType.NAMED, String.valueOf( true ) );
			}
			else {
				throw new AssertionFailure( "Unknown EnumType: " + enumType );
			}
		}
		else {
			typeParameters.put( EnumType.TYPE, String.valueOf( Types.INTEGER ) );
		}
//		typeParameters.put( DynamicParameterizedType.RETURNED_CLASS, attributeType );
		return typeParameters;
	}
}
