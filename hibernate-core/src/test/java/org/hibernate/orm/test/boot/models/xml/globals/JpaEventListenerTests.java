/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.globals;

import java.util.List;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.orm.test.boot.models.process.ManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.internal.jdk.VoidClassDetails;
import org.hibernate.models.spi.MethodDetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class JpaEventListenerTests {
	@Test
	void testGlobalRegistration() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/globals.xml" )
				.build();

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources(
					managedResources,
					bootstrapContext
			);
			final List<JpaEventListener> registrations = categorizedDomainModel
					.getGlobalRegistrations()
					.getEntityListenerRegistrations();
			assertThat( registrations ).hasSize( 1 );
			final JpaEventListener registration = registrations.get( 0 );
			final MethodDetails postPersistMethod = registration.getPostPersistMethod();
			assertThat( postPersistMethod ).isNotNull();
			assertThat( postPersistMethod.getReturnType() ).isEqualTo( VoidClassDetails.VOID_CLASS_DETAILS );
			assertThat( postPersistMethod.getArgumentTypes() ).hasSize( 1 );
		}
	}

}
