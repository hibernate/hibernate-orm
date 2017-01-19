/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.MappingException;
import org.hibernate.Metamodel;
import org.hibernate.internal.util.collections.streams.StreamUtils;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @deprecated Was designed as an SPI extension to the JPA {@link javax.persistence.metamodel.Metamodel}
 * however, most of that functionality has been moved to TypeConfiguration instead.
 */
@Deprecated
public interface MetamodelImplementor extends Metamodel {
	void close();

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityNameResolvers()}
	 */
	@Deprecated
	default Collection<EntityNameResolver> getEntityNameResolvers() {
		return getTypeConfiguration().getEntityNameResolvers();
	}


	/**
	 * Get all entity persisters as a Map, keyed by the entity-name.
	 *
	 * @return The (unmodifiable) map of all entity persisters
	 *
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityPersisterMap()}
	 * instead
	 */
	@Deprecated
	default Map<String,EntityPersister<?>> getEntityPersisterMap() {
		return getTypeConfiguration().getEntityPersisterMap();
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityPersisterMap()}
	 * instead
	 */
	@Deprecated
	default Map<String,EntityPersister<?>> entityPersisters() {
		return getTypeConfiguration().getEntityPersisterMap();
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityPersisterMap()}
	 * instead to access the keys
	 */
	@Deprecated
	default String[] getAllEntityNames() {
		return getTypeConfiguration().getEntityPersisterMap().keySet().stream().collect( StreamUtils.toStringArray() );
	}


	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityNameResolvers()}
	 */
	@Deprecated
	default <T> EntityPersister<? extends T> locateEntityPersister(Class<T> byClass) {
		return getTypeConfiguration().resolveEntityPersister( byClass );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityNameResolvers()}
	 */
	@Deprecated
	default <T> EntityPersister<T> locateEntityPersister(String byName) {
		return getTypeConfiguration().resolveEntityPersister( byName );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#findEntityPersister(Class)}
	 */
	@Deprecated
	default <T> EntityPersister<? extends T> entityPersister(Class<T> entityClass) {
		return getTypeConfiguration().findEntityPersister( entityClass );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#findEntityPersister(String)}
	 */
	@Deprecated
	default <T> EntityPersister<? extends T> entityPersister(String entityName) {
		return getTypeConfiguration().findEntityPersister( entityName );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getCollectionPersisterMap}
	 */
	@Deprecated
	default Map<String,CollectionPersister<?,?,?>> collectionPersisters() {
		return getTypeConfiguration().getCollectionPersisterMap();
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getCollectionRolesByEntityParticipant}
	 */
	@Deprecated
	default Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return getTypeConfiguration().getCollectionRolesByEntityParticipant( entityName );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#findCollectionPersister}
	 */
	@Deprecated
	default CollectionPersister<?,?,?> collectionPersister(String role) {
		return getTypeConfiguration().findCollectionPersister( role );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getCollectionPersisterMap()}
	 * and collect the Map keys whcih are the entity names
	 */
	@Deprecated
	default String[] getAllCollectionRoles() {
		return getTypeConfiguration().getCollectionPersisterMap().keySet().stream().collect( StreamUtils.toStringArray() );
	}

}
