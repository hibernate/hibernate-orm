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

package org.hibernate.metamodel.source.internal.annotations.attribute.type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
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

	public static HibernateTypeResolver createAttributeTypeResolver(AbstractPersistentAttribute attribute) {
		return new HibernateTypeResolver(
				attribute.getName(),
				attribute.getBackingMember().getType().getErasedType(),
				attribute.getBackingMember().getAnnotations().get( HibernateDotNames.TYPE ),
				attribute.getContext()
		);
	}

	public static HibernateTypeResolver createCollectionElementTypeResolver(PluralAttribute pluralAttribute, JavaTypeDescriptor elementType) {
		return new HibernateTypeResolver(
				pluralAttribute.getName(),
				elementType,
				pluralAttribute.getBackingMember().getAnnotations().get( HibernateDotNames.TYPE ),
				pluralAttribute.getContext()
		);
	}

	public static HibernateTypeResolver createCollectionIndexTypeResolver(PluralAttribute pluralAttribute, JavaTypeDescriptor mapKeyType) {
		// todo : should also handle @MapKeyClass here..
		// todo : even @MapKey since it would indicate a type by the reference attribute
		final AnnotationInstance mapKeyTypeAnnotation = pluralAttribute.getBackingMember()
				.getAnnotations()
				.get( HibernateDotNames.MAP_KEY_TYPE );
		final AnnotationInstance typeAnnotation = mapKeyTypeAnnotation == null
				? null
				: JandexHelper.extractAnnotationValue( mapKeyTypeAnnotation, "value" );
		return new HibernateTypeResolver(
				pluralAttribute.getName(),
				mapKeyType,
				typeAnnotation,
				pluralAttribute.getContext()
		);
	}

	private HibernateTypeResolver(
			String name,
			JavaTypeDescriptor javaType,
			AnnotationInstance annotation,
			EntityBindingContext context) {
		super( name, javaType, annotation, context );

		if ( annotation != null ) {
			this.typeName = JandexHelper.getValue(
					annotation,
					"type",
					String.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class )
			);
		}
		else {
			this.typeName = null;
		}
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
							javaType().getName().toString()
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
