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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * Defines a context for storing information during the building of the {@link MetamodelImpl}.
 * <p/>
 * This contextual information includes data needing to be processed in a second pass as well as
 * cross-references into the built metamodel classes.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
class MetadataContext {
	private final AttributeFactory attributeFactory = new AttributeFactory( this );

	private HashMap<Class<?>,EntityTypeImpl<?>> entityTypes
			= new HashMap<Class<?>, EntityTypeImpl<?>>();
	private HashMap<String,EntityTypeImpl<?>> entityTypesByEntityName
			= new HashMap<String, EntityTypeImpl<?>>();
	private LinkedHashMap<PersistentClass,EntityTypeImpl<?>> entityTypesByPersistentClass
			= new LinkedHashMap<PersistentClass,EntityTypeImpl<?>>();
	private HashMap<Class<?>, EmbeddableTypeImpl<?>> embeddables
			= new HashMap<Class<?>, EmbeddableTypeImpl<?>>();

	/**
	 * Given a Hibernate {@link PersistentClass}, locate the corresponding JPA {@link org.hibernate.type.EntityType}
	 * implementation.  May retur null if the given {@link PersistentClass} has not yet been processed.
	 *
	 * @param persistentClass The Hibernate (config time) metamodel instance representing an entity.
	 * @return Tne corresponding JPA {@link org.hibernate.type.EntityType}, or null if not yet processed.
	 */
	public EntityTypeImpl<?> locateEntityType(PersistentClass persistentClass) {
		return entityTypesByPersistentClass.get( persistentClass );
	}

	/**
	 * Given a Java {@link Class}, locate the corresponding JPA {@link org.hibernate.type.EntityType}.  May
	 * return null which could means that no such mapping exists at least at this time.
	 *
	 * @param javaType The java class.
	 * @return The corresponding JPA {@link org.hibernate.type.EntityType}, or null.
	 */
	public EntityTypeImpl<?> locateEntityType(Class<?> javaType) {
		return entityTypes.get( javaType );
	}

	/**
	 * Given an entity-name, locate the corresponding JPA {@link org.hibernate.type.EntityType}.  May
	 * return null which could means that no such mapping exists at least at this time.
	 *
	 * @param entityName The entity-name.
	 * @return The corresponding JPA {@link org.hibernate.type.EntityType}, or null.
	 */
	public EntityTypeImpl<?> locateEntityType(String entityName) {
		return entityTypesByEntityName.get( entityName );
	}

	/**
	 * Retrieves the {@link Class java type} to {@link EntityType} map.
	 *
	 * @return The {@link Class java type} to {@link EntityType} map.
	 */
	public Map<Class<?>, EntityTypeImpl<?>> getEntityTypeMap() {
		return Collections.unmodifiableMap( entityTypes );
	}

	/*package*/ void registerEntityType(PersistentClass persistentClass, EntityTypeImpl<?> entityType) {
		entityTypes.put( entityType.getBindableJavaType(), entityType );
		entityTypesByEntityName.put( persistentClass.getEntityName(), entityType );
		entityTypesByPersistentClass.put( persistentClass, entityType );
	}

	/*package*/ void registerEmbeddedableType(EmbeddableTypeImpl<?> embeddableType) {
		embeddables.put( embeddableType.getJavaType(), embeddableType );
	}

	public Map<Class<?>, EmbeddableTypeImpl<?>> getEmbeddableTypeMap() {
		return Collections.unmodifiableMap( embeddables );
	}

	@SuppressWarnings({ "unchecked" })
	public void wrapUp() {
		// IMPL NOTE : entityTypesByPersistentClass is a insertion-ordered map, where the insertion order
		//		ensures that a type's super type is already processed...
		for ( Map.Entry<PersistentClass,EntityTypeImpl<?>> entry : entityTypesByPersistentClass.entrySet() ) {
			applyIdMetadata( entry.getKey(), entry.getValue() );
			applyVersionAttribute( entry.getKey(), entry.getValue() );
			Iterator<Property> properties = ( Iterator<Property> ) entry.getKey().getPropertyIterator();
			while ( properties.hasNext() ) {
				final Property property = properties.next();
				final Attribute attribute = attributeFactory.buildAttribute( entry.getValue(), property );
				entry.getValue().getBuilder().addAttribute( attribute );
			}
			entry.getValue().lock();
			// todo : find the X_ style metamodel classes, if present, and inject
		}
	}

	private <X> void applyIdMetadata(PersistentClass persistentClass, EntityTypeImpl<X> jpaEntityType) {
		if ( persistentClass.hasIdentifierProperty() ) {
			jpaEntityType.getBuilder().applyIdAttribute(
					attributeFactory.buildIdAttribute( jpaEntityType, persistentClass.getIdentifierProperty() ) 
			);
		}
		else {
			jpaEntityType.getBuilder().applyIdClassAttributes( buildIdClassAttributes( jpaEntityType, persistentClass ) );
		}
	}

	private <X> void applyVersionAttribute(PersistentClass persistentClass, EntityTypeImpl<X> jpaEntityType) {
		if ( ! persistentClass.isVersioned() ) {
			return;
		}
		jpaEntityType.getBuilder().applyVersionAttribute(
				attributeFactory.buildVerisonAttribute( jpaEntityType, persistentClass.getVersion() )
		);
	}

	@SuppressWarnings( "unchecked")
	private <X> Set<SingularAttribute<? super X, ?>> buildIdClassAttributes(
			EntityTypeImpl<X> jpaEntityType,
			PersistentClass persistentClass) {
		Set<SingularAttribute<? super X, ?>> attributes = new HashSet<SingularAttribute<? super X, ?>>();
		Iterator<Property> properties = persistentClass.getIdentifierMapper().getPropertyIterator();
		while ( properties.hasNext() ) {
			attributes.add( attributeFactory.buildIdAttribute( jpaEntityType, properties.next() ) );
		}
		return attributes;
	}

}
