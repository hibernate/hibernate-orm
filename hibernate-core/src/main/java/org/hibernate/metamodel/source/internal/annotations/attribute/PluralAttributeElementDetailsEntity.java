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

import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementDetailsEntity implements PluralAttributeElementDetails {
	private final ClassDescriptor javaType;
	private final PluralAttributeElementNature elementNature;
	private final AttributeTypeResolver typeResolver;

	public PluralAttributeElementDetailsEntity(
			PluralAttribute pluralAttribute,
			JavaTypeDescriptor inferredElementType) {
		this.javaType = determineJavaType( pluralAttribute, inferredElementType );

		if ( this.javaType == null ) {
			throw pluralAttribute.getContext().makeMappingException(
					"Could not determine element type information for plural attribute : "
							+ pluralAttribute.getBackingMember().toString()
			);
		}

		this.elementNature = decodeElementNature( pluralAttribute );
		this.typeResolver = buildTypeResolver( pluralAttribute, javaType );
	}

	private ClassDescriptor determineJavaType(
			PluralAttribute pluralAttribute,
			JavaTypeDescriptor inferredElementType) {

		// NOTE : ClassDescriptor because JPA says an entity must be a class, not an interface

		final AnnotationInstance targetAnnotation = pluralAttribute.getBackingMember()
				.getAnnotations()
				.get( HibernateDotNames.TARGET );
		if ( targetAnnotation != null ) {
			final AnnotationValue targetValue = targetAnnotation.value();
			if ( targetValue != null ) {
				return (ClassDescriptor) pluralAttribute.getContext().getJavaTypeDescriptorRepository().getType(
						targetValue.asClass().name()
				);
			}
		}

		final AnnotationInstance oneToManyAnnotation = pluralAttribute.getBackingMember()
				.getAnnotations()
				.get( JPADotNames.ONE_TO_MANY );
		if ( oneToManyAnnotation != null ) {
			final AnnotationValue targetClassValue = oneToManyAnnotation.value( "targetEntity" );
			if ( targetClassValue != null ) {
				return (ClassDescriptor) pluralAttribute.getContext().getJavaTypeDescriptorRepository().getType(
						targetClassValue.asClass().name()
				);
			}
		}

		final AnnotationInstance manyToManyAnnotation = pluralAttribute.getBackingMember()
				.getAnnotations()
				.get( JPADotNames.MANY_TO_MANY );
		if ( manyToManyAnnotation != null ) {
			final AnnotationValue targetClassValue = manyToManyAnnotation.value( "targetEntity" );
			if ( targetClassValue != null ) {
				return (ClassDescriptor) pluralAttribute.getContext().getJavaTypeDescriptorRepository().getType(
						targetClassValue.asClass().name()
				);
			}
		}

		return (ClassDescriptor) inferredElementType;
	}

	private PluralAttributeElementNature decodeElementNature(PluralAttribute pluralAttribute) {
		switch ( pluralAttribute.getNature() ) {
			case MANY_TO_ANY: {
				return PluralAttributeElementNature.MANY_TO_ANY;
			}
			case MANY_TO_MANY: {
				return PluralAttributeElementNature.MANY_TO_MANY;
			}
			case ONE_TO_MANY: {
				return PluralAttributeElementNature.ONE_TO_MANY;
			}
			default: {
				throw pluralAttribute.getContext().makeMappingException(
						"Unexpected plural attribute nature : " + pluralAttribute.getNature()
				);
			}
		}
	}

	private AttributeTypeResolver buildTypeResolver(PluralAttribute pluralAttribute, ClassDescriptor javaType) {
		// todo : No idea what this should be for entities : return the entity name as type name?  return no type name?
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaType() {
		return javaType;
	}

	@Override
	public PluralAttributeElementNature getElementNature() {
		return elementNature;
	}

	@Override
	public AttributeTypeResolver getTypeResolver() {
		return typeResolver;
	}
}
