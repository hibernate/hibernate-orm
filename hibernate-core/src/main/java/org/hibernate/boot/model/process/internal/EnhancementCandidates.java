/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.LinkedHashSet;
import java.util.List;

/// Collects transformation candidates without resolving the source model.
///
/// @author Steve Ebersole
public final class EnhancementCandidates {
	private EnhancementCandidates() {
	}

	public static List<String> forContainer(PersistenceUnitInfo unit) {
		unit.getManagedClassNames().forEach( name -> ManagedResourceValidation.validateClassName(
				name, "getManagedClassNames() entry '{class}'", "getManagedPackageDescriptors() returning \"{package}\"", "getManagedModuleDescriptors()" ) );
		return forContainer( unit.getAllClassNames() );
	}

	public static List<String> forContainer(List<String> names) {
		final var result = new LinkedHashSet<String>();
		for ( var name : names ) {
			ManagedResourceValidation.validateClassName( name, "getAllClassNames() entry '{class}'",
					"getAllPackageDescriptors() returning \"{package}\"", "getAllModuleDescriptors()" );
			result.add( name );
		}
		return List.copyOf( result );
	}

}
