/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Convert;

/// Path-keyed mapping adjustments scoped to an attribute.
///
/// Collects annotation-driven adjustments whose target is identified by an attribute path
/// relative to the source member, such as [AttributeOverride], [AssociationOverride],
/// and explicit [Convert] declarations.
///
/// @since 9.0
/// @author Steve Ebersole
class PathAdjustmentCollector {
	private final Map<AttributePath, AttributeOverride> attributeOverrides = new HashMap<>();
	private final Map<AttributePath, AssociationOverride> associationOverrides = new HashMap<>();
	private final Map<AttributePath, Convert> conversions = new HashMap<>();

	PathAdjustmentCollector(
			MemberDetails member,
			BindingContext bindingContext) {
		final var modelsContext = bindingContext.getBootstrapContext().getModelsContext();
		collectMemberAdjustments( member, modelsContext );
	}

	PathAdjustmentCollector(
			MemberDetails member,
			ClassDetails ownerType,
			ClassDetails hierarchyRootType,
			BindingContext bindingContext) {
		final var modelsContext = bindingContext.getBootstrapContext().getModelsContext();
		collectTypeAdjustments( member, ownerTypeChain( ownerType, hierarchyRootType ), modelsContext );
		collectMemberAdjustments( member, modelsContext );
	}

	PathAdjustmentCollector(
			PathAdjustmentCollector parent,
			MemberDetails member,
			BindingContext bindingContext) {
		if ( parent != null ) {
			attributeOverrides.putAll( parent.attributeOverrides );
			associationOverrides.putAll( parent.associationOverrides );
			conversions.putAll( parent.conversions );
		}

		final var modelsContext = bindingContext.getBootstrapContext().getModelsContext();
		collectMemberAdjustments( member, modelsContext );
	}

	private static List<ClassDetails> ownerTypeChain(ClassDetails ownerType, ClassDetails hierarchyRootType) {
		if ( ownerType == null ) {
			return List.of();
		}

		final java.util.ArrayList<ClassDetails> chain = new java.util.ArrayList<>();
		ClassDetails current = ownerType;
		while ( current != null && current != ClassDetails.OBJECT_CLASS_DETAILS ) {
			chain.add( 0, current );
			if ( sameClass( current, hierarchyRootType ) ) {
				break;
			}
			current = current.getSuperClass();
		}
		return chain;
	}

	private static boolean sameClass(ClassDetails one, ClassDetails another) {
		if ( one == null || another == null ) {
			return false;
		}

		final String oneClassName = one.getClassName();
		final String anotherClassName = another.getClassName();
		if ( oneClassName != null || anotherClassName != null ) {
			return Objects.equals( oneClassName, anotherClassName );
		}

		return Objects.equals( one.getName(), another.getName() );
	}

	private void collectMemberAdjustments(MemberDetails member, org.hibernate.models.spi.ModelsContext modelsContext) {
		for ( AttributeOverride override : member.getRepeatedAnnotationUsages( AttributeOverride.class, modelsContext ) ) {
			attributeOverrides.put( AttributePath.parse( override.name() ), override );
		}
		for ( AssociationOverride override : member.getRepeatedAnnotationUsages( AssociationOverride.class, modelsContext ) ) {
			associationOverrides.put( AttributePath.parse( override.name() ), override );
		}
		for ( Convert conversion : member.getRepeatedAnnotationUsages( Convert.class, modelsContext ) ) {
			if ( conversion.attributeName() != null && !conversion.attributeName().isEmpty() ) {
				conversions.put( AttributePath.parse( conversion.attributeName() ), conversion );
			}
		}
	}

	private void collectTypeAdjustments(
			MemberDetails member,
			List<ClassDetails> ownerTypes,
			org.hibernate.models.spi.ModelsContext modelsContext) {
		for ( ClassDetails ownerType : ownerTypes ) {
			collectTypeAdjustments( member, ownerType, modelsContext );
		}
	}

	private void collectTypeAdjustments(
			MemberDetails member,
			ClassDetails type,
			org.hibernate.models.spi.ModelsContext modelsContext) {
		if ( type == null ) {
			return;
		}

		final String memberPrefix = member.resolveAttributeName() + ".";
		for ( AttributeOverride override : type.getRepeatedAnnotationUsages( AttributeOverride.class, modelsContext ) ) {
			final String relativePath = relativePath( memberPrefix, override.name() );
			if ( relativePath != null ) {
				attributeOverrides.put( AttributePath.parse( relativePath ), override );
			}
		}
		for ( AssociationOverride override : type.getRepeatedAnnotationUsages( AssociationOverride.class, modelsContext ) ) {
			final String relativePath = relativePath( memberPrefix, override.name() );
			if ( relativePath != null ) {
				associationOverrides.put( AttributePath.parse( relativePath ), override );
			}
		}
		for ( Convert conversion : type.getRepeatedAnnotationUsages( Convert.class, modelsContext ) ) {
			final String relativePath = conversion.attributeName() == null
					? null
					: relativePath( memberPrefix, conversion.attributeName() );
			if ( relativePath != null && !relativePath.isEmpty() ) {
				conversions.put( AttributePath.parse( relativePath ), conversion );
			}
		}
	}

	private static String relativePath(String memberPrefix, String path) {
		return path != null && path.startsWith( memberPrefix )
				? path.substring( memberPrefix.length() )
				: null;
	}

	AttributeOverride locateAttributeOverride(String path) {
		return attributeOverrides.get( AttributePath.parse( path ) );
	}

	AssociationOverride locateAssociationOverride(String path) {
		return associationOverrides.get( AttributePath.parse( path ) );
	}

	Convert locateConversion(String path) {
		return conversions.get( AttributePath.parse( path ) );
	}
}
