/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
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
	public void apply(PersistenceUnitMetadata metadata) {
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
