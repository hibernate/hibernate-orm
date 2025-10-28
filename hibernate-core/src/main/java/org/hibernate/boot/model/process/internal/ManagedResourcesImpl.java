/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.MappingSettings;

import jakarta.persistence.AttributeConverter;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;


/**
 * @author Steve Ebersole
 */
public class ManagedResourcesImpl implements ManagedResources {
	private final Map<Class<? extends AttributeConverter<?,?>>, ConverterDescriptor<?,?>> attributeConverterDescriptorMap = new HashMap<>();
	private final Set<Class<?>> annotatedClassReferences = new LinkedHashSet<>();
	private final Set<String> annotatedClassNames = new LinkedHashSet<>();
	private final Set<String> annotatedPackageNames = new LinkedHashSet<>();
	private final List<Binding<? extends JaxbBindableMappingDescriptor>> mappingFileBindings = new ArrayList<>();
	private Map<String, Class<?>> extraQueryImports;

	public static ManagedResourcesImpl baseline(MetadataSources sources, BootstrapContext bootstrapContext) {
		final var managedResources = new ManagedResourcesImpl();
		bootstrapContext.getAttributeConverters().forEach( managedResources::addAttributeConverterDefinition );
		managedResources.annotatedClassReferences.addAll( sources.getAnnotatedClasses() );
		managedResources.annotatedClassNames.addAll( sources.getAnnotatedClassNames() );
		managedResources.annotatedPackageNames.addAll( sources.getAnnotatedPackages() );
		handleXmlMappings( sources, managedResources, bootstrapContext );
		managedResources.extraQueryImports = sources.getExtraQueryImports();
		return managedResources;
	}

	private static void handleXmlMappings(
			MetadataSources sources,
			ManagedResourcesImpl impl,
			BootstrapContext bootstrapContext) {
		if ( !bootstrapContext.getMetadataBuildingOptions().isXmlMappingEnabled() ) {
			BOOT_LOGGER.ignoringXmlMappings(
					sources.getMappingXmlBindings().size(),
					MappingSettings.XML_MAPPING_ENABLED
			);
		}
		else {
			impl.mappingFileBindings.addAll( sources.getXmlBindings() );
		}
	}

	public ManagedResourcesImpl() {
	}

	@Override
	public Collection<ConverterDescriptor<?,?>> getAttributeConverterDescriptors() {
		return unmodifiableCollection( attributeConverterDescriptorMap.values() );
	}

	@Override
	public Collection<Class<?>> getAnnotatedClassReferences() {
		return unmodifiableSet( annotatedClassReferences );
	}

	@Override
	public Collection<String> getAnnotatedClassNames() {
		return unmodifiableSet( annotatedClassNames );
	}

	@Override
	public Collection<String> getAnnotatedPackageNames() {
		return unmodifiableSet( annotatedPackageNames );
	}

	@Override
	public Collection<Binding<? extends JaxbBindableMappingDescriptor>> getXmlMappingBindings() {
		return unmodifiableList( mappingFileBindings );
	}

	@Override
	public Map<String, Class<?>> getExtraQueryImports() {
		return extraQueryImports;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// @Internal

	@Internal
	public void addAttributeConverterDefinition(ConverterDescriptor<?,?> descriptor) {
		attributeConverterDescriptorMap.put( descriptor.getAttributeConverterClass(), descriptor );
	}

	@Internal
	public void addAnnotatedClassReference(Class<?> annotatedClassReference) {
		annotatedClassReferences.add( annotatedClassReference );
	}

	@Internal
	public void addAnnotatedClassName(String annotatedClassName) {
		annotatedClassNames.add( annotatedClassName );
	}

	@Internal
	public void addAnnotatedPackageName(String annotatedPackageName) {
		annotatedPackageNames.add( annotatedPackageName );
	}

	@Internal
	public void addXmlBinding(Binding<JaxbBindableMappingDescriptor> binding) {
		mappingFileBindings.add( binding );
	}
}
