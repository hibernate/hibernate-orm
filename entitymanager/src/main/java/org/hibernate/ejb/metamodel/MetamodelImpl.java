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

	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = entities.get( cls );
		if ( entityType == null ) throw new IllegalArgumentException( "Not an entity: " + cls );
		//unsafe casting is our map inserts guarantee them
		return (EntityType<X>) entityType;
	}

	public <X> ManagedType<X> type(Class<X> cls) {
		ManagedType<?> type = null;
		type = entities.get( cls );
		if ( type == null ) throw new IllegalArgumentException( "Not an managed type: " + cls );
		//unsafe casting is our map inserts guarantee them
		return (ManagedType<X>) type;
	}

	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddableType<?> embeddableType = embeddables.get( cls );
		if ( embeddableType == null ) throw new IllegalArgumentException( "Not an entity: " + cls );
		//unsafe casting is our map inserts guarantee them
		return (EmbeddableType<X>) embeddableType;
	}

	public Set<ManagedType<?>> getManagedTypes() {
		final Set<ManagedType<?>> managedTypes = new HashSet<ManagedType<?>>( entities.size() + embeddables.size() );
		managedTypes.addAll( entities.values() );
		managedTypes.addAll( embeddables.values() );
		return managedTypes;
	}

	public Set<EntityType<?>> getEntities() {
		return new HashSet<EntityType<?>>(entities.values());
	}

	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<EmbeddableType<?>>(embeddables.values());
	}
}
