/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.io.IOException;
import org.hibernate.HibernateException;
import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.model.process.internal.ManagedResourcesBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import java.util.List;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.service.ServiceRegistry;


/// XML adapter for the common resource builder.
///
/// @author Steve Ebersole
public final class AdditionalManagedResourcesImpl {
	public static class Builder {
		private final ManagedResourcesBuilder resources = new ManagedResourcesBuilder();
		private final ServiceRegistry serviceRegistry;

		public Builder(ServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
		}

		public Builder() {
			this( null );
		}

		public Builder addLoadedClasses(List<Class<?>> classes) {
			if ( classes != null ) {
				classes.forEach( resources::addClass );
			}
			return this;
		}

		public Builder addLoadedClasses(Class<?>... classes) {
			return addLoadedClasses( List.of( classes ) );
		}

		public Builder addClassDetails(List<ClassDetails> details) {
			if ( details != null ) {
				details.forEach( resources::addClassDetails );
			}
			return this;
		}

		public Builder addPackages(String... packages) {
			List.of( packages ).forEach( resources::addPackageDescriptor );
			return this;
		}

		public ManagedResources build() {
			return resources.build();
		}

		public Builder addXmlMappings(String resourceName) {
			return addXmlMappings( resourceName, new Origin( SourceType.RESOURCE, resourceName ) );
		}

		public Builder addXmlMappings(String resourceName, Origin origin) {
			final var registry = serviceRegistry == null
					? new StandardServiceRegistryBuilder().build()
					: serviceRegistry;
			try (var stream = registry.requireService( ClassLoaderService.class ).locateResourceStream( resourceName )) {
				if ( stream == null ) {
					throw new MappingNotFoundException( origin );
				}
				return addXmlBinding( new MappingBinder( registry ).bind( stream, origin ) );
			}
			catch (IOException e) {
				throw new HibernateException( "Could not read mapping " + resourceName, e );
			}
			finally {
				if ( serviceRegistry == null ) {
					StandardServiceRegistryBuilder.destroy( registry );
				}
			}
		}

		public Builder addXmlBinding(Binding<? extends JaxbBindableMappingDescriptor> binding) {
			resources.addXmlBinding( binding );
			return this;
		}

		public void addJaxbEntityMappings(List<JaxbEntityMappingsImpl> mappings) {
			if ( mappings != null ) {
				mappings.forEach( mapping -> addXmlBinding( new Binding<>( mapping, new Origin( SourceType.OTHER, "additional" ) ) ) );
			}
		}
	}
}
