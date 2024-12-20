/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.models.spi.ConversionRegistration;
import org.hibernate.boot.models.spi.ConverterRegistration;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.spi.ClassDetailsRegistry;

/**
 * @author Steve Ebersole
 */
public class DomainModelSource {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final GlobalRegistrations globalRegistrations;
	private final RootMappingDefaults effectiveMappingDefaults;
	private final PersistenceUnitMetadata persistenceUnitMetadata;
	private final List<String> allKnownClassNames;

	public DomainModelSource(
			ClassDetailsRegistry classDetailsRegistry,
			List<String> allKnownClassNames,
			GlobalRegistrations globalRegistrations,
			RootMappingDefaults effectiveMappingDefaults,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.allKnownClassNames = allKnownClassNames;
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

	public List<String> getManagedClassNames() {
		return allKnownClassNames;
	}
}
