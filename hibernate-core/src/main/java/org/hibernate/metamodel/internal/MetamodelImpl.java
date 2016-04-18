/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MappedSuperclassType;

import org.hibernate.MappingException;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.HEMLogging;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;

/**
 * Hibernate implementation of the JPA {@link javax.persistence.metamodel.Metamodel} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelImpl implements MetamodelImplementor, Serializable {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( MetamodelImpl.class );

	private final SessionFactoryImplementor sessionFactory;
	private final Map<Class<?>, EntityTypeImpl<?>> entities;
	private final Map<Class<?>, EmbeddableTypeImpl<?>> embeddables;
	private final Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap;
	private final Map<String, EntityTypeImpl<?>> entityTypesByEntityName;
	private final transient Map<String,String> imports;

	/**
	 * Build the metamodel using the information from the collection of Hibernate
	 * {@link PersistentClass} models as well as the Hibernate {@link org.hibernate.SessionFactory}.
	 *
	 * @param persistentClasses Iterator over the Hibernate (config-time) metamodel
	 * @param sessionFactory The Hibernate session factory.
	 *
	 * @return The built metamodel
	 *
	 * @deprecated use {@link #buildMetamodel(Iterator, Set, SessionFactoryImplementor, boolean)} instead
	 */
	@Deprecated
	public static MetamodelImpl buildMetamodel(
			Iterator<PersistentClass> persistentClasses,
			SessionFactoryImplementor sessionFactory) {
		return buildMetamodel( persistentClasses, Collections.emptySet(), sessionFactory, false );
	}

	/**
	 * Build the metamodel using the information from the collection of Hibernate
	 * {@link PersistentClass} models as well as the Hibernate {@link org.hibernate.SessionFactory}.
	 *
	 * @param persistentClasses Iterator over the Hibernate (config-time) metamodel
	 * @param mappedSuperclasses All known MappedSuperclasses
	 * @param sessionFactory The Hibernate session factory.
	 * @param ignoreUnsupported ignore unsupported/unknown annotations (like @Any)
	 *
	 * @return The built metamodel
	 */
	public static MetamodelImpl buildMetamodel(
			Iterator<PersistentClass> persistentClasses,
			Set<MappedSuperclass> mappedSuperclasses,
			SessionFactoryImplementor sessionFactory,
			boolean ignoreUnsupported) {
		MetadataContext context = new MetadataContext( sessionFactory, mappedSuperclasses, ignoreUnsupported );
		while ( persistentClasses.hasNext() ) {
			PersistentClass pc = persistentClasses.next();
			locateOrBuildEntityType( pc, context );
		}
		handleUnusedMappedSuperclasses( context );
		context.wrapUp();
		return new MetamodelImpl(
				sessionFactory,
				context.getEntityTypeMap(),
				context.getEmbeddableTypeMap(),
				context.getMappedSuperclassTypeMap(),
				context.getEntityTypesByEntityName()
		);
	}

	private static void handleUnusedMappedSuperclasses(MetadataContext context) {
		final Set<MappedSuperclass> unusedMappedSuperclasses = context.getUnusedMappedSuperclasses();
		if ( !unusedMappedSuperclasses.isEmpty() ) {
			for ( MappedSuperclass mappedSuperclass : unusedMappedSuperclasses ) {
				log.unusedMappedSuperclass( mappedSuperclass.getMappedClass().getName() );
				locateOrBuildMappedsuperclassType( mappedSuperclass, context );
			}
		}
	}

	private static EntityTypeImpl<?> locateOrBuildEntityType(PersistentClass persistentClass, MetadataContext context) {
		EntityTypeImpl<?> entityType = context.locateEntityType( persistentClass );
		if ( entityType == null ) {
			entityType = buildEntityType( persistentClass, context );
		}
		return entityType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings("unchecked")
	private static EntityTypeImpl<?> buildEntityType(PersistentClass persistentClass, MetadataContext context) {
		final Class javaType = persistentClass.getMappedClass();
		context.pushEntityWorkedOn( persistentClass );
		final MappedSuperclass superMappedSuperclass = persistentClass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = persistentClass.getSuperclass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		EntityTypeImpl entityType = new EntityTypeImpl(
				javaType,
				superType,
				persistentClass
		);

		context.registerEntityType( persistentClass, entityType );
		context.popEntityWorkedOn( persistentClass );
		return entityType;
	}

	private static MappedSuperclassTypeImpl<?> locateOrBuildMappedsuperclassType(
			MappedSuperclass mappedSuperclass, MetadataContext context) {
		MappedSuperclassTypeImpl<?> mappedSuperclassType = context.locateMappedSuperclassType( mappedSuperclass );
		if ( mappedSuperclassType == null ) {
			mappedSuperclassType = buildMappedSuperclassType( mappedSuperclass, context );
		}
		return mappedSuperclassType;
	}

	//TODO remove / reduce @SW scope
	@SuppressWarnings("unchecked")
	private static MappedSuperclassTypeImpl<?> buildMappedSuperclassType(
			MappedSuperclass mappedSuperclass,
			MetadataContext context) {
		final MappedSuperclass superMappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
		AbstractIdentifiableType<?> superType = superMappedSuperclass == null
				? null
				: locateOrBuildMappedsuperclassType( superMappedSuperclass, context );
		//no mappedSuperclass, check for a super entity
		if ( superType == null ) {
			final PersistentClass superPersistentClass = mappedSuperclass.getSuperPersistentClass();
			superType = superPersistentClass == null
					? null
					: locateOrBuildEntityType( superPersistentClass, context );
		}
		final Class javaType = mappedSuperclass.getMappedClass();
		MappedSuperclassTypeImpl mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaType,
				mappedSuperclass,
				superType
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
			SessionFactoryImplementor sessionFactory,
			Map<Class<?>, EntityTypeImpl<?>> entities,
			Map<Class<?>, EmbeddableTypeImpl<?>> embeddables,
			Map<Class<?>, MappedSuperclassType<?>> mappedSuperclassTypeMap,
			Map<String, EntityTypeImpl<?>> entityTypesByEntityName) {
		this.sessionFactory = sessionFactory;
		this.entities = entities;
		this.embeddables = embeddables;
		this.mappedSuperclassTypeMap = mappedSuperclassTypeMap;
		this.entityTypesByEntityName = entityTypesByEntityName;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> EntityType<X> entity(Class<X> cls) {
		final EntityType<?> entityType = entities.get( cls );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "Not an entity: " + cls );
		}
		return (EntityType<X>) entityType;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X> ManagedType<X> managedType(Class<X> cls) {
		ManagedType<?> type = entities.get( cls );
		if ( type == null ) {
			type = mappedSuperclassTypeMap.get( cls );
		}
		if ( type == null ) {
			type = embeddables.get( cls );
		}
		if ( type == null ) {
			throw new IllegalArgumentException( "Not a managed type: " + cls );
		}
		return (ManagedType<X>) type;
	}

	@Override
	@SuppressWarnings({"unchecked"})
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

	@Override
	@SuppressWarnings("unchecked")
	public <X> EntityType<X> entity(String entityName) {
		return (EntityType<X>) entityTypesByEntityName.get( entityName );
	}

	@Override
	public String getImportedClassName(String className) {
		String result = imports.get( className );
		if ( result == null ) {
			try {
				sessionFactory.getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
				imports.put( className, className );
				return className;
			}
			catch ( ClassLoadingException cnfe ) {
				return null;
			}
		}
		else {
			return result;
		}
	}	/**
	 * Given the name of an entity class, determine all the class and interface names by which it can be
	 * referenced in an HQL query.
	 *
	 * @param className The name of the entity class
	 *
	 * @return the names of all persistent (mapped) classes that extend or implement the
	 *     given class or interface, accounting for implicit/explicit polymorphism settings
	 *     and excluding mapped subclasses/joined-subclasses of other classes in the result.
	 * @throws MappingException
	 */
	public String[] getImplementors(String className) throws MappingException {

		final Class clazz;
		try {
			clazz = getSessionFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( className );
		}
		catch (ClassLoadingException e) {
			return new String[] { className }; //for a dynamic-class
		}

		ArrayList<String> results = new ArrayList<>();
		for ( EntityPersister checkPersister : getSessionFactory(.values() ) {
			if ( ! Queryable.class.isInstance( checkPersister ) ) {
				continue;
			}
			final Queryable checkQueryable = Queryable.class.cast( checkPersister );
			final String checkQueryableEntityName = checkQueryable.getEntityName();
			final boolean isMappedClass = className.equals( checkQueryableEntityName );
			if ( checkQueryable.isExplicitPolymorphism() ) {
				if ( isMappedClass ) {
					return new String[] { className }; //NOTE EARLY EXIT
				}
			}
			else {
				if ( isMappedClass ) {
					results.add( checkQueryableEntityName );
				}
				else {
					final Class mappedClass = checkQueryable.getMappedClass();
					if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
						final boolean assignableSuperclass;
						if ( checkQueryable.isInherited() ) {
							Class mappedSuperclass = getEntityPersister( checkQueryable.getMappedSuperclass() ).getMappedClass();
							assignableSuperclass = clazz.isAssignableFrom( mappedSuperclass );
						}
						else {
							assignableSuperclass = false;
						}
						if ( !assignableSuperclass ) {
							results.add( checkQueryableEntityName );
						}
					}
				}
			}
		}
		return results.toArray( new String[results.size()] );
	}

	@Override
	public Map<String, EntityPersister> entityPersisters() {
		return entityPersisters;
	}

	@Override
	public EntityPersister entityPersister(String entityName) throws MappingException {
		EntityPersister result = entityPersisters.get( entityName );
		if ( result == null ) {
			throw new MappingException( "Unknown entity: " + entityName );
		}
		return result;
	}


	@Override
	public EntityPersister locateEntityPersister(Class byClass) {
		EntityPersister entityPersister = entityPersisters.get( byClass.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaceMap.get( byClass );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisters.get( mappedEntityName );
			}
		}

		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byClass.getName() );
		}

		return entityPersister;
	}

	@Override
	public EntityPersister locateEntityPersister(String byName) {
		final EntityPersister entityPersister = entityPersisters.get( byName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byName );
		}
		return entityPersister;
	}

}
