/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.orm.test.boot.models.CategorizedDomainModel;
import org.hibernate.orm.test.boot.models.ManagedResourcesProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.testing.orm.junit.ServiceRegistryScope;

/**
 * @author Steve Ebersole
 */
public class CategorizationTestsHelper {
	public static CategorizedDomainModel buildCategorizedDomainModel(ServiceRegistryScope scope, Class<?>... managedClasses) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataSources metadataSources = new MetadataSources( serviceRegistry ).addAnnotatedClasses( managedClasses );
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		final ManagedResources managedResources = MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );

		//noinspection UnnecessaryLocalVariable
		final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
				managedResources,
				bootstrapContext
		);
		return categorizedDomainModel;
	}
}
