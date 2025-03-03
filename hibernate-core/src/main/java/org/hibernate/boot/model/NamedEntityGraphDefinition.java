/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import jakarta.persistence.NamedEntityGraph;
import org.hibernate.mapping.PersistentClass;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Models a {@linkplain NamedEntityGraph @NamedEntityGraph}
 *
 * @author Steve Ebersole
 */
public class NamedEntityGraphDefinition {
	public enum Source { JPA, PARSED }

	private final String name;

	private final String entityName;

	private final Source source;
	private final NamedGraphCreator graphCreator;

	public NamedEntityGraphDefinition(jakarta.persistence.NamedEntityGraph annotation, String jpaEntityName, String entityName) {
		this.name = isNotEmpty( annotation.name() ) ? annotation.name() : jpaEntityName;
		if ( name == null ) {
			throw new IllegalArgumentException( "Named entity graph name cannot be null" );
		}

		this.entityName = entityName;

		source = Source.JPA;
		graphCreator = new NamedGraphCreatorJpa( annotation, jpaEntityName );
	}

	public NamedEntityGraphDefinition(org.hibernate.annotations.NamedEntityGraph annotation, PersistentClass persistentClass) {
		this.name = isNotEmpty( annotation.name() ) ? annotation.name() : persistentClass.getJpaEntityName();
		if ( name == null ) {
			throw new IllegalArgumentException( "Named entity graph name cannot be null" );
		}

		this.entityName = persistentClass.getEntityName();

		source = Source.PARSED;
		graphCreator = new NamedGraphCreatorParsed( persistentClass.getMappedClass(), annotation );
	}

	public NamedEntityGraphDefinition(org.hibernate.annotations.NamedEntityGraph annotation) {
		this.name = annotation.name();
		if ( name == null ) {
			throw new IllegalArgumentException( "Named entity graph name cannot be null" );
		}

		this.entityName = null;

		source = Source.PARSED;
		graphCreator = new NamedGraphCreatorParsed( annotation );
	}

	public String getRegisteredName() {
		return name;
	}

	public String getEntityName() {
		return entityName;
	}

	public Source getSource() {
		return source;
	}

	public NamedGraphCreator getGraphCreator() {
		return graphCreator;
	}
}
