/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
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
		final Component copy = component.copy();
		copy.setComponentClassName( memberDetails.getType().determineRawClass().getName() );
		copy.setGeneric( false );
		if ( !includeUndeclaredProperties ) {
			copy.getProperties().clear();
		}
		else {
			copy.getProperties().replaceAll( (property) -> genericPropertyCopy( property, context ) );
		}
		return copy;
	}

	private static Property genericPropertyCopy(Property property, MetadataBuildingContext context) {
		final Property copy = property.copy();
		final var value = property.getValue().copy();
		if ( value instanceof BasicValue basicValue ) {
			BasicValueResolutionBuilder.applyResolution(
					BasicValueResolutionBuilder.Input.create(
							basicValue,
							property.isGenericSpecialization() || property.getMemberDetails() == null
									? BasicValueSource.genericDeclaration()
									: BasicValueSource.embeddableMember( property.getMemberDetails() )
					)
			);
		}
		copy.setValue( value );
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
