/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.metamodel;

import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;
import java.io.Serializable;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.mapping.PersistentClass;

/**
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements Metamodel, Serializable {

	private final Map<Class<?>,EntityType<?>> entities;
	private Map<Class<?>, EmbeddableType<?>> embeddables;

	public MetamodelImpl(Iterator<PersistentClass> classes) {
		Map<Class<?>,EntityType<?>> localEntities = new HashMap<Class<?>,EntityType<?>>();
		MetadataContext context = new MetadataContext();
		while ( classes.hasNext() ) {
			buildEntityType( classes, localEntities, context );
		}
		this.entities = Collections.unmodifiableMap( localEntities );
		this.embeddables = Collections.unmodifiableMap( context.getEmbeddableTypes() );
		context.postProcess( this );
	}

	private <X> void buildEntityType(Iterator<PersistentClass> classes, Map<Class<?>, EntityType<?>> localEntities, MetadataContext context) {
		PersistentClass persistentClass = classes.next();
		@SuppressWarnings( "unchecked" )
		final Class<X> clazz = persistentClass.getMappedClass();
		if ( clazz != null ) {
			localEntities.put( clazz, new EntityTypeImpl<X>(clazz, persistentClass, context) );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = entities.get( cls );
		if ( entityType == null ) throw new IllegalArgumentException( "Not an entity: " + cls );
		//unsafe casting is our map inserts guarantee them
		return (EntityType<X>) entityType;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public <X> ManagedType<X> managedType(Class<X> cls) {
		ManagedType<?> type = entities.get( cls );
		if ( type == null ) throw new IllegalArgumentException( "Not an managed type: " + cls );
		//unsafe casting is our map inserts guarantee them
		return (ManagedType<X>) type;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddableType<?> embeddableType = embeddables.get( cls );
		if ( embeddableType == null ) throw new IllegalArgumentException( "Not an entity: " + cls );
		//unsafe casting is our map inserts guarantee them
		return (EmbeddableType<X>) embeddableType;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<ManagedType<?>> getManagedTypes() {
		final Set<ManagedType<?>> managedTypes = new HashSet<ManagedType<?>>( entities.size() + embeddables.size() );
		managedTypes.addAll( entities.values() );
		managedTypes.addAll( embeddables.values() );
		return managedTypes;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<EntityType<?>> getEntities() {
		return new HashSet<EntityType<?>>(entities.values());
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<EmbeddableType<?>>(embeddables.values());
	}
}
