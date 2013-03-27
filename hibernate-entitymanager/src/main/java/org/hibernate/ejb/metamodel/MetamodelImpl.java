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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.*;

/**
 * Hibernate implementation of the JPA {@link Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements Metamodel, Serializable {
	private final Map<Class<?>,EntityTypeImpl<?>> entities;
	private final Map<Class<?>, EmbeddableTypeImpl<?>> embeddables;
	private final Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap;
    private final Map<String, EntityTypeImpl<?>> entityTypesByEntityName;

    /**
   	 * Build the metamodel using the information from the collection of Hibernate
   	 * {@link PersistentClass} models as well as the Hibernate {@link org.hibernate.SessionFactory}.
   	 *
   	 * @param persistentClasses Iterator over the Hibernate (config-time) metamodel
   	 * @param sessionFactory The Hibernate session factory.
   	 * @return The built metamodel
	 * 
	 * @deprecated use {@link #buildMetamodel(java.util.Iterator,org.hibernate.engine.spi.SessionFactoryImplementor,boolean)} instead
   	 */
	@Deprecated
   	public static MetamodelImpl buildMetamodel(
   			Iterator<PersistentClass> persistentClasses,
   			SessionFactoryImplementor sessionFactory) {
        return buildMetamodel(persistentClasses, sessionFactory, false);
   	}

	/**
	 * Build the metamodel using the information from the collection of Hibernate
	 * {@link PersistentClass} models as well as the Hibernate {@link org.hibernate.SessionFactory}.
	 *
	 * @param persistentClasses Iterator over the Hibernate (config-time) metamodel
	 * @param sessionFactory The Hibernate session factory.
     * @param ignoreUnsupported ignore unsupported/unknown annotations (like @Any)
	 * @return The built metamodel
	 */
	public static MetamodelImpl buildMetamodel(
			Iterator<PersistentClass> persistentClasses,
			SessionFactoryImplementor sessionFactory,
            boolean ignoreUnsupported) {
		MetadataContext context = new MetadataContext( sessionFactory, ignoreUnsupported );
		while ( persistentClasses.hasNext() ) {
			PersistentClass pc = persistentClasses.next();
			locateOrBuildEntityType( pc, context );
		}
		context.wrapUp();
		return new MetamodelImpl( context.getEntityTypeMap(), context.getEmbeddableTypeMap(), context.getMappedSuperclassTypeMap(), context.getEntityTypesByEntityName() );
	}

	private static EntityTypeImpl<?> locateOrBuildEntityType(PersistentClass persistentClass, MetadataContext context) {
		EntityTypeImpl<?> entityType = context.locateEntityType( persistentClass );
		if ( entityType == null ) {
			entityType = buildEntityType( persistentClass, context );
		}
		return entityType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings( "unchecked" )
	private static EntityTypeImpl<?> buildEntityType(PersistentClass persistentClass, MetadataContext context) {
		final Class javaType = persistentClass.getMappedClass();
		context.pushEntityWorkedOn(persistentClass);
		final MappedSuperclass superMappedSuperclass = persistentClass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if (superType == null) {
			final PersistentClass superPersistentClass = persistentClass.getSuperclass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		EntityTypeImpl entityType = new EntityTypeImpl(
				javaType,
				superType,
				persistentClass.getJpaEntityName(),
				persistentClass.hasIdentifierProperty(),
				persistentClass.isVersioned()
		);

        entityType.setTypeName(persistentClass.getEntityName());

        context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn(persistentClass);
		return entityType;
	}
	
	private static MappedSuperclassTypeImpl<?> locateOrBuildMappedsuperclassType(
			MappedSuperclass mappedSuperclass, MetadataContext context) {
		MappedSuperclassTypeImpl<?> mappedSuperclassType = context.locateMappedSuperclassType( mappedSuperclass );
		if ( mappedSuperclassType == null ) {
			mappedSuperclassType = buildMappedSuperclassType(mappedSuperclass, context);
		}
		return mappedSuperclassType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings( "unchecked" )
	private static MappedSuperclassTypeImpl<?> buildMappedSuperclassType(MappedSuperclass mappedSuperclass,
																		 MetadataContext context) {
		final MappedSuperclass superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if (superType == null) {
			final PersistentClass superPersistentClass = mappedSuperclass.getSuperPersistentClass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		final Class javaType = mappedSuperclass.getMappedClass();
		MappedSuperclassTypeImpl mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaType,
				superType,
				mappedSuperclass.hasIdentifierProperty(),
				mappedSuperclass.isVersioned()
		);
		context.registerMappedSuperclassType( mappedSuperclass, mappedSuperclassType );
		return mappedSuperclassType;
	}

	/**
	 * Instantiate the metamodel.
	 *
	 * @param entities The entity mappings.
	 * @param embeddables The embeddable (component) mappings.
	 * @param mappedSuperclassTypeMap The {@link javax.persistence.MappedSuperclass} mappings
	 */
	private MetamodelImpl(
			Map<Class<?>, EntityTypeImpl<?>> entities,
			Map<Class<?>, EmbeddableTypeImpl<?>> embeddables,
            Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap,
            Map<String, EntityTypeImpl<?>> entityTypesByEntityName) {
		this.entities = entities;
		this.embeddables = embeddables;
		this.mappedSuperclassTypeMap = mappedSuperclassTypeMap;
        this.entityTypesByEntityName = entityTypesByEntityName;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = entities.get( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		return (EntityType<X>) entityType;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> ManagedType<X> managedType(Class<X> cls) {
		ManagedType<?> type = entities.get( cls );
		if ( type == null ) {
			type = mappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = embeddables.get( cls );
		}
		if ( type == null ) {
			throw new IllegalArgumentException( "Not an managed type: " + cls );
		}
		return (ManagedType<X>) type;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> EmbeddableType<X> embeddable(Class<X> cls) {
		final EmbeddableType<?> embeddableType = embeddables.get( cls );
		if ( embeddableType == null ) {
			throw new IllegalArgumentException( "Not an embeddable: " + cls );
		}
		return (EmbeddableType<X>) embeddableType;
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		final int setSize = CollectionHelper.determineProperSizing(
				entities.size() + mappedSuperclassTypeMap.size() + embeddables.size()
		);
		final Set<ManagedType<?>> managedTypes = new HashSet<ManagedType<?>>( setSize );
		managedTypes.addAll( entities.values() );
		managedTypes.addAll( mappedSuperclassTypeMap.values() );
		managedTypes.addAll( embeddables.values() );
		return managedTypes;
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return new HashSet<EntityType<?>>( entityTypesByEntityName.values() );
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return new HashSet<EmbeddableType<?>>( embeddables.values() );
	}
}
