/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import jakarta.persistence.NamedEntityGraph;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Models a {@linkplain NamedEntityGraph @NamedEntityGraph}
 *
 * @author Steve Ebersole
 */
public class NamedEntityGraphDefinition {
	private final NamedEntityGraph annotation;
	private final String jpaEntityName;
	private final String entityName;
	private final String name;

	public NamedEntityGraphDefinition(NamedEntityGraph annotation, String jpaEntityName, String entityName) {
		this.annotation = annotation;
		this.jpaEntityName = jpaEntityName;
		this.entityName = entityName;
		this.name = isNotEmpty( annotation.name() ) ? annotation.name() : jpaEntityName;
		if ( name == null ) {
			throw new IllegalArgumentException( "Named entity graph name cannot be null" );
		}
	}

	public String getRegisteredName() {
		return name;
	}

	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public String getEntityName() {
		return entityName;
	}

	public NamedEntityGraph getAnnotation() {
		return annotation;
	}
}
