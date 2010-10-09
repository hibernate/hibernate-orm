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
import java.util.HashSet;
import java.io.Serializable;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Hibernate implementation of the JPA {@link Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements Metamodel, Serializable {
	private final Map<Class<?>,EntityTypeImpl<?>> entities;
	private final Map<Class<?>, EmbeddableTypeImpl<?>> embeddables;

	/**
	 * Build the metamodel using the information from the collection of Hibernate
	 * {@link PersistentClass} models as well as the Hibernate {@link org.hibernate.SessionFactory}.
	 *
	 * @param persistentClasses Iterator over the Hibernate (config-time) metamodel
	 * @param sessionFactory The Hibernate session factry.
	 * @return The built metamodel
	 */
	public static MetamodelImpl buildMetamodel(
			Iterator<PersistentClass> persistentClasses,
			SessionFactoryImplementor sessionFactory) {
		MetadataContext context = new MetadataContext( sessionFactory );
		while ( persistentClasses.hasNext() ) {
			PersistentClass pc = persistentClasses.next();
			if ( pc.getMappedClass() != null ) {
				locateOrBuildEntityType( pc, context );
			}
		}
		context.wrapUp();
		return new MetamodelImpl( context.getEntityTypeMap(), context.getEmbeddableTypeMap() );
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
				persistentClass.getClassName(),
				persistentClass.hasIdentifierProperty(),
				persistentClass.isVersioned()
		);
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
	 */
	private MetamodelImpl(
			Map<Class<?>, EntityTypeImpl<?>> entities,
			Map<Class<?>, EmbeddableTypeImpl<?>> embeddables) {
		this.entities = entities;
		this.embeddables = embeddables;
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
