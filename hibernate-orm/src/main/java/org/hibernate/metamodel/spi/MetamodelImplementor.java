/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityGraph;

import org.hibernate.EntityNameResolver;
import org.hibernate.MappingException;
import org.hibernate.Metamodel;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public interface MetamodelImplementor extends Metamodel {
	@Override
	SessionFactoryImplementor getSessionFactory();

	Collection<EntityNameResolver> getEntityNameResolvers();

	/**
	 * Locate an EntityPersister by the entity class.  The passed Class might refer to either
	 * the entity name directly, or it might name a proxy interface for the entity.  This
	 * method accounts for both, preferring the direct named entity name.
	 *
	 * @param byClass The concrete Class or proxy interface for the entity to locate the persister for.
	 *
	 * @return The located EntityPersister, never {@code null}
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 */
	EntityPersister locateEntityPersister(Class byClass);

	/**
	 * Locate the entity persister by name.
	 *
	 * @param byName The entity name
	 *
	 * @return The located EntityPersister, never {@code null}
	 *
	 * @throws org.hibernate.UnknownEntityTypeException If a matching EntityPersister cannot be located
	 */
	EntityPersister locateEntityPersister(String byName);

	/**
	 * Locate the persister for an entity by the entity class.
	 *
	 * @param entityClass The entity class
	 *
	 * @return The entity persister
	 *
	 * @throws MappingException Indicates persister for that class could not be found.
	 */
	EntityPersister entityPersister(Class entityClass);

	/**
	 * Locate the persister for an entity by the entity-name
	 *
	 * @param entityName The name of the entity for which to retrieve the persister.
	 *
	 * @return The persister
	 *
	 * @throws MappingException Indicates persister could not be found with that name.
	 */
	EntityPersister entityPersister(String entityName);

	/**
	 * Get all entity persisters as a Map, which entity name its the key and the persister is the value.
	 *
	 * @return The Map contains all entity persisters.
	 */
	Map<String,EntityPersister> entityPersisters();

	/**
	 * Get the persister object for a collection role.
	 *
	 * @param role The role of the collection for which to retrieve the persister.
	 *
	 * @return The persister
	 *
	 * @throws MappingException Indicates persister could not be found with that role.
	 */
	CollectionPersister collectionPersister(String role);

	/**
	 * Get all collection persisters as a Map, which collection role as the key and the persister is the value.
	 *
	 * @return The Map contains all collection persisters.
	 */
	Map<String,CollectionPersister> collectionPersisters();

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
	 * Get the names of all entities known to this Metamodel
	 *
	 * @return All of the entity names
	 */
	String[] getAllEntityNames();

	/**
	 * Get the names of all collections known to this Metamodel
	 *
	 * @return All of the entity names
	 */
	String[] getAllCollectionRoles();

	<T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph);

	<T> EntityGraph<T> findEntityGraphByName(String name);

	<T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass);

	void close();
}
