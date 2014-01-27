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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.usertype.DynamicParameterizedType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * Type Resolver which checks {@link org.hibernate.annotations.Type} to find the type info.
 *
 * @author Strong Liu
 * @author Brett Meyer
 * @author Gail Badner
 */
public class HibernateTypeResolver extends AbstractAttributeTypeResolver {
	private final String typeName;

	public static HibernateTypeResolver createAttributeTypeResolver(MappedAttribute attribute) {
		return new HibernateTypeResolver(
				attribute.getName(),
				attribute.getAttributeType(),
				resolveAnnotationInstance( attribute.annotations(), HibernateDotNames.TYPE ),
				attribute.getContext()
		);
	}

	public static HibernateTypeResolver createCollectionElementTypeResolver(
			PluralAssociationAttribute pluralAssociationAttribute) {
		return new HibernateTypeResolver(
				pluralAssociationAttribute.getName(),
				pluralAssociationAttribute.getReferencedAttributeType(),
				resolveAnnotationInstance( pluralAssociationAttribute.annotations(), HibernateDotNames.TYPE ),
				pluralAssociationAttribute.getContext()
		);
	}

	public static HibernateTypeResolver createCollectionIndexTypeResolver(
			PluralAssociationAttribute pluralAssociationAttribute) {
		final AnnotationInstance annotation = resolveAnnotationInstance(
				pluralAssociationAttribute.annotations(),
				HibernateDotNames.MAP_KEY_TYPE
		);
		final AnnotationInstance typeAnnotation = annotation == null ?
				null :
				JandexHelper.getValue( annotation, "value", AnnotationInstance.class,
						pluralAssociationAttribute.getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		return new HibernateTypeResolver(
				pluralAssociationAttribute.getName(),
				pluralAssociationAttribute.getIndexType(),
				typeAnnotation,
				pluralAssociationAttribute.getContext()
		);
	}

	private HibernateTypeResolver(
			String name,
			Class<?> javaClass,
			AnnotationInstance annotation,
			EntityBindingContext context) {
		super( name, javaClass, annotation, context );
		this.typeName =
				annotation() == null ?
						null :
						JandexHelper.getValue( annotation(), "type", String.class,
								getContext().getServiceRegistry().getService( ClassLoaderService.class ) );

	}

	@Override
	protected String resolveHibernateTypeName() {
		return typeName;
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters() {
		if ( annotation() != null ) {
			AnnotationValue parameterAnnotationValue = annotation().value( "parameters" );
			if ( parameterAnnotationValue != null ) {
				AnnotationInstance[] parameterAnnotations = parameterAnnotationValue.asNestedArray();
				final HashMap<String, String> typeParameters = new HashMap<String, String>( parameterAnnotations.length );
				for ( AnnotationInstance parameterAnnotationInstance : parameterAnnotations ) {
					typeParameters.put(
							JandexHelper.getValue( parameterAnnotationInstance, "name", String.class,
									getContext().getServiceRegistry().getService( ClassLoaderService.class ) ),
							JandexHelper.getValue( parameterAnnotationInstance, "value", String.class,
									getContext().getServiceRegistry().getService( ClassLoaderService.class ) )
					);
				}
				return typeParameters;
			}
			else if ( typeName != null && !hasTypeDef() ) {
				try {
					final Class<?> typeClass =
							getContext().getServiceRegistry().getService( ClassLoaderService.class ).classForName( typeName );
					if ( DynamicParameterizedType.class.isAssignableFrom( typeClass ) ) {
						return Collections.singletonMap(
							DynamicParameterizedType.RETURNED_CLASS,
							javaClass().getName()
						);
					}
				}
				catch (ClassLoadingException ex ) {
					// ignore
				}
			}
		}

		return Collections.emptyMap();
	}
}
