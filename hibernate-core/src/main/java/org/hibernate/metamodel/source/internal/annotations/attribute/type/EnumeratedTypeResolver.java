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

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.reflite.internal.ModifierUtils;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.type.EnumType;
import org.hibernate.usertype.DynamicParameterizedType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Strong Liu
 * @author Brett Meyer
 * @author Gail Badner
 */
public class EnumeratedTypeResolver extends AbstractAttributeTypeResolver {

	public static EnumeratedTypeResolver createAttributeTypeResolver(AbstractPersistentAttribute attribute) {
		return new EnumeratedTypeResolver(
				attribute.getName(),
				attribute.getBackingMember().getType().getErasedType(),
				attribute.getBackingMember().getAnnotations().get( JPADotNames.ENUMERATED ),
				attribute.getContext()
		);
	}

	public static EnumeratedTypeResolver createCollectionElementTypeResolver(PluralAttribute pluralAttribute, JavaTypeDescriptor elementType) {
		return new EnumeratedTypeResolver(
				pluralAttribute.getName(),
				elementType,
				pluralAttribute.getBackingMember().getAnnotations().get( JPADotNames.ENUMERATED ),
				pluralAttribute.getContext()
		);
	}

	public static EnumeratedTypeResolver createCollectionIndexTypeResolver(PluralAttribute pluralAttribute, JavaTypeDescriptor mapKeyType) {
		return new EnumeratedTypeResolver(
				pluralAttribute.getName(),
				mapKeyType,
				pluralAttribute.getBackingMember().getAnnotations().get( JPADotNames.MAP_KEY_ENUMERATED ),
				pluralAttribute.getContext()
		);
	}

	private final String typeName;
	private final Map<String, String> typeParameters;

	private EnumeratedTypeResolver(
			String name,
			JavaTypeDescriptor javaType,
			AnnotationInstance annotation,
			EntityBindingContext context) {
		super( name, javaType, annotation, context );

		final boolean isEnum = ModifierUtils.isEnum( javaType );
		if ( !isEnum ) {
			if ( annotation != null ) {
				throw new AnnotationException(
						String.format(
								"Attribute %s is not a enumerated type, but has %s annotation.",
								name(),
								annotation.name().toString()
						)
				);
			}

			this.typeName = null;
			this.typeParameters = Collections.emptyMap();
		}
		else {
			if ( annotation != null ) {
				this.typeName = EnumType.class.getName();
				this.typeParameters = determineTypeParameters( annotation );
			}
			else {
				final TypeDefinition typeDefinition = getTypeDefinition();
				if ( typeDefinition != null ) {
					this.typeName = typeDefinition.getTypeImplementorClass().getName();
					this.typeParameters = determineTypeParameters( null );
					if ( typeDefinition.getParameters() != null
							&& !typeDefinition.getParameters().isEmpty() ) {
						typeParameters.putAll( typeDefinition.getParameters() );
					}
				}
				else {
					this.typeName = EnumType.class.getName();
					this.typeParameters = determineTypeParameters( null );
				}
			}
		}
	}

	private Map<String, String> determineTypeParameters(AnnotationInstance annotation) {
		javax.persistence.EnumType enumType = javax.persistence.EnumType.ORDINAL;
		if ( annotation != null ) {
			final AnnotationValue enumTypeValue = annotation.value();
			if ( enumTypeValue != null ) {
				enumType = javax.persistence.EnumType.valueOf( enumTypeValue.asEnum() );
			}
		}

		HashMap<String, String> typeParameters = new HashMap<String, String>();
		if ( javax.persistence.EnumType.ORDINAL.equals( enumType ) ) {
			typeParameters.put( EnumType.TYPE, String.valueOf( java.sql.Types.INTEGER ) );
			typeParameters.put( EnumType.NAMED, String.valueOf( false ) );
		}
		else if ( javax.persistence.EnumType.STRING.equals( enumType ) ) {
			typeParameters.put( EnumType.TYPE, String.valueOf( java.sql.Types.VARCHAR ) );
			typeParameters.put( EnumType.NAMED, String.valueOf( true ) );
		}
		typeParameters.put( EnumType.ENUM, javaType().getName().toString() );
		// todo : this may be trouble. doesn't this expect the the Class reference?
		typeParameters.put( DynamicParameterizedType.RETURNED_CLASS, javaType().getName().toString() );
		return typeParameters;
	}

	@Override
	public String resolveHibernateTypeName() {
		return typeName;
	}

	@Override
	protected Map<String, String> resolveHibernateTypeParameters() {
		return typeParameters;
	}
}
