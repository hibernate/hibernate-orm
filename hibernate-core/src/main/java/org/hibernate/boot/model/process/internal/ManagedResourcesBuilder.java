/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.spi.MetadataBuildingOptions;

/**
 * @author Steve Ebersole
 */
public class ManagedResourcesBuilder {
	/**
	 * Singleton access
	 */
	public static final ManagedResourcesBuilder INSTANCE = new ManagedResourcesBuilder();

	private ManagedResourcesBuilder() {
	}

	public ManagedResources buildCompleteManagedResources(
			final MetadataSources sources,
			final MetadataBuildingOptions options) {

		// Instantiate ManagedResourcesImpl, and build its baseline from MetadataSources
		final ManagedResourcesImpl managedResources = ManagedResourcesImpl.baseline( sources, options );

		ScanningCoordinator.INSTANCE.coordinateScan( managedResources, options, sources.getXmlMappingBinderAccess() );

		return managedResources;
	}


}
