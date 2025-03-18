/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.envers.boot.model.PersistentEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntityMappingData {

	private PersistentEntity entityDefinition;
	private final List<PersistentEntity> additionalEntityDefinitions;

	private final JaxbHbmHibernateMapping mapping;
	private final List<JaxbHbmHibernateMapping> additionalMappings;

	public EntityMappingData() {
		this.mapping = new JaxbHbmHibernateMapping();
		this.mapping.setAutoImport( false );

		this.additionalEntityDefinitions = new ArrayList<>();
		this.additionalMappings = new ArrayList<>();
	}

	public PersistentEntity getEntityDefinition() {
		return entityDefinition;
	}

	public JaxbHbmHibernateMapping getMapping() {
		return mapping;
	}

	public List<JaxbHbmHibernateMapping> getAdditionalMappings() {
		return additionalMappings;
	}

	public void addMapping(PersistentEntity mapping) {
		this.entityDefinition = mapping;
	}

	public void addAdditionalMapping(PersistentEntity mapping) {
		this.additionalEntityDefinitions.add( mapping );
	}

	public void build() {
		entityDefinition.build( this.mapping );
		for ( PersistentEntity additionalDefinition : additionalEntityDefinitions ) {
			final JaxbHbmHibernateMapping newMapping = new JaxbHbmHibernateMapping();
			additionalMappings.add( newMapping );
			additionalDefinition.build( newMapping );
		}
	}

	public boolean isRootEntity() {
		return !mapping.getClazz().isEmpty();
	}

	public boolean isSubclassEntity() {
		return !mapping.getSubclass().isEmpty();
	}

	public boolean isUnionSubclassEntity() {
		return !mapping.getUnionSubclass().isEmpty();
	}

	public boolean isJoinedSubclassEntity() {
		return !mapping.getJoinedSubclass().isEmpty();
	}

	public boolean isEntityTypeKnown() {
		return !isRootEntity() && !isSubclassEntity() && !isUnionSubclassEntity() && !isJoinedSubclassEntity();
	}
}
