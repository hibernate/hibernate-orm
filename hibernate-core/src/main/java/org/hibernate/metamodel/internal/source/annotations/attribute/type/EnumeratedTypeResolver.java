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

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.type.EnumType;
import org.hibernate.usertype.DynamicParameterizedType;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Strong Liu
 * @author Brett Meyer
 * @author Gail Badner
 */
public class EnumeratedTypeResolver extends AbstractAttributeTypeResolver {
	private final boolean isEnum;
	private final javax.persistence.EnumType enumType;

	public static EnumeratedTypeResolver createAttributeTypeResolver(MappedAttribute attribute) {
		return new EnumeratedTypeResolver(
				attribute.getName(),
				attribute.getAttributeType(),
				resolveAnnotationInstance( attribute.annotations(),JPADotNames.ENUMERATED ),
				attribute.getContext()
		);
	}

	public static EnumeratedTypeResolver createCollectionElementTypeResolver(
			PluralAssociationAttribute pluralAssociationAttribute) {
		return new EnumeratedTypeResolver(
				pluralAssociationAttribute.getName(),
				pluralAssociationAttribute.getReferencedAttributeType(),
				resolveAnnotationInstance( pluralAssociationAttribute.annotations(),JPADotNames.ENUMERATED ),
				pluralAssociationAttribute.getContext()
		);
	}

	public static EnumeratedTypeResolver createCollectionIndexTypeResolver(
			PluralAssociationAttribute pluralAssociationAttribute) {
		return new EnumeratedTypeResolver(
				pluralAssociationAttribute.getName(),
				pluralAssociationAttribute.getIndexType(),
				resolveAnnotationInstance( pluralAssociationAttribute.annotations(), JPADotNames.MAP_KEY_ENUMERATED ),
				pluralAssociationAttribute.getContext()
		);
	}

	private EnumeratedTypeResolver(
			String name,
			Class<?> javaClass,
			AnnotationInstance annotation,
			EntityBindingContext context) {
		super( name, javaClass, annotation, context );
		this.isEnum = javaClass().isEnum();
		this.enumType = annotation == null ?
				null :
				JandexHelper.getEnumValue( annotation, "value", javax.persistence.EnumType.class,
						getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
	}

	@Override
	public String resolveHibernateTypeName() {
		if ( annotation() != null ) {
			if ( isEnum ) {
				return EnumType.class.getName();
			} else {
				throw new AnnotationException(
						String.format(
								"Attribute %s is not a Enumerated type, but has %s or %s annotation.",
								name(),
								JPADotNames.ENUMERATED,
								JPADotNames.MAP_KEY_ENUMERATED
						)
				);
			}
		} 
		else if ( !hasTypeDef() && isEnum ) {
			return EnumType.class.getName();
		}
		return null;
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters() {
		HashMap<String, String> typeParameters = new HashMap<String, String>();

		if ( enumType != null ) {
			typeParameters.put(EnumType.ENUM, javaClass().getName() );
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
		typeParameters.put( DynamicParameterizedType.RETURNED_CLASS, javaClass().getName() );
		return typeParameters;
	}
}
