/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.jpa;

import java.util.Set;

import jakarta.persistence.metamodel.Attribute;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/// Injects JPA static metamodel fields from binding-view source facts.
///
/// The runtime JPA domain model remains the source of the injected values.  This
/// class only replaces the traversal and field-name selection for identifiable
/// managed types when the resolved bootstrap pipeline has retained a
/// `BootBindingModel`.
///
/// @since 9.0
/// @author Steve Ebersole
public class JpaStaticMetamodelInjection {
	private final JpaStaticMetamodelInjectionSource source;

	public JpaStaticMetamodelInjection(JpaStaticMetamodelInjectionSource source) {
		this.source = source;
	}

	public boolean populate(MetadataContext context, Set<String> processedMetamodelClasses) {
		if ( !context.isStaticMetamodelPopulationEnabled() ) {
			return false;
		}

		boolean populated = false;
		for ( var managedTypeSource : source.managedTypes() ) {
			final ManagedDomainType<?> managedType =
					context.locateManagedType( managedTypeSource.javaType(), managedTypeSource.kind() );
			if ( managedType == null || managedType.getRepresentationMode() == RepresentationMode.MAP ) {
				continue;
			}

			final Class<?> metamodelClass = context.metamodelClass( managedType );
			if ( metamodelClass == null ) {
				continue;
			}

			if ( processedMetamodelClasses.add( metamodelClass.getName() ) ) {
				context.injectStaticMetamodelManagedType( managedType, metamodelClass );
				for ( var fieldReference : managedTypeSource.fields() ) {
					final Attribute<?, ?> attribute = resolveAttribute( managedType, fieldReference );
					if ( attribute != null ) {
						context.injectStaticMetamodelAttribute( metamodelClass, attribute );
					}
				}
				populated = true;
			}
		}
		return populated;
	}

	private Attribute<?, ?> resolveAttribute(
			ManagedDomainType<?> managedType,
			JpaStaticMetamodelInjectionSource.FieldReference fieldReference) {
		return switch ( fieldReference.role() ) {
			case DECLARED_ATTRIBUTE -> managedType.findDeclaredAttribute( fieldReference.fieldName() );
			case IDENTIFIER_ATTRIBUTE -> findIdentifierAttribute( managedType, fieldReference.fieldName() );
			case VERSION_ATTRIBUTE -> findVersionAttribute( managedType, fieldReference.fieldName() );
		};
	}

	private Attribute<?, ?> findIdentifierAttribute(ManagedDomainType<?> managedType, String fieldName) {
		if ( managedType instanceof IdentifiableDomainType<?> identifiableType ) {
			final var identifierAttribute = identifiableType.findIdAttribute();
			if ( identifierAttribute != null && identifierAttribute.getName().equals( fieldName ) ) {
				return identifierAttribute;
			}
			final Attribute<?, ?>[] idClassAttribute = new Attribute[1];
			identifiableType.visitIdClassAttributes( (attribute) -> {
				if ( attribute.getName().equals( fieldName ) ) {
					idClassAttribute[0] = attribute;
				}
			} );
			return idClassAttribute[0];
		}
		return null;
	}

	private Attribute<?, ?> findVersionAttribute(ManagedDomainType<?> managedType, String fieldName) {
		if ( managedType instanceof IdentifiableDomainType<?> identifiableType ) {
			final var versionAttribute = identifiableType.findVersionAttribute();
			if ( versionAttribute != null && versionAttribute.getName().equals( fieldName ) ) {
				return versionAttribute;
			}
		}
		return null;
	}
}
