/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.jpa;

import java.util.Set;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.MetadataContext;
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
				context.injectStaticMetamodelFields( managedType, metamodelClass, managedTypeSource.fieldNames() );
				populated = true;
			}
		}
		return populated;
	}
}
