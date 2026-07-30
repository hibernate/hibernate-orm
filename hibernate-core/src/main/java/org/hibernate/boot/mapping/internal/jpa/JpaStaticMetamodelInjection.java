/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.jpa;

import java.util.ArrayList;
import java.util.Set;

import jakarta.persistence.metamodel.Attribute;

import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/// Injects JPA static metamodel fields from binding-view source facts.
///
/// The runtime JPA domain model remains the source of the injected values.  This
/// class only replaces the traversal and field-name selection when the resolved
/// bootstrap pipeline supplies the injection plan accumulated during semantic
/// binding and mapping materialization.
///
/// Population may be invoked more than once as runtime managed types become
/// available. The caller's processed-class set makes this idempotent while
/// allowing mapped superclasses used only by embeddables to be populated after
/// the embeddable runtime model is built.
///
/// @since 9.0
/// @author Steve Ebersole
public class JpaStaticMetamodelInjection {
	private final JpaStaticMetamodelInjectionSource source;

	public JpaStaticMetamodelInjection(JpaStaticMetamodelInjectionSource source) {
		this.source = source;
	}

	public boolean populateAvailableTypes(
			MetadataContext context,
			Set<String> processedMetamodelClasses) {
		if ( !context.isStaticMetamodelPopulationEnabled() ) {
			return false;
		}

		boolean populated = false;
		for ( var managedTypeSource : source.managedTypes() ) {
			final ManagedDomainType<?> managedType =
					context.locateManagedType( managedTypeSource.javaType(), managedTypeSource.kind() );
			populated |= populate(
					context,
					processedMetamodelClasses,
					managedTypeSource,
					managedType
			);
		}
		return populated;
	}

	public boolean populateEmbeddableType(
			MetadataContext context,
			Set<String> processedMetamodelClasses,
			ManagedDomainType<?> embeddableType) {
		for ( var managedTypeSource : source.managedTypes() ) {
			if ( managedTypeSource.kind() == ManagedTypeBinding.Kind.EMBEDDABLE
					&& managedTypeSource.javaType().equals( embeddableType.getJavaType() ) ) {
				return populate(
						context,
						processedMetamodelClasses,
						managedTypeSource,
						embeddableType
				);
			}
		}
		return false;
	}

	private boolean populate(
			MetadataContext context,
			Set<String> processedMetamodelClasses,
			JpaStaticMetamodelInjectionSource.ManagedTypeReference managedTypeSource,
			ManagedDomainType<?> managedType) {
		if ( managedType == null || managedType.getRepresentationMode() == RepresentationMode.MAP ) {
			return false;
		}

		final Class<?> metamodelClass = context.metamodelClass( managedType );
		if ( metamodelClass == null ) {
			return false;
		}

		final var resolvedAttributes = new ArrayList<ResolvedField>( managedTypeSource.fields().size() );
		for ( var fieldReference : managedTypeSource.fields() ) {
			final Attribute<?, ?> attribute =
					resolveAttribute( context, managedTypeSource, managedType, fieldReference );
			if ( attribute == null ) {
				return false;
			}
			resolvedAttributes.add( new ResolvedField( fieldReference, attribute ) );
		}

		if ( processedMetamodelClasses.add( metamodelClass.getName() ) ) {
			context.injectStaticMetamodelManagedType( managedType, metamodelClass );
			for ( var resolvedField : resolvedAttributes ) {
				context.injectStaticMetamodelAttribute(
						metamodelClass,
						resolvedField.attribute(),
						resolvedField.reference().role()
								!= JpaStaticMetamodelInjectionSource.FieldRole.CONCRETE_GENERIC_ATTRIBUTE
				);
			}
			return true;
		}
		return false;
	}

	private Attribute<?, ?> resolveAttribute(
			MetadataContext context,
			JpaStaticMetamodelInjectionSource.ManagedTypeReference managedTypeSource,
			ManagedDomainType<?> managedType,
			JpaStaticMetamodelInjectionSource.FieldReference fieldReference) {
		return switch ( fieldReference.role() ) {
			case DECLARED_ATTRIBUTE -> {
				final var declaredAttribute = managedType.findDeclaredAttribute( fieldReference.fieldName() );
				yield declaredAttribute == null
						? context.locateEmbeddableMappedSuperclassAttribute(
								managedTypeSource.javaType(),
								fieldReference.fieldName()
						)
						: declaredAttribute;
			}
			case EMBEDDABLE_DECLARED_ATTRIBUTE -> managedType.findDeclaredAttribute( fieldReference.fieldName() );
			case CONCRETE_GENERIC_ATTRIBUTE ->
					managedType.findDeclaredConcreteGenericAttribute( fieldReference.fieldName() );
			case IDENTIFIER_ATTRIBUTE -> findIdentifierAttribute( managedType, fieldReference.fieldName() );
			case VERSION_ATTRIBUTE -> findVersionAttribute( managedType, fieldReference.fieldName() );
		};
	}

	private record ResolvedField(
			JpaStaticMetamodelInjectionSource.FieldReference reference,
			Attribute<?, ?> attribute) {
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
			return idClassAttribute[0] == null ? managedType.findDeclaredAttribute( fieldName ) : idClassAttribute[0];
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
