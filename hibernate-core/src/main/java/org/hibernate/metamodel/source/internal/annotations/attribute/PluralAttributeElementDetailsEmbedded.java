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
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.AttributeConversionInfo;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolverComposition;
import org.hibernate.metamodel.source.internal.annotations.entity.EmbeddableTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementDetailsEmbedded implements PluralAttributeElementDetails, EmbeddedContainer {
	private final PluralAttribute pluralAttribute;

	private final ClassDescriptor javaType;
	private final EmbeddableTypeMetadata embeddableTypeMetadata;
	private final AttributeTypeResolver typeResolver;

	public PluralAttributeElementDetailsEmbedded(
			PluralAttribute pluralAttribute,
			JavaTypeDescriptor inferredElementType) {
		this.pluralAttribute = pluralAttribute;
		this.javaType = determineJavaType( pluralAttribute, inferredElementType );

		if ( this.javaType == null ) {
			throw pluralAttribute.getContext().makeMappingException(
					"Could not determine element type information for plural attribute : "
							+ pluralAttribute.getBackingMember().toString()
			);
		}

		this.embeddableTypeMetadata = buildEmbeddedMetadata( pluralAttribute, javaType );
		this.typeResolver = new AttributeTypeResolverComposition( javaType, pluralAttribute.getContext() );
	}

	private static ClassDescriptor determineJavaType(
			PluralAttribute pluralAttribute,
			JavaTypeDescriptor inferredElementType) {

		// NOTE : ClassDescriptor because atm only classes can be embedded/embeddable

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

		final AnnotationInstance elementCollectionAnnotation = pluralAttribute.getBackingMember()
				.getAnnotations()
				.get( JPADotNames.ELEMENT_COLLECTION );
		if ( elementCollectionAnnotation != null ) {
			final AnnotationValue targetClassValue = elementCollectionAnnotation.value( "targetClass" );
			if ( targetClassValue != null ) {
				return (ClassDescriptor) pluralAttribute.getContext().getJavaTypeDescriptorRepository().getType(
						targetClassValue.asClass().name()
				);
			}
		}

		return (ClassDescriptor) inferredElementType;
	}

	private EmbeddableTypeMetadata buildEmbeddedMetadata(PluralAttribute pluralAttribute, ClassDescriptor javaType) {
		final boolean isMap = pluralAttribute.getPluralAttributeNature() == PluralAttributeNature.MAP;
		final AttributeRole role = isMap
				? pluralAttribute.getRole().append( "value" )
				: pluralAttribute.getRole().append( "element" );
		final AttributePath path = isMap
				? pluralAttribute.getPath().append( "value" )
				: pluralAttribute.getPath();

		// we pass `this` (as EmbeddedContainer) in order to route calls back properly.
		return new EmbeddableTypeMetadata(
				javaType,
				this,
				role,
				path,
				pluralAttribute.getAccessType(),
				pluralAttribute.getAccessorStrategy(),
				pluralAttribute.getContext()
		);
	}

	@Override
	public JavaTypeDescriptor getJavaType() {
		return javaType;
	}

	@Override
	public PluralAttributeElementNature getElementNature() {
		return PluralAttributeElementNature.AGGREGATE;
	}

	@Override
	public AttributeTypeResolver getTypeResolver() {
		return typeResolver;
	}

	public EmbeddableTypeMetadata getEmbeddableTypeMetadata() {
		return embeddableTypeMetadata;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddedContainer impl

	@Override
	public MemberDescriptor getBackingMember() {
		return pluralAttribute.getBackingMember();
	}

	@Override
	public AttributeConversionInfo locateConversionInfo(AttributePath attributePath) {
		return pluralAttribute.getContainer().locateConversionInfo( attributePath );
	}

	@Override
	public AttributeOverride locateAttributeOverride(AttributePath attributePath) {
		return pluralAttribute.getContainer().locateAttributeOverride( attributePath );
	}

	@Override
	public AssociationOverride locateAssociationOverride(AttributePath attributePath) {
		return pluralAttribute.getContainer().locateAssociationOverride( attributePath );
	}

	@Override
	public NaturalIdMutability getContainerNaturalIdMutability() {
		return null;
	}

	@Override
	public boolean getContainerOptionality() {
		return false;
	}

	@Override
	public boolean getContainerUpdatability() {
		return true;
	}

	@Override
	public boolean getContainerInsertability() {
		return true;
	}

	@Override
	public void registerConverter(AttributePath attributePath, AttributeConversionInfo conversionInfo) {
		pluralAttribute.getContainer().registerConverter( attributePath, conversionInfo );
	}

	@Override
	public void registerAttributeOverride(AttributePath attributePath, AttributeOverride override) {
		pluralAttribute.getContainer().registerAttributeOverride( attributePath, override );
	}

	@Override
	public void registerAssociationOverride(AttributePath attributePath, AssociationOverride override) {
		pluralAttribute.getContainer().registerAssociationOverride( attributePath, override );
	}
}
