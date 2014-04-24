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
package org.hibernate.metamodel.source.internal.annotations.entity;

import java.util.Collection;
import javax.persistence.AccessType;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.AttributeConversionInfo;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.EmbeddedContainer;
import org.hibernate.metamodel.source.internal.annotations.util.ConverterAndOverridesHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.PARENT;

/**
 * This is called *Embeddable* type metadata to closely follow the JPA
 * terminology; just be aware that this more closely models the *Embedded*.
 * Generally this is used as a delegate from "composite contexts" such as
 * Embedded attributes and plural attributes with Embedded elements/keys.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class EmbeddableTypeMetadata extends ManagedTypeMetadata {
	private final EmbeddedContainer container;
	private final NaturalIdMutability naturalIdMutability;
	private final String parentReferencingAttributeName;
	private final String customTuplizerClassName;

	public EmbeddableTypeMetadata(
			JavaTypeDescriptor embeddableType,
			EmbeddedContainer container,
			AttributeRole attributeRoleBase,
			AttributePath attributePathBase,
			AccessType defaultAccessType,
			String defaultAccessorStrategy,
			AnnotationBindingContext context) {
		super( embeddableType, attributeRoleBase, attributePathBase, defaultAccessType, defaultAccessorStrategy, context );

		this.container = container;
		this.naturalIdMutability = container.getContainerNaturalIdMutability();
		this.parentReferencingAttributeName = decodeParentAnnotation( embeddableType );
		this.customTuplizerClassName = decodeTuplizerAnnotation( container );

		// the idea here is to collect up class-level annotations and to apply
		// the maps from supers
		collectConversionInfo();
		collectAttributeOverrides();
		collectAssociationOverrides();

		collectAttributesIfNeeded();
	}

	private String decodeParentAnnotation(JavaTypeDescriptor embeddableType) {
		final Collection<AnnotationInstance> parentAnnotations = embeddableType.findAnnotations( PARENT );
		if ( parentAnnotations == null || parentAnnotations.isEmpty() ) {
			return null;
		}

		if ( parentAnnotations.size() > 1 ) {
			throw getLocalBindingContext().makeMappingException(
					"Embeddable class contained multiple @Parent annotations; only one is allowed"
			);
		}

		final AnnotationInstance parentAnnotation = parentAnnotations.iterator().next();
		return JandexHelper.getPropertyName( parentAnnotation.target() );
	}

	private String decodeTuplizerAnnotation(EmbeddedContainer container) {
		// prefer tuplizer defined at the embedded level
		{
			// might be null though in the case of an IdClass
			if ( container.getBackingMember() != null ) {
				final AnnotationInstance tuplizerAnnotation = container.getBackingMember().getAnnotations().get(
						HibernateDotNames.TUPLIZER
				);
				if ( tuplizerAnnotation != null ) {
					return tuplizerAnnotation.value( "impl" ).asString();
				}
			}
		}

		// The tuplizer on the embeddable (if one) would be covered by this.getCustomTuplizerClassName()...
		return super.getCustomTuplizerClassName();
	}

	private void collectConversionInfo() {
	}

	private void collectAttributeOverrides() {
		ConverterAndOverridesHelper.INSTANCE.processAttributeOverrides(
				new AttributePath(),
				this,
				this,
				getLocalBindingContext()
		);
	}

	private void collectAssociationOverrides() {
	}

	public String getParentReferencingAttributeName() {
		return parentReferencingAttributeName;
	}

	public NaturalIdMutability getNaturalIdMutability() {
		 return naturalIdMutability;
	}

	@Override
	public String getCustomTuplizerClassName() {
		return customTuplizerClassName;
	}

	@Override
	public AttributeConversionInfo locateConversionInfo(AttributePath attributePath) {
		return container.locateConversionInfo( attributePath );
	}

	@Override
	public AttributeOverride locateAttributeOverride(AttributePath attributePath) {
		return container.locateAttributeOverride( attributePath );
	}

	@Override
	public AssociationOverride locateAssociationOverride(AttributePath attributePath) {
		return container.locateAssociationOverride( attributePath );
	}

	@Override
	public void registerConverter(AttributePath attributePath, AttributeConversionInfo conversionInfo) {
		container.registerConverter( attributePath, conversionInfo );
	}

	@Override
	public void registerAttributeOverride(AttributePath attributePath, AttributeOverride override) {
		container.registerAttributeOverride( attributePath, override );
	}

	@Override
	public void registerAssociationOverride(AttributePath attributePath, AssociationOverride override) {
		container.registerAssociationOverride( attributePath, override );
	}

	@Override
	public boolean canAttributesBeInsertable() {
		return container.getContainerInsertability();
	}

	@Override
	@SuppressWarnings("SimplifiableIfStatement")
	public boolean canAttributesBeUpdatable() {
		if ( naturalIdMutability == NaturalIdMutability.IMMUTABLE ) {
			return false;
		}
		return container.getContainerUpdatability();
	}

	@Override
	public NaturalIdMutability getContainerNaturalIdMutability() {
		return naturalIdMutability;
	}
}


