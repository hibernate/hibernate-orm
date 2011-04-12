/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations;

import java.util.Iterator;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.metamodel.source.Metadata;

/**
 * Main class responsible to creating and binding the Hibernate meta-model from annotations.
 * This binder only has to deal with annotation index. XML configuration is already processed and pseudo annotations
 * are added to the annotation index.
 *
 * @author Hardy Ferentschik
 * @todo On top of the index we probably needs to pass some sort of XMLContext for global configuration data
 * @todo The annotation index should really be passed at construction time
 */
public class AnnotationBinder {
	private static final Logger log = LoggerFactory.getLogger( AnnotationBinder.class );
	private final Metadata metadata;

	public AnnotationBinder(Metadata metadata) {
		this.metadata = metadata;
	}

	public void bindMappedClasses(Index annotationIndex) {
		// need to order our annotated entities into an order we can process
		ConfiguredClassHierarchyBuilder builder = new ConfiguredClassHierarchyBuilder();
		Set<ConfiguredClassHierarchy> hierarchies = builder.createEntityHierarchies( annotationIndex );

		// now we process each hierarchy one at the time
		for ( ConfiguredClassHierarchy hierarchy : hierarchies ) {
			Iterator<ConfiguredClass> iter = hierarchy.iterator();
			while ( iter.hasNext() ) {
				ConfiguredClass entity = iter.next();
				bindEntity( entity );
			}
		}
	}

	public void bindEntity(ConfiguredClass entity) {
		ClassInfo classInfo = entity.getClassInfo();

		//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ENTITY );
		AnnotationInstance mappedSuperClassAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, JPADotNames.MAPPED_SUPER_CLASS
		);
		AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, JPADotNames.HIBERNATE_ENTITY
		);


//		//TODO: be more strict with secondarytable allowance (not for ids, not for secondary table join columns etc)
//		InheritanceState inheritanceState = inheritanceStatePerClass.get( clazzToProcess );
//		AnnotatedClassType classType = mappings.getClassType( clazzToProcess );
//
//		//Queries declared in MappedSuperclass should be usable in Subclasses
//		if ( AnnotatedClassType.EMBEDDABLE_SUPERCLASS.equals( classType ) ) {
//			bindQueries( clazzToProcess, mappings );
//			bindTypeDefs( clazzToProcess, mappings );
//			bindFilterDefs( clazzToProcess, mappings );
//		}
//
//		if ( !isEntityClassType( clazzToProcess, classType ) ) {
//			return;
//		}
//
		log.info( "Binding entity from annotated class: {}", classInfo.name() );
//
//		PersistentClass superEntity = getSuperEntity(
//				clazzToProcess, inheritanceStatePerClass, mappings, inheritanceState
//		);
//
//		PersistentClass persistentClass = makePersistentClass( inheritanceState, superEntity );


		EntityBinder entityBinder = new EntityBinder(
				metadata, classInfo, jpaEntityAnnotation, hibernateEntityAnnotation
		);
