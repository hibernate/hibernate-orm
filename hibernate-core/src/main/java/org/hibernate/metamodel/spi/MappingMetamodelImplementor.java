/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import java.util.Collection;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;

/**
 * @author Steve Ebersole
 */
public interface MappingMetamodelImplementor extends MappingMetamodel, QueryParameterBindingTypeResolver {

	/**
	 * Retrieves a set of all the collection roles in which the given entity is a participant, as either an
	 * index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 *
	 * @return set of all the collection roles in which the given entityName participates.
	 */
	Set<String> getCollectionRolesByEntityParticipant(String entityName);

	/**
	 * Access to the EntityNameResolver instance that Hibernate is configured to
	 * use for determining the entity descriptor from an instance of an entity
	 */
	Collection<EntityNameResolver> getEntityNameResolvers();

	/**
	 * Get the names of all entities known to this Metamodel
	 *
	 * @return All the entity names
	 */
	default String[] getAllEntityNames() {
		return streamEntityDescriptors()
				.map( EntityPersister::getEntityName )
				.toArray( String[]::new );
	}

	/**
	 * Get the names of all collections known to this Metamodel
	 *
	 * @return All the entity names
	 */
	default String[] getAllCollectionRoles() {
		return streamCollectionDescriptors()
				.map( CollectionPersister::getRole )
				.toArray( String[]::new );
	}
}
