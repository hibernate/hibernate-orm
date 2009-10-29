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
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.annotations.common.AssertionFailure;

/**
 * Defines a context for storing information during the building of the {@link MetamodelImpl}.
 * <p/>
 * This contextual information includes data needing to be processed in a second pass as well as
 * cross-references into the built metamodel classes.
 * <p/>
 * At the end of the day, clients are interested in the {@link #getEntityTypeMap} and {@link #getEmbeddableTypeMap}
 * results, which represent all the registered {@link #registerEntityType entities} and
 *  {@link #registerEmbeddedableType embeddabled} respectively.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
class MetadataContext {
	private final SessionFactoryImplementor sessionFactory;
	private final AttributeFactory attributeFactory = new AttributeFactory( this );

	private Map<Class<?>,EntityTypeImpl<?>> entityTypes
			= new HashMap<Class<?>, EntityTypeImpl<?>>();
	private Map<String,EntityTypeImpl<?>> entityTypesByEntityName
			= new HashMap<String, EntityTypeImpl<?>>();
	private Map<PersistentClass,EntityTypeImpl<?>> entityTypesByPersistentClass
			= new HashMap<PersistentClass,EntityTypeImpl<?>>();
	private Map<Class<?>, EmbeddableTypeImpl<?>> embeddables
			= new HashMap<Class<?>, EmbeddableTypeImpl<?>>();
	private Map<MappedSuperclass, MappedSuperclassTypeImpl<?>> mappedSuperclassByMappedSuperclassMapping
			= new HashMap<MappedSuperclass,MappedSuperclassTypeImpl<?>>();
	//this list contains MappedSuperclass and EntityTypes ordered by superclass first
	private List<Object> orderedMappings = new ArrayList<Object>();

	public MetadataContext(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/*package*/ SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * Retrieves the {@link Class java type} to {@link EntityType} map.
	 *
	 * @return The {@link Class java type} to {@link EntityType} map.
	 */
	public Map<Class<?>, EntityTypeImpl<?>> getEntityTypeMap() {
		return Collections.unmodifiableMap( entityTypes );
	}

	public Map<Class<?>, EmbeddableTypeImpl<?>> getEmbeddableTypeMap() {
		return Collections.unmodifiableMap( embeddables );
	}

	/*package*/ void registerEntityType(PersistentClass persistentClass, EntityTypeImpl<?> entityType) {
		entityTypes.put( entityType.getBindableJavaType(), entityType );
		entityTypesByEntityName.put( persistentClass.getEntityName(), entityType );
		entityTypesByPersistentClass.put( persistentClass, entityType );
		orderedMappings.add( persistentClass );
	}

	/*package*/ void registerEmbeddedableType(EmbeddableTypeImpl<?> embeddableType) {
		embeddables.put( embeddableType.getJavaType(), embeddableType );
	}

	/*package*/ void registerMappedSuperclassType(MappedSuperclass mappedSuperclass,
												  MappedSuperclassTypeImpl<?> mappedSuperclassType) {
		mappedSuperclassByMappedSuperclassMapping.put( mappedSuperclass, mappedSuperclassType );
		orderedMappings.add( mappedSuperclass );
	}

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

	@SuppressWarnings({ "unchecked" })
	public void wrapUp() {
		//we need to process types from superclasses to subclasses
		for (Object mapping : orderedMappings) {
			if ( PersistentClass.class.isAssignableFrom( mapping.getClass() ) ) {
				@SuppressWarnings( "unchecked" )
				final PersistentClass safeMapping = (PersistentClass) mapping;
				final EntityTypeImpl<?> jpa2Mapping = entityTypesByPersistentClass.get( safeMapping );
				applyIdMetadata( safeMapping, jpa2Mapping );
				applyVersionAttribute( safeMapping, jpa2Mapping );
				Iterator<Property> properties = ( Iterator<Property> ) safeMapping.getDeclaredPropertyIterator();
				while ( properties.hasNext() ) {
					final Property property = properties.next();
					final Attribute attribute = attributeFactory.buildAttribute( jpa2Mapping, property, true );
					jpa2Mapping.getBuilder().addAttribute( attribute );
				}
				jpa2Mapping.lock();
				populateStaticMetamodel( jpa2Mapping );
			}
			else if ( MappedSuperclass.class.isAssignableFrom( mapping.getClass() ) ) {
				@SuppressWarnings( "unchecked" )
				final MappedSuperclass safeMapping = (MappedSuperclass) mapping;
				final MappedSuperclassTypeImpl<?> jpa2Mapping = mappedSuperclassByMappedSuperclassMapping.get(
						safeMapping
				);
				applyIdMetadata( safeMapping, jpa2Mapping );
				applyVersionAttribute( safeMapping, jpa2Mapping );
				Iterator<Property> properties = ( Iterator<Property> ) safeMapping.getDeclaredPropertyIterator();
				while ( properties.hasNext() ) {
					final Property property = properties.next();
					final Attribute attribute = attributeFactory.buildAttribute( jpa2Mapping, property, false );
					jpa2Mapping.getBuilder().addAttribute( attribute );
				}
				jpa2Mapping.lock();
				populateStaticMetamodel( jpa2Mapping );
			}
			else {
				throw new AssertionFailure( "Unexpected mapping type: " + mapping.getClass() );
			}
		}
	}

	private <X> void applyIdMetadata(PersistentClass persistentClass, EntityTypeImpl<X> jpaEntityType) {
		if ( persistentClass.hasIdentifierProperty() ) {
			final Property declaredIdentifierProperty = persistentClass.getDeclaredIdentifierProperty();
			if (declaredIdentifierProperty != null) {
				jpaEntityType.getBuilder().applyIdAttribute(
						attributeFactory.buildIdAttribute( jpaEntityType, declaredIdentifierProperty, true )
				);
			}
		}
		else {
			jpaEntityType.getBuilder().applyIdClassAttributes( buildIdClassAttributes( jpaEntityType, persistentClass ) );
		}
	}

	private <X> void applyIdMetadata(MappedSuperclass mappingType, MappedSuperclassTypeImpl<X> jpaMappingType) {
		if ( mappingType.hasIdentifierProperty() ) {
			final Property declaredIdentifierProperty = mappingType.getDeclaredIdentifierProperty();
			if (declaredIdentifierProperty != null) {
				jpaMappingType.getBuilder().applyIdAttribute(
						attributeFactory.buildIdAttribute( jpaMappingType, declaredIdentifierProperty, false )
				);
			}
		}
		//an MappedSuperclass can have no identifier if the id is set below in the hierarchy
		else if ( mappingType.getIdentifierMapper() != null ){
			final Set<SingularAttribute<? super X, ?>> attributes = buildIdClassAttributes(
					jpaMappingType, mappingType
			);
			jpaMappingType.getBuilder().applyIdClassAttributes( attributes );
		}
	}

	private <X> void applyVersionAttribute(PersistentClass persistentClass, EntityTypeImpl<X> jpaEntityType) {
		final Property declaredVersion = persistentClass.getDeclaredVersion();
		if (declaredVersion != null) {
			jpaEntityType.getBuilder().applyVersionAttribute(
					attributeFactory.buildVersionAttribute( jpaEntityType, declaredVersion, true )
			);
		}
	}

	private <X> void applyVersionAttribute(MappedSuperclass mappingType, MappedSuperclassTypeImpl<X> jpaMappingType) {
		final Property declaredVersion = mappingType.getDeclaredVersion();
		if ( declaredVersion != null ) {
			jpaMappingType.getBuilder().applyVersionAttribute(
					attributeFactory.buildVersionAttribute( jpaMappingType, declaredVersion, false )
			);
		}
	}

	private <X> Set<SingularAttribute<? super X, ?>> buildIdClassAttributes(
			EntityTypeImpl<X> jpaEntityType,
			PersistentClass persistentClass) {
		Set<SingularAttribute<? super X, ?>> attributes = new HashSet<SingularAttribute<? super X, ?>>();
		@SuppressWarnings( "unchecked")
		Iterator<Property> properties = persistentClass.getIdentifierMapper().getPropertyIterator();
		while ( properties.hasNext() ) {
			attributes.add( attributeFactory.buildIdAttribute( jpaEntityType, properties.next(), true ) );
		}
		return attributes;
	}

	private <X> Set<SingularAttribute<? super X, ?>> buildIdClassAttributes(
			MappedSuperclassTypeImpl<X> jpaMappingType,
			MappedSuperclass mappingType) {
		Set<SingularAttribute<? super X, ?>> attributes = new HashSet<SingularAttribute<? super X, ?>>();
		@SuppressWarnings( "unchecked" )
		Iterator<Property> properties = mappingType.getIdentifierMapper().getPropertyIterator();
		while ( properties.hasNext() ) {
			attributes.add( attributeFactory.buildIdAttribute( jpaMappingType, properties.next(), false ) );
		}
		return attributes;
	}

	private <X> void populateStaticMetamodel(AbstractManagedType<X> managedType) {
		final Class<X> managedTypeClass = managedType.getJavaType();
		final String metamodelClassName = managedTypeClass.getName() + "_";
		try {
			final Class metamodelClass = Class.forName( metamodelClassName, true, managedTypeClass.getClassLoader() );
			// we found the class; so populate it...
			registerAttributes( metamodelClass, managedType );
		}
		catch ( ClassNotFoundException ignore ) {
			// nothing to do...
		}

		// todo : this does not account for @MappeSuperclass, mainly because this is not being tracked in our
		// internal metamodel as populated from the annotatios properly
		AbstractManagedType<? super X> superType = managedType.getSupertype();
		if ( superType != null ) {
			populateStaticMetamodel( superType );
		}
	}

	private final Set<Class> processedMetamodelClasses = new HashSet<Class>();

	private <X> void registerAttributes(Class metamodelClass, AbstractManagedType<X> managedType) {
		if ( processedMetamodelClasses.add( metamodelClass ) ) {
			return;
		}

		// push the attributes on to the metamodel class...
	}

	public MappedSuperclassTypeImpl<?> locateMappedSuperclassType(MappedSuperclass mappedSuperclass) {
		return mappedSuperclassByMappedSuperclassMapping.get(mappedSuperclass);
	}
}
