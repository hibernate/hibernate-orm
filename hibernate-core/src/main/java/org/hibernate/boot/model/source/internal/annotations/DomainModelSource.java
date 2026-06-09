/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.stream.Stream;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.models.spi.ConversionRegistration;
import org.hibernate.boot.models.spi.ConverterRegistration;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadata;
import org.hibernate.models.spi.ClassDetailsRegistry;

/**
 * @author Steve Ebersole
 */
public class DomainModelSource {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final GlobalRegistrations globalRegistrations;
	private final RootMappingDefaults effectiveMappingDefaults;
	private final PersistenceUnitMetadata persistenceUnitMetadata;
	private final List<ClassDetails> managedJavaTypes;
	private final List<ClassDetails> dynamicManagedTypes;
	private final List<ClassDetails> packageDescriptors;
	private final List<ModuleDescriptor> moduleDescriptors;

	/// Resolved module metadata, kept separate from ordinary types.
	///
	/// @author Steve Ebersole
	public record ModuleDescriptor(String name, AnnotationTarget target) {
	}

	public DomainModelSource(
			ClassDetailsRegistry classDetailsRegistry,
			List<ClassDetails> managedJavaTypes,
			List<ClassDetails> dynamicManagedTypes,
			List<ClassDetails> packageDescriptors,
			List<ModuleDescriptor> moduleDescriptors,
			GlobalRegistrations globalRegistrations,
			RootMappingDefaults effectiveMappingDefaults,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.managedJavaTypes = List.copyOf( managedJavaTypes );
		this.dynamicManagedTypes = List.copyOf( dynamicManagedTypes );
		this.packageDescriptors = List.copyOf( packageDescriptors );
		this.moduleDescriptors = List.copyOf( moduleDescriptors );
		this.globalRegistrations = globalRegistrations;
		this.effectiveMappingDefaults = effectiveMappingDefaults;
		this.persistenceUnitMetadata = persistenceUnitMetadata;
	}

	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	public RootMappingDefaults getEffectiveMappingDefaults() {
		return effectiveMappingDefaults;
	}

	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	public List<ConversionRegistration> getConversionRegistrations() {
		return globalRegistrations.getConverterRegistrations();
	}

	public Set<ConverterRegistration> getConverterRegistrations() {
		return globalRegistrations.getJpaConverters();
	}

	public List<ClassDetails> getManagedJavaTypes() {
		return managedJavaTypes;
	}

	public List<ClassDetails> getDynamicManagedTypes() {
		return dynamicManagedTypes;
	}

	public List<ClassDetails> getPackageDescriptors() {
		return packageDescriptors;
	}

	public List<ModuleDescriptor> getModuleDescriptors() {
		return moduleDescriptors;
	}

	public List<ClassDetails> getManagedTypes() {
		return Stream.concat( managedJavaTypes.stream(), dynamicManagedTypes.stream() ).toList();
	}
}
