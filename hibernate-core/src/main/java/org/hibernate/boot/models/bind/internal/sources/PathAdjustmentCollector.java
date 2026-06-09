/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.sources;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.models.bind.spi.BindingContext;
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
