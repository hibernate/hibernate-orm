/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.model.domain.JpaMetamodel;

/**
 * Entry point providing access to the runtime metamodels:
 * <ul>
 * <li>the {@linkplain JpaMetamodel domain model}, our implementation of the
 *     JPA-defined {@linkplain jakarta.persistence.metamodel.Metamodel model}
 *     of the Java types, and
 * <li>our {@linkplain MappingMetamodel relational mapping model} of how these
 *     types are made persistent.
 * </ul>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface RuntimeMetamodels {
	/**
	 * Access to the JPA / domain metamodel.
	 */
	JpaMetamodel getJpaMetamodel();

	/**
	 * Access to the relational mapping model.
	 */
	MappingMetamodel getMappingMetamodel();


	// some convenience methods...

	default EntityMappingType getEntityMappingType(String entityName) {
		return getMappingMetamodel().getEntityDescriptor( entityName );
	}

	default EntityMappingType getEntityMappingType(Class<?> entityType) {
		return getMappingMetamodel().getEntityDescriptor( entityType );
	}

	default PluralAttributeMapping getPluralAttributeMapping(String role) {
		return getMappingMetamodel().findCollectionDescriptor( role ).getAttributeMapping();
	}

	default String getImportedName(String name) {
		return getMappingMetamodel().getImportedName( name );
	}
}
