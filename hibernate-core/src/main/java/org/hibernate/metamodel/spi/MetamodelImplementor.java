/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>
 */
package org.hibernate.metamodel.spi;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.MappingException;
import org.hibernate.Metamodel;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Extensions to the JPA-defined {@link Metamodel} contract.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link MappingMetamodel} or
 *             {@link org.hibernate.metamodel.model.domain.JpaMetamodel}
 *             instead. See {@link org.hibernate.metamodel.RuntimeMetamodels}.
 */
@Deprecated(since = "6.0")
public interface MetamodelImplementor extends MappingMetamodel, Metamodel {
	/**
	 * @deprecated Use {@link MappingMetamodelImplementor#getEntityNameResolvers} instead
	 */
	@Deprecated(since = "6.0")
	Collection<EntityNameResolver> getEntityNameResolvers();

	/**
	 * Locate the persister for an entity by the entity class.
	 *
	 * @param entityClass The entity class
	 *
	 * @return The entity persister
	 *
	 * @throws MappingException Indicates persister for that class could not be found.
	 *
	 * @deprecated Use {@link MappingMetamodel#getEntityDescriptor} instead
	 */
	@Deprecated(since = "6.0")
	default EntityPersister entityPersister(Class<?> entityClass) {
		return getEntityDescriptor(entityClass);
	}

	/**
	 * Locate the persister for an entity by the entity-name
	 *
	 * @param entityName The name of the entity for which to retrieve the persister.
	 *
	 * @return The persister
	 *
	 * @throws MappingException Indicates persister could not be found with that name.
	 *
	 * @deprecated Use {@link MappingMetamodel#getEntityDescriptor} instead
	 */
	@Deprecated(since = "6.0")
	default EntityPersister entityPersister(String entityName) {
		return getEntityDescriptor(entityName);
	}

	/**
	 * Get all entity persisters as a Map, which entity name its the key and the persister is the value.
	 *
	 * @return The Map contains all entity persisters.
	 *
	 * @deprecated With no direct replacement; see {@link MappingMetamodel#forEachEntityDescriptor}
	 * and {@link MappingMetamodel#streamEntityDescriptors()} instead
	 */
	@Deprecated(since = "6.0")
	Map<String,EntityPersister> entityPersisters();

	/**
	 * Get the persister object for a collection role.
	 *
	 * @param role The role of the collection for which to retrieve the persister.
	 *
	 * @return The persister
	 *
	 * @throws MappingException Indicates persister could not be found with that role.
	 *
	 * @deprecated Use {@link MappingMetamodel#getCollectionDescriptor} instead
	 */
	@Deprecated(since = "6.0")
	default CollectionPersister collectionPersister(String role) {
		return getCollectionDescriptor(role);
	}

	/**
	 * Get all collection persisters as a Map, which collection role as the key and the persister is the value.
	 *
	 * @return The Map contains all collection persisters.
	 *
	 * @deprecated With no direct replacement; see {@link MappingMetamodel#forEachCollectionDescriptor}
	 * and {@link MappingMetamodel#streamCollectionDescriptors()} instead
	 */
	@Deprecated(since = "6.0")
	Map<String,CollectionPersister> collectionPersisters();

	/**
	 * Retrieves a set of all the collection roles in which the given entity is a participant, as either an
	 * index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 *
	 * @return set of all the collection roles in which the given entityName participates.
	 *
	 * @deprecated Use {@link MappingMetamodelImplementor#getCollectionRolesByEntityParticipant}
	 * and {@link MappingMetamodel#streamCollectionDescriptors()} instead
	 */
	@Deprecated(since = "6.0")
	Set<String> getCollectionRolesByEntityParticipant(String entityName);

	/**
	 * Get the names of all entities known to this Metamodel
	 *
	 * @return All the entity names
	 */
	String[] getAllEntityNames();

	/**
	 * Get the names of all collections known to this Metamodel
	 *
	 * @return All the entity names
	 */
	String[] getAllCollectionRoles();

	void close();
}
