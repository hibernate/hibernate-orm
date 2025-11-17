/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult;

/**
 * @author Steve Ebersole
 */
public class XmlProcessingResultImpl implements XmlProcessingResult {
	private final List<OverrideTuple<JaxbEntityImpl>> entityOverrides = new ArrayList<>();
	private final List<OverrideTuple<JaxbMappedSuperclassImpl>> mappedSuperclassesOverrides = new ArrayList<>();
	private final List<OverrideTuple<JaxbEmbeddableImpl>> embeddableOverrides = new ArrayList<>();

	public void addEntityOverride(OverrideTuple<JaxbEntityImpl> overrideTuple) {
		entityOverrides.add( overrideTuple );
	}

	public void addMappedSuperclassesOverride(OverrideTuple<JaxbMappedSuperclassImpl> overrideTuple) {
		mappedSuperclassesOverrides.add( overrideTuple );
	}

	public void addEmbeddableOverride(OverrideTuple<JaxbEmbeddableImpl> overrideTuple) {
		embeddableOverrides.add( overrideTuple );
	}

	@Override
	public void apply() {
		ManagedTypeProcessor.processOverrideEmbeddable( getEmbeddableOverrides() );

		ManagedTypeProcessor.processOverrideMappedSuperclass( getMappedSuperclassesOverrides() );

		ManagedTypeProcessor.processOverrideEntity( getEntityOverrides() );
	}

	@Override
	public List<OverrideTuple<JaxbEntityImpl>> getEntityOverrides() {
		return entityOverrides;
	}

	@Override
	public List<OverrideTuple<JaxbMappedSuperclassImpl>> getMappedSuperclassesOverrides() {
		return mappedSuperclassesOverrides;
	}

	@Override
	public List<OverrideTuple<JaxbEmbeddableImpl>> getEmbeddableOverrides() {
		return embeddableOverrides;
	}
}
