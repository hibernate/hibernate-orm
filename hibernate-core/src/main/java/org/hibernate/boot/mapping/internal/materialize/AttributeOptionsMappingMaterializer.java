/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/// Materializes simple attribute option facts onto legacy mapping objects.
///
/// The source facts live in [AttributeBindingView]; this class is only the
/// compatibility writer for the current `org.hibernate.mapping` shape.
///
/// @since 9.0
/// @author Steve Ebersole
public class AttributeOptionsMappingMaterializer {
	public void materializeOptions(
			AttributeBindingView attributeBinding,
			Property property) {
		property.setOptimisticLocked( attributeBinding.optimisticLocked() );

		if ( attributeBinding.immutable() ) {
			property.setUpdatable( false );
		}
	}

	public void materializeBasicValueOptions(
			AttributeBindingView attributeBinding,
			BasicValue basicValue) {
		materializeBasicValueOptions( attributeBinding, basicValue, null );
	}

	public void materializeBasicValueOptions(
			AttributeBindingView attributeBinding,
			BasicValue basicValue,
			BasicValueResolutionBuilder.Input resolutionInput) {
		if ( attributeBinding.immutable() ) {
			if ( resolutionInput != null ) {
				resolutionInput.markAttributeImmutable();
			}
		}
		final Class<? extends MutabilityPlan<?>> mutabilityPlanClass = attributeBinding.explicitMutabilityPlanClass();
		if ( mutabilityPlanClass != null ) {
			if ( resolutionInput != null ) {
				resolutionInput.setAttributeMutabilityPlanClass( mutabilityPlanClass );
			}
		}
	}

}
