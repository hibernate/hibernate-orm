/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.boot.mapping.internal.binders.CustomMappingBinder;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.context.VersionPropertyHandoff;
import org.hibernate.boot.mapping.internal.view.VersionBindingView;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;

/// Materializes the legacy mapping contribution for a `@Version` attribute.
///
/// Version binding records source facts as a [VersionBindingView].  This
/// materializer owns the destination-side mutation of [RootClass], including
/// the dedicated version and declared-version slots expected by later boot and
/// runtime metamodel code.
///
/// @since 9.0
/// @author Steve Ebersole
public class VersionMappingMaterializer {
	public Property materializeVersion(
			VersionBindingView contribution,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Property property = new PropertyMappingMaterializer().createProperty(
				contribution.attributeName(),
				contribution.member()
		);
		typeBinding.setVersion( property );
		typeBinding.setDeclaredVersion( property );
		typeBinding.addProperty( property );

		new BasicValueMappingMaterializer().materializeVersionBasicValue(
				contribution.member(),
				contribution.valueIntent(),
				property,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		CustomMappingBinder.callAttributeBinders(
				contribution.member(),
				typeBinding,
				property,
				bindingState,
				bindingContext
		);
		bindingState.addVersionPropertyHandoff( new VersionPropertyHandoff( contribution, typeBinding, property ) );
		return property;
	}
}
