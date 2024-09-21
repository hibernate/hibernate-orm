/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.boot.jaxb.SourceType.OTHER;

/**
 * @author Steve Ebersole
 */
public class AdditionalManagedResourcesImpl implements ManagedResources {
	private final Collection<Class<?>> knownClasses;
	private final Collection<ClassDetails> classDetails;
	private final Collection<String> packageNames;
	private final Collection<Binding<JaxbBindableMappingDescriptor>> xmlMappings;

	public AdditionalManagedResourcesImpl(
			Collection<Class<?>> knownClasses,
			Collection<ClassDetails> classDetails,
			Collection<String> packageNames,
			Collection<Binding<JaxbBindableMappingDescriptor>> xmlMappings) {
		this.knownClasses = knownClasses;
		this.classDetails = classDetails;
		this.packageNames = packageNames;
		this.xmlMappings = xmlMappings;
	}

	@Override
	public Collection<ConverterDescriptor> getAttributeConverterDescriptors() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Class<?>> getAnnotatedClassReferences() {
		return knownClasses == null ? Collections.emptyList() : knownClasses;
	}

	@Override
	public Collection<String> getAnnotatedClassNames() {
		if ( CollectionHelper.isNotEmpty( classDetails ) ) {
			return classDetails.stream().map( ClassDetails::getName ).toList();
		}
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getAnnotatedPackageNames() {
		return packageNames == null ? Collections.emptyList() : packageNames;
	}

	@Override
	public Collection<Binding<JaxbBindableMappingDescriptor>> getXmlMappingBindings() {
		if ( xmlMappings == null ) {
			return Collections.emptyList();
		}

		return xmlMappings;
	}

	@Override
	public Map<String, Class<?>> getExtraQueryImports() {
		return Collections.emptyMap();
	}

	public static class Builder {
		private final MappingBinder mappingBinder;

		private List<Class<?>> classes;
		private List<ClassDetails> classDetails;
		private List<String> packageNames;
		private Collection<Binding<JaxbBindableMappingDescriptor>> xmlMappings;

		public Builder(ServiceRegistry serviceRegistry) {
			this.mappingBinder = new MappingBinder( serviceRegistry );
		}

		public Builder() {
			this( new StandardServiceRegistryBuilder().build() );
		}

		public Builder addLoadedClasses(List<Class<?>> additionalClasses) {
			if ( additionalClasses != null ) {
				if ( this.classes == null ) {
					this.classes = new ArrayList<>();
				}
				this.classes.addAll( additionalClasses );
			}
			return this;
		}

		public Builder addLoadedClasses(Class<?>... additionalClasses) {
			if ( this.classes == null ) {
				this.classes = new ArrayList<>();
			}
			Collections.addAll( this.classes, additionalClasses );
			return this;
		}

		public Builder addClassDetails(List<ClassDetails> additionalClassDetails) {
			if ( additionalClassDetails != null ) {
				if ( this.classDetails == null ) {
					this.classDetails = new ArrayList<>();
				}
				this.classDetails.addAll( additionalClassDetails );
			}
			return this;
		}

		public Builder addPackages(String... packageNames) {
			if ( this.packageNames == null ) {
				this.packageNames = new ArrayList<>();
			}
			Collections.addAll( this.packageNames, packageNames );
			return this;
		}

		public ManagedResources build() {
			return new AdditionalManagedResourcesImpl( classes, classDetails, packageNames, xmlMappings );
		}

		public Builder addXmlMappings(String resourceName) {
			return addXmlMappings( resourceName, new Origin( SourceType.RESOURCE, resourceName ) );
		}

		public Builder addXmlMappings(String resourceName, Origin origin) {
			return addXmlBinding( mappingBinder.bind(
					Builder.class.getClassLoader().getResourceAsStream( resourceName ),
					origin
			) );
		}

		public Builder addXmlBinding(Binding<JaxbBindableMappingDescriptor> binding) {
			if ( xmlMappings == null ) {
				xmlMappings = new ArrayList<>();
			}
			xmlMappings.add( binding );
			return this;
		}

		public void addJaxbEntityMappings(List<JaxbEntityMappingsImpl> additionalJaxbMappings) {
			if ( additionalJaxbMappings == null ) {
				return;
			}
			for ( JaxbEntityMappingsImpl additionalJaxbMapping : additionalJaxbMappings ) {
				addXmlBinding( new Binding<>( additionalJaxbMapping, new Origin( OTHER, "additional" ) ) );
			}
		}
	}
}
