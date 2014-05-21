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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolverComposition;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.EnumeratedTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.HibernateTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.LobTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.TemporalTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementDetailsBasic implements PluralAttributeElementDetails {
	private final JavaTypeDescriptor javaType;
	private final AttributeTypeResolver typeResolver;

	public PluralAttributeElementDetailsBasic(
			PluralAttribute pluralAttribute,
			JavaTypeDescriptor inferredElementType) {
		this.javaType = determineJavaType( pluralAttribute, inferredElementType );
		this.typeResolver = buildTypeResolver( pluralAttribute, javaType );
	}

	private static JavaTypeDescriptor determineJavaType(PluralAttribute pluralAttribute, JavaTypeDescriptor elementType) {
		if ( elementType != null ) {
			return elementType;
		}

		final AnnotationInstance annotation = pluralAttribute.getBackingMember().getAnnotations().get(
				JPADotNames.ELEMENT_COLLECTION );
		if ( annotation == null ) {
			throw pluralAttribute.getContext().makeMappingException(
					"Could not determine element type information for plural attribute ["
							+ pluralAttribute.getBackingMember().toString()
							+ "]; could not locate @ElementCollection annotation"
			);
		}

		DotName dotName = null;
		final AnnotationValue targetClassValue = annotation.value( "targetClass" );
		if ( targetClassValue == null ) {
			final AnnotationInstance typeAnnotation = pluralAttribute.getBackingMember().getAnnotations().get(
					HibernateDotNames.TYPE );
			if (typeAnnotation != null) {
				dotName = DotName.createSimple( typeAnnotation.value( "type" ).asString() );
			}
			else {
				throw pluralAttribute.getContext().makeMappingException(
						"Could not determine element type information for plural attribute ["
								+ pluralAttribute.getBackingMember().toString()
								+ "]; Either specify targetClass in @ElementCollection or provide @Type"
				);
			}
		}
		else {
			dotName = targetClassValue.asClass().name();
		}

		return pluralAttribute.getContext().getJavaTypeDescriptorRepository().getType( dotName );
	}

	private static AttributeTypeResolver buildTypeResolver(PluralAttribute pluralAttribute, JavaTypeDescriptor javaType) {
		return new AttributeTypeResolverComposition(
				javaType,
				pluralAttribute.getContext(),
				HibernateTypeResolver.createCollectionElementTypeResolver( pluralAttribute, javaType ),
				TemporalTypeResolver.createCollectionElementTypeResolver( pluralAttribute, javaType ),
				LobTypeResolver.createCollectionElementTypeResolve( pluralAttribute, javaType ),
				EnumeratedTypeResolver.createCollectionElementTypeResolver( pluralAttribute, javaType )
		);
	}

	@Override
	public JavaTypeDescriptor getJavaType() {
		return javaType;
	}

	@Override
	public PluralAttributeElementNature getElementNature() {
		return PluralAttributeElementNature.BASIC;
	}

	@Override
	public AttributeTypeResolver getTypeResolver() {
		return typeResolver;
	}
}
