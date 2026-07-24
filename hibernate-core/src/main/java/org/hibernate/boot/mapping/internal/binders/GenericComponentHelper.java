/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.MemberDetails;

/**
 * @author Steve Ebersole
 */
public final class GenericComponentHelper {
	private GenericComponentHelper() {
	}

	public static void handleGenericComponentProperty(
			Property property,
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		if ( property.getValue() instanceof Component component ) {
			final var collector = context.getMetadataCollector();
			if ( component.isGeneric() && component.getPropertySpan() > 0
					&& collector.getGenericComponent( component.getComponentClass() ) == null ) {
				collector.registerGenericComponent( doGenericComponentCopy( component, memberDetails, context, true ) );
			}
		}
	}

	public static Component genericComponentCopy(
			Component component,
			MemberDetails memberDetails,
			MetadataBuildingContext context) {
		return doGenericComponentCopy( component, memberDetails, context, true );
	}

	public static Component genericComponentCopy(
			Component component,
			MemberDetails memberDetails,
			MetadataBuildingContext context,
			boolean includeUndeclaredProperties) {
		return doGenericComponentCopy( component, memberDetails, context, includeUndeclaredProperties );
	}

	private static Component doGenericComponentCopy(
			Component component,
			MemberDetails memberDetails,
			MetadataBuildingContext context,
		boolean includeUndeclaredProperties) {
		final java.util.List<Property> declarationProperties = includeUndeclaredProperties
				? component.getProperties()
						.stream()
						.map( (property) -> genericPropertyCopy( property, context ) )
						.toList()
				: java.util.List.of();
		final Component copy = component.copyForDeclaration( declarationProperties );
		copy.setComponentClassDetails( memberDetails.getType().determineRawClass() );
		copy.setGeneric( false );
		return copy;
	}

	private static Property genericPropertyCopy(Property property, MetadataBuildingContext context) {
		final var value = property.getValue().copy();
		if ( value instanceof BasicValue basicValue ) {
			final var details = BasicValueResolutionDetails.create(
					basicValue,
					property.isGenericSpecialization() || property.getMemberDetails() == null
							? BasicValueSource.genericDeclaration()
							: BasicValueSource.embeddableMember( property.getMemberDetails() )
			);
			BasicValueResolutionBuilder.applyResolution(
					details,
					context.getServiceComponents(),
					MappingResolutionState.from( context ),
					context
			);
		}
		final Property copy = property.copyForDeclaration( value );
		if ( property.isGenericSpecialization() ) {
			copy.setGeneric( true );
			copy.setGenericSpecialization( false );
			if ( property.getMemberDetails() != null ) {
				copy.setReturnedClassName( property.getMemberDetails().getType().getName() );
			}
		}
		return copy;
	}
}
