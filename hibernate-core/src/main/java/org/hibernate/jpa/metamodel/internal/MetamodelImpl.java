/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.metamodel.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Hibernate implementation of the JPA {@link javax.persistence.metamodel.Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements Metamodel, Serializable {
	private final Map<Class<?>, EntityTypeImpl<?>> entityTypeMap;
	private final Map<Class<?>, MappedSuperclassTypeImpl<?>> mappedSuperclassTypeMap;
	private final Map<Class<?>, EmbeddableTypeImpl<?>> embeddableTypeMap;
	private final Map<String, EntityTypeImpl<?>> entityTypesByEntityName;

	/**
	 * Instantiate the metamodel.
	 *
	 * @param entityTypeMap The entity mappings.
	 * @param mappedSuperclassTypeMap The {@link javax.persistence.MappedSuperclass} mappings
	 * @param embeddableTypeMap The embeddable (component) mappings.
	 */
	public MetamodelImpl(
			Map<Class<?>, EntityTypeImpl<?>> entityTypeMap,
			Map<Class<?>, MappedSuperclassTypeImpl<?>> mappedSuperclassTypeMap,
			Map<Class<?>, EmbeddableTypeImpl<?>> embeddableTypeMap,
			Map<String, EntityTypeImpl<?>> entityTypesByEntityName) {
		this.entityTypeMap = entityTypeMap;
		this.mappedSuperclassTypeMap = mappedSuperclassTypeMap;
		this.embeddableTypeMap = embeddableTypeMap;
		this.entityTypesByEntityName = entityTypesByEntityName;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = entityTypeMap.get( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		return (EntityType<X>) entityType;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> ManagedType<X> managedType(Class<X> cls) {
		ManagedType<?> type = entityTypeMap.get( cls );
		if ( type == null ) {
			type = mappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = embeddableTypeMap.get( cls );
		}
		if ( type == null ) {
			throw new IllegalArgumentException( "Not an managed type: " + cls );
		}
		return (ManagedType<X>) type;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddableType<?> embeddableType = embeddableTypeMap.get( cls );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls );
		}
		return (EmbeddableType<X>) embeddableType;
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		final int setSize = CollectionHelper.determineProperSizing(
				entityTypeMap.size() + mappedSuperclassTypeMap.size() + embeddableTypeMap.size()
		);
		final Set<ManagedType<?>> managedTypes = new HashSet<ManagedType<?>>( setSize );
		managedTypes.addAll( entityTypeMap.values() );
		managedTypes.addAll( mappedSuperclassTypeMap.values() );
		managedTypes.addAll( embeddableTypeMap.values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<EntityType<?>>( entityTypesByEntityName.values() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<EmbeddableType<?>>( embeddableTypeMap.values() );
	}

	public EntityTypeImpl getEntityTypeByName(String entityName) {
		return entityTypesByEntityName.get( entityName );
	}
}