//		entityBinder.setInheritanceState( inheritanceState );
//
//		bindQueries( clazzToProcess, mappings );
//		bindFilterDefs( clazzToProcess, mappings );
//		bindTypeDefs( clazzToProcess, mappings );
//		bindFetchProfiles( clazzToProcess, mappings );
//		BinderHelper.bindAnyMetaDefs( clazzToProcess, mappings );
//
//		String schema = "";
//		String table = ""; //might be no @Table annotation on the annotated class
//		String catalog = "";
//		List<UniqueConstraintHolder> uniqueConstraints = new ArrayList<UniqueConstraintHolder>();
//		if ( clazzToProcess.isAnnotationPresent( javax.persistence.Table.class ) ) {
//			javax.persistence.Table tabAnn = clazzToProcess.getAnnotation( javax.persistence.Table.class );
//			table = tabAnn.name();
//			schema = tabAnn.schema();
//			catalog = tabAnn.catalog();
//			uniqueConstraints = TableBinder.buildUniqueConstraintHolders( tabAnn.uniqueConstraints() );
//		}
//
//		Ejb3JoinColumn[] inheritanceJoinedColumns = makeInheritanceJoinColumns(
//				clazzToProcess, mappings, inheritanceState, superEntity
//		);
//		Ejb3DiscriminatorColumn discriminatorColumn = null;
//		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
//			discriminatorColumn = processDiscriminatorProperties(
//					clazzToProcess, mappings, inheritanceState, entityBinder
//			);
//		}
//
//		entityBinder.setProxy( clazzToProcess.getAnnotation( Proxy.class ) );
//		entityBinder.setBatchSize( clazzToProcess.getAnnotation( BatchSize.class ) );
//		entityBinder.setWhere( clazzToProcess.getAnnotation( Where.class ) );
//	    entityBinder.setCache( determineCacheSettings( clazzToProcess, mappings ) );
//
//		//Filters are not allowed on subclasses
//		if ( !inheritanceState.hasParents() ) {
//			bindFilters( clazzToProcess, entityBinder, mappings );
//		}
//
//		entityBinder.bindEntity();
//
//		if ( inheritanceState.hasTable() ) {
//			Check checkAnn = clazzToProcess.getAnnotation( Check.class );
//			String constraints = checkAnn == null ?
//					null :
//					checkAnn.constraints();
//			entityBinder.bindTable(
//					schema, catalog, table, uniqueConstraints,
//					constraints, inheritanceState.hasDenormalizedTable() ?
//							superEntity.getTable() :
//							null
//			);
//		}
//		else {
//			if ( clazzToProcess.isAnnotationPresent( Table.class ) ) {
//				log.warn(
//						"Illegal use of @Table in a subclass of a SINGLE_TABLE hierarchy: " + clazzToProcess
//								.getName()
//				);
//			}
//		}
//
//		PropertyHolder propertyHolder = PropertyHolderBuilder.buildPropertyHolder(
//				clazzToProcess,
//				persistentClass,
//				entityBinder, mappings, inheritanceStatePerClass
//		);
//
//		javax.persistence.SecondaryTable secTabAnn = clazzToProcess.getAnnotation(
//				javax.persistence.SecondaryTable.class
//		);
//		javax.persistence.SecondaryTables secTabsAnn = clazzToProcess.getAnnotation(
//				javax.persistence.SecondaryTables.class
//		);
//		entityBinder.firstLevelSecondaryTablesBinding( secTabAnn, secTabsAnn );
//
//		OnDelete onDeleteAnn = clazzToProcess.getAnnotation( OnDelete.class );
//		boolean onDeleteAppropriate = false;
//		if ( InheritanceType.JOINED.equals( inheritanceState.getType() ) && inheritanceState.hasParents() ) {
//			onDeleteAppropriate = true;
//			final JoinedSubclass jsc = ( JoinedSubclass ) persistentClass;
//			if ( persistentClass.getEntityPersisterClass() == null ) {
//				persistentClass.getRootClass().setEntityPersisterClass( JoinedSubclassEntityPersister.class );
//			}
//			SimpleValue key = new DependantValue( mappings, jsc.getTable(), jsc.getIdentifier() );
//			jsc.setKey( key );
//			ForeignKey fk = clazzToProcess.getAnnotation( ForeignKey.class );
//			if ( fk != null && !BinderHelper.isEmptyAnnotationValue( fk.name() ) ) {
//				key.setForeignKeyName( fk.name() );
//			}
//			if ( onDeleteAnn != null ) {
//				key.setCascadeDeleteEnabled( OnDeleteAction.CASCADE.equals( onDeleteAnn.action() ) );
//			}
//			else {
//				key.setCascadeDeleteEnabled( false );
//			}
//			//we are never in a second pass at that stage, so queue it
//			SecondPass sp = new JoinedSubclassFkSecondPass( jsc, inheritanceJoinedColumns, key, mappings );
//			mappings.addSecondPass( sp );
//			mappings.addSecondPass( new CreateKeySecondPass( jsc ) );
//
//		}
//		else if ( InheritanceType.SINGLE_TABLE.equals( inheritanceState.getType() ) ) {
//			if ( inheritanceState.hasParents() ) {
//				if ( persistentClass.getEntityPersisterClass() == null ) {
//					persistentClass.getRootClass().setEntityPersisterClass( SingleTableEntityPersister.class );
//				}
//			}
//			else {
//				if ( inheritanceState.hasSiblings() || !discriminatorColumn.isImplicit() ) {
//					//need a discriminator column
//					bindDiscriminatorToPersistentClass(
//							(RootClass) persistentClass,
//							discriminatorColumn,
//							entityBinder.getSecondaryTables(),
//							propertyHolder,
//							mappings
//					);
//					entityBinder.bindDiscriminatorValue();//bind it again since the type might have changed
//				}
//			}
//		}
//		else if ( InheritanceType.TABLE_PER_CLASS.equals( inheritanceState.getType() ) ) {
//			if ( inheritanceState.hasParents() ) {
//				if ( persistentClass.getEntityPersisterClass() == null ) {
//					persistentClass.getRootClass().setEntityPersisterClass( UnionSubclassEntityPersister.class );
//				}
//			}
//		}
//		if ( onDeleteAnn != null && !onDeleteAppropriate ) {
//			log.warn(
//					"Inapropriate use of @OnDelete on entity, annotation ignored: {}", propertyHolder.getEntityName()
//			);
//		}
//
//		// try to find class level generators
//		HashMap<String, IdGenerator> classGenerators = buildLocalGenerators( clazzToProcess, mappings );
//
//		// check properties
//		final InheritanceState.ElementsToProcess elementsToProcess = inheritanceState.getElementsToProcess();
//		inheritanceState.postProcess( persistentClass, entityBinder );
//
//		final boolean subclassAndSingleTableStrategy = inheritanceState.getType() == InheritanceType.SINGLE_TABLE
//				&& inheritanceState.hasParents();
//		Set<String> idPropertiesIfIdClass = new HashSet<String>();
//		boolean isIdClass = mapAsIdClass(
//				inheritanceStatePerClass,
//				inheritanceState,
//				persistentClass,
//				entityBinder,
//				propertyHolder,
//				elementsToProcess,
//				idPropertiesIfIdClass,
//				mappings
//		);
//
//		if ( !isIdClass ) {
//			entityBinder.setWrapIdsInEmbeddedComponents( elementsToProcess.getIdPropertyCount() > 1 );
//		}
//
//		processIdPropertiesIfNotAlready(
//				inheritanceStatePerClass,
//				mappings,
//				persistentClass,
//				entityBinder,
//				propertyHolder,
//				classGenerators,
//				elementsToProcess,
//				subclassAndSingleTableStrategy,
//				idPropertiesIfIdClass
//		);
//
//		if ( !inheritanceState.hasParents() ) {
//			final RootClass rootClass = ( RootClass ) persistentClass;
//			mappings.addSecondPass( new CreateKeySecondPass( rootClass ) );
//		}
//		else {
//			superEntity.addSubclass( (Subclass) persistentClass );
//		}
//
//		mappings.addClass( persistentClass );
//
//		//Process secondary tables and complementary definitions (ie o.h.a.Table)
//		mappings.addSecondPass( new SecondaryTableSecondPass( entityBinder, propertyHolder, clazzToProcess ) );
//
//		//add process complementary Table definition (index & all)
//		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Table.class ) );
//		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Tables.class ) );
	}
}


