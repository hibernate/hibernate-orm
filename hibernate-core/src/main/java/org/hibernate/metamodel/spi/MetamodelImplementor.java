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
import org.hibernate.Metamodel;
import org.hibernate.internal.util.collections.streams.StreamUtils;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @deprecated Was designed as an SPI extension to the JPA {@link javax.persistence.metamodel.Metamodel}
 * however, most of that functionality has been moved to {@link TypeConfiguration} instead.
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
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityDescriptorMap()}
	 * instead
	 */
	@Deprecated
	default Map<String,EntityDescriptor<?>> getEntityPersisterMap() {
		return getTypeConfiguration().getEntityDescriptorMap();
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityDescriptorMap()}
	 * instead
	 */
	@Deprecated
	default Map<String,EntityDescriptor<?>> entityPersisters() {
		return getTypeConfiguration().getEntityDescriptorMap();
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityDescriptorMap()}
	 * instead to access the keys
	 */
	@Deprecated
	default String[] getAllEntityNames() {
		return getTypeConfiguration().getEntityDescriptorMap().keySet().stream().collect( StreamUtils.toStringArray() );
	}


	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityNameResolvers()}
	 */
	@Deprecated
	default <T> EntityDescriptor<? extends T> locateEntityPersister(Class<T> byClass) {
		return getTypeConfiguration().resolveEntityDescriptor( byClass );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getEntityNameResolvers()}
	 */
	@Deprecated
	default <T> EntityDescriptor<T> locateEntityPersister(String byName) {
		return getTypeConfiguration().resolveEntityDescriptor( byName );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#findEntityDescriptor(Class)}
	 */
	@Deprecated
	default <T> EntityDescriptor<? extends T> entityPersister(Class<T> entityClass) {
		return getTypeConfiguration().findEntityDescriptor( entityClass );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#findEntityDescriptor(String)}
	 */
	@Deprecated
	default <T> EntityDescriptor<? extends T> entityPersister(String entityName) {
		return getTypeConfiguration().findEntityDescriptor( entityName );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getCollectionDescriptorMap}
	 */
	@Deprecated
	default Map<String,PersistentCollectionDescriptor<?,?,?>> collectionPersisters() {
		return getTypeConfiguration().getCollectionDescriptorMap();
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getCollectionRolesByEntityParticipant}
	 */
	@Deprecated
	default Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return getTypeConfiguration().getCollectionRolesByEntityParticipant( entityName );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#findCollectionDescriptor}
	 */
	@Deprecated
	default PersistentCollectionDescriptor<?,?,?> collectionPersister(String role) {
		return getTypeConfiguration().findCollectionDescriptor( role );
	}

	/**
	 * @deprecated (6.0) Use {@link #getTypeConfiguration()} -> {@link TypeConfiguration#getCollectionDescriptorMap()}
	 * and collect the Map keys whcih are the entity names
	 */
	@Deprecated
	default String[] getAllCollectionRoles() {
		return getTypeConfiguration().getCollectionDescriptorMap().keySet().stream().collect( StreamUtils.toStringArray() );
	}

}
