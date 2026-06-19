/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.ModelsException;
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
			Property property,
			BasicValue basicValue) {
		property.setOptimisticLocked( attributeBinding.optimisticLocked() );

		if ( attributeBinding.immutable() ) {
			property.setUpdatable( false );
		}

		final Class<? extends MutabilityPlan<?>> mutabilityPlanClass = attributeBinding.explicitMutabilityPlanClass();
		if ( mutabilityPlanClass != null ) {
			basicValue.setExplicitMutabilityPlanAccess(
					(typeConfiguration) -> instantiateMutabilityPlan( attributeBinding, mutabilityPlanClass )
			);
		}
	}

	private static MutabilityPlan<?> instantiateMutabilityPlan(
			AttributeBindingView attributeBinding,
			Class<? extends MutabilityPlan<?>> mutabilityPlanClass) {
		try {
			return mutabilityPlanClass.getConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			final ModelsException modelsException = new ModelsException(
					"Error instantiating local @MutabilityPlan - " + attributeBinding.member().getName()
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}
}
