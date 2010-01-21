/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AnnotationException;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.FilterJoinTables;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.IndexColumn;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyHolderBuilder;
import org.hibernate.cfg.PropertyInferredData;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.engine.ExecuteUpdateResultCheckStyle;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdGenerator;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Table;
import org.hibernate.util.StringHelper;

/**
 * Base class for binding different types of collections to Hibernate configuration objects.
 *
 * @author inger
 * @author Emmanuel Bernard
 */
@SuppressWarnings({"unchecked", "serial"})
public abstract class CollectionBinder {

	private Logger log = LoggerFactory.getLogger( CollectionBinder.class );

	protected Collection collection;
	protected String propertyName;
	PropertyHolder propertyHolder;
	int batchSize;
	private String mappedBy;
	private XClass collectionType;
	private XClass targetEntity;
	private ExtendedMappings mappings;
	private Ejb3JoinColumn[] inverseJoinColumns;
	private String cascadeStrategy;
	String cacheConcurrencyStrategy;
	String cacheRegionName;
	private boolean oneToMany;
	protected IndexColumn indexColumn;
	private String orderBy;
	protected String hqlOrderBy;
	private boolean isSorted;
	private Class comparator;
	private boolean hasToBeSorted;
	protected boolean cascadeDeleteEnabled;
	protected String mapKeyPropertyName;
	private boolean insertable = true;
	private boolean updatable = true;
	private Ejb3JoinColumn[] fkJoinColumns;
	private boolean isExplicitAssociationTable;
	private Ejb3Column[] elementColumns;
	private boolean isEmbedded;
	private XProperty property;
	private boolean ignoreNotFound;
	private TableBinder tableBinder;
	private Ejb3Column[] mapKeyColumns;
	private Ejb3JoinColumn[] mapKeyManyToManyColumns;
	protected HashMap<String, IdGenerator> localGenerators;
	protected Map<XClass, InheritanceState> inheritanceStatePerClass;
	private XClass declaringClass;
	private boolean declaringClassSet;
	private AccessType accessType;
	private boolean hibernateExtensionMapping;

	public boolean isMap() {
		return false;
	}

	public void setIsHibernateExtensionMapping(boolean hibernateExtensionMapping) {
		this.hibernateExtensionMapping = hibernateExtensionMapping;
	}

	protected boolean isHibernateExtensionMapping() {
		return hibernateExtensionMapping;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setInheritanceStatePerClass(Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public void setCascadeStrategy(String cascadeStrategy) {
		this.cascadeStrategy = cascadeStrategy;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public void setInverseJoinColumns(Ejb3JoinColumn[] inverseJoinColumns) {
		this.inverseJoinColumns = inverseJoinColumns;
	}

	public void setJoinColumns(Ejb3JoinColumn[] joinColumns) {
		this.joinColumns = joinColumns;
	}

	private Ejb3JoinColumn[] joinColumns;

	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	public void setBatchSize(BatchSize batchSize) {
		this.batchSize = batchSize == null ? -1 : batchSize.size();
	}

	public void setEjb3OrderBy(javax.persistence.OrderBy orderByAnn) {
		if ( orderByAnn != null ) {
			hqlOrderBy = orderByAnn.value();
		}
	}

	public void setSqlOrderBy(OrderBy orderByAnn) {
		if ( orderByAnn != null ) {
			if ( !BinderHelper.isDefault( orderByAnn.clause() ) ) orderBy = orderByAnn.clause();
		}
	}

	public void setSort(Sort sortAnn) {
		if ( sortAnn != null ) {
			isSorted = !SortType.UNSORTED.equals( sortAnn.type() );
			if ( isSorted && SortType.COMPARATOR.equals( sortAnn.type() ) ) {
				comparator = sortAnn.comparator();
			}
		}
	}

	/**
	 * collection binder factory
	 */
	public static CollectionBinder getCollectionBinder(
			String entityName, XProperty property,
			boolean isIndexed, boolean isHibernateExtensionMapping
	) {
		CollectionBinder result;
		if ( property.isArray() ) {
			if ( property.getElementClass().isPrimitive() ) {
				result = new PrimitiveArrayBinder();
			}
			else {
				result = new ArrayBinder();
			}
		}
		else if ( property.isCollection() ) {
			//TODO consider using an XClass
			Class returnedClass = property.getCollectionClass();
			if ( java.util.Set.class.equals( returnedClass ) ) {
				if ( property.isAnnotationPresent( CollectionId.class ) ) {
					throw new AnnotationException( "Set do not support @CollectionId: "
							+ StringHelper.qualify( entityName, property.getName() ) );
				}
				result = new SetBinder();
			}
			else if ( java.util.SortedSet.class.equals( returnedClass ) ) {
				if ( property.isAnnotationPresent( CollectionId.class ) ) {
					throw new AnnotationException( "Set do not support @CollectionId: "
							+ StringHelper.qualify( entityName, property.getName() ) );
				}
				result = new SetBinder( true );
			}
			else if ( java.util.Map.class.equals( returnedClass ) ) {
				if ( property.isAnnotationPresent( CollectionId.class ) ) {
					throw new AnnotationException( "Map do not support @CollectionId: "
							+ StringHelper.qualify( entityName, property.getName() ) );
				}
				result = new MapBinder();
			}
			else if ( java.util.SortedMap.class.equals( returnedClass ) ) {
				if ( property.isAnnotationPresent( CollectionId.class ) ) {
					throw new AnnotationException( "Map do not support @CollectionId: "
							+ StringHelper.qualify( entityName, property.getName() ) );
				}
				result = new MapBinder( true );
			}
			else if ( java.util.Collection.class.equals( returnedClass ) ) {
				if ( property.isAnnotationPresent( CollectionId.class ) ) {
					result = new IdBagBinder();
				}
				else {
					result = new BagBinder();
				}
			}
			else if ( java.util.List.class.equals( returnedClass ) ) {
				if ( isIndexed ) {
					if ( property.isAnnotationPresent( CollectionId.class ) ) {
						throw new AnnotationException(
								"List do not support @CollectionId and @OrderColumn (or @IndexColumn) at the same time: "
								+ StringHelper.qualify( entityName, property.getName() ) );
					}
					result = new ListBinder();
				}
				else if ( property.isAnnotationPresent( CollectionId.class ) ) {
					result = new IdBagBinder();
				}
				else {
					result = new BagBinder();
				}
			}
			else {
				throw new AnnotationException(
						returnedClass.getName() + " collection not yet supported: "
								+ StringHelper.qualify( entityName, property.getName() )
				);
			}
		}
		else {
			throw new AnnotationException(
					"Illegal attempt to map a non collection as a @OneToMany, @ManyToMany or @CollectionOfElements: "
							+ StringHelper.qualify( entityName, property.getName() )
			);
		}
		result.setIsHibernateExtensionMapping( isHibernateExtensionMapping );
		return result;
	}

	protected CollectionBinder() {
	}

	protected CollectionBinder(boolean sorted) {
		this.hasToBeSorted = sorted;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}

	public void setTableBinder(TableBinder tableBinder) {
		this.tableBinder = tableBinder;
	}

	public void setCollectionType(XClass collectionType) {
		this.collectionType = collectionType;
	}

	public void setTargetEntity(XClass targetEntity) {
		this.targetEntity = targetEntity;
	}

	public void setMappings(ExtendedMappings mappings) {
		this.mappings = mappings;
	}

	protected abstract Collection createCollection(PersistentClass persistentClass);

	public Collection getCollection() {
		return collection;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setDeclaringClass(XClass declaringClass) {
		this.declaringClass = declaringClass;
		this.declaringClassSet = true;
	}

	public void bind() {
		this.collection = createCollection( propertyHolder.getPersistentClass() );
		log.debug( "Collection role: {}", StringHelper.qualify( propertyHolder.getPath(), propertyName ) );
		collection.setRole( StringHelper.qualify( propertyHolder.getPath(), propertyName ) );
		collection.setNodeName( propertyName );

		if ( (property.isAnnotationPresent( org.hibernate.annotations.MapKey.class )
				|| property.isAnnotationPresent( MapKeyColumn.class ) )
			&& mapKeyPropertyName != null ) {
			throw new AnnotationException(
					"Cannot mix @javax.persistence.MapKey and @MapKeyColumn or @org.hibernate.annotations.MapKey "
							+ "on the same collection: " + StringHelper.qualify(
							propertyHolder.getPath(), propertyName
					)
			);
		}

		//set laziness
		defineFetchingStrategy();
		collection.setBatchSize( batchSize );
		if ( orderBy != null && hqlOrderBy != null ) {
			throw new AnnotationException(
					"Cannot use sql order by clause in conjunction of EJB3 order by clause: " + safeCollectionRole()
			);
		}

		collection.setMutable( !property.isAnnotationPresent( Immutable.class ) );
		OptimisticLock lockAnn = property.getAnnotation( OptimisticLock.class );
		if ( lockAnn != null ) collection.setOptimisticLocked( !lockAnn.excluded() );
		Persister persisterAnn = property.getAnnotation( Persister.class );
		if ( persisterAnn != null ) collection.setCollectionPersisterClass( persisterAnn.impl() );

		// set ordering
		if ( orderBy != null ) collection.setOrderBy( orderBy );
		if ( isSorted ) {
			collection.setSorted( true );
			if ( comparator != null ) {
				try {
					collection.setComparator( (Comparator) comparator.newInstance() );
				}
				catch (ClassCastException e) {
					throw new AnnotationException(
							"Comparator not implementing java.util.Comparator class: "
									+ comparator.getName() + "(" + safeCollectionRole() + ")"
					);
				}
				catch (Exception e) {
					throw new AnnotationException(
							"Could not instantiate comparator class: "
									+ comparator.getName() + "(" + safeCollectionRole() + ")"
					);
				}
			}
		}
		else {
			if ( hasToBeSorted ) {
				throw new AnnotationException(
						"A sorted collection has to define @Sort: "
								+ safeCollectionRole()
				);
			}
		}

		//set cache
		if ( StringHelper.isNotEmpty( cacheConcurrencyStrategy ) ) {
			collection.setCacheConcurrencyStrategy( cacheConcurrencyStrategy );
			collection.setCacheRegionName( cacheRegionName );
		}

		//SQL overriding
		SQLInsert sqlInsert = property.getAnnotation( SQLInsert.class );
		SQLUpdate sqlUpdate = property.getAnnotation( SQLUpdate.class );
		SQLDelete sqlDelete = property.getAnnotation( SQLDelete.class );
		SQLDeleteAll sqlDeleteAll = property.getAnnotation( SQLDeleteAll.class );
		Loader loader = property.getAnnotation( Loader.class );
		if ( sqlInsert != null ) {
			collection.setCustomSQLInsert( sqlInsert.sql().trim(), sqlInsert.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlInsert.check().toString().toLowerCase() )
			);

		}
		if ( sqlUpdate != null ) {
			collection.setCustomSQLUpdate( sqlUpdate.sql(), sqlUpdate.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlUpdate.check().toString().toLowerCase() )
			);
		}
		if ( sqlDelete != null ) {
			collection.setCustomSQLDelete( sqlDelete.sql(), sqlDelete.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlDelete.check().toString().toLowerCase() )
			);
		}
		if ( sqlDeleteAll != null ) {
			collection.setCustomSQLDeleteAll( sqlDeleteAll.sql(), sqlDeleteAll.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlDeleteAll.check().toString().toLowerCase() )
			);
		}
		if ( loader != null ) {
			collection.setLoaderName( loader.namedQuery() );
		}

		//work on association
		boolean isMappedBy = !BinderHelper.isDefault( mappedBy );

		if (isMappedBy
				&& (property.isAnnotationPresent( JoinColumn.class )
					|| property.isAnnotationPresent( JoinColumns.class )
					|| propertyHolder.getJoinTable( property ) != null ) ) {
			String message = "Associations marked as mappedBy must not define database mappings like @JoinTable or @JoinColumn: ";
			message += StringHelper.qualify( propertyHolder.getPath(), propertyName );
			throw new AnnotationException( message );
		}

		collection.setInverse( isMappedBy );

		//many to many may need some second pass informations
		if ( !oneToMany && isMappedBy ) {
			mappings.addMappedBy( getCollectionType().getName(), mappedBy, propertyName );
		}
		//TODO reducce tableBinder != null and oneToMany
		XClass collectionType = getCollectionType();
		if ( inheritanceStatePerClass == null) throw new AssertionFailure( "inheritanceStatePerClass not set" );
		SecondPass sp = getSecondPass(
				fkJoinColumns,
				joinColumns,
				inverseJoinColumns,
				elementColumns,
				mapKeyColumns, mapKeyManyToManyColumns, isEmbedded,
				property, collectionType,
				ignoreNotFound, oneToMany,
				tableBinder, mappings
		);
		if ( collectionType.isAnnotationPresent( Embeddable.class )
				|| property.isAnnotationPresent( CollectionOfElements.class ) //legacy hibernate
				|| property.isAnnotationPresent( ElementCollection.class ) //JPA 2
				) {
			// do it right away, otherwise @ManyToOne on composite element call addSecondPass
			// and raise a ConcurrentModificationException
			//sp.doSecondPass( CollectionHelper.EMPTY_MAP );
			mappings.addSecondPass( sp, !isMappedBy );
		}
		else {
			mappings.addSecondPass( sp, !isMappedBy );
		}

		mappings.addCollection( collection );

		//property building
		PropertyBinder binder = new PropertyBinder();
		binder.setName( propertyName );
		binder.setValue( collection );
		binder.setCascade( cascadeStrategy );
		if ( cascadeStrategy != null && cascadeStrategy.indexOf( "delete-orphan" ) >= 0 ) {
			collection.setOrphanDelete( true );
		}
		binder.setAccessType( accessType );
		binder.setProperty( property );
		binder.setInsertable( insertable );
		binder.setUpdatable( updatable );
		Property prop = binder.makeProperty();
		//we don't care about the join stuffs because the column is on the association table.
		if (! declaringClassSet) throw new AssertionFailure( "DeclaringClass is not set in CollectionBinder while binding" );
		propertyHolder.addProperty( prop, declaringClass );
	}

	private void defineFetchingStrategy() {
		LazyCollection lazy = property.getAnnotation( LazyCollection.class );
		Fetch fetch = property.getAnnotation( Fetch.class );
		OneToMany oneToMany = property.getAnnotation( OneToMany.class );
		ManyToMany manyToMany = property.getAnnotation( ManyToMany.class );
		CollectionOfElements collectionOfElements = property.getAnnotation( CollectionOfElements.class ); //legacy hibernate
		ElementCollection elementCollection = property.getAnnotation( ElementCollection.class ); //jpa 2
		ManyToAny manyToAny = property.getAnnotation( ManyToAny.class );
		FetchType fetchType;
		if ( oneToMany != null ) {
			fetchType = oneToMany.fetch();
		}
		else if ( manyToMany != null ) {
			fetchType = manyToMany.fetch();
		}
		else if ( elementCollection != null ) {
			fetchType = elementCollection.fetch();
		}
		else if ( collectionOfElements != null ) {
			fetchType = collectionOfElements.fetch();
		}
		else if ( manyToAny != null ) {
			fetchType = FetchType.LAZY;
		}
		else {
			throw new AssertionFailure(
					"Define fetch strategy on a property not annotated with @ManyToOne nor @OneToMany nor @CollectionOfElements"
			);
		}
		if ( lazy != null ) {
			collection.setLazy( !( lazy.value() == LazyCollectionOption.FALSE ) );
			collection.setExtraLazy( lazy.value() == LazyCollectionOption.EXTRA );
		}
		else {
			collection.setLazy( fetchType == FetchType.LAZY );
			collection.setExtraLazy( false );
		}
		if ( fetch != null ) {
			if ( fetch.value() == org.hibernate.annotations.FetchMode.JOIN ) {
				collection.setFetchMode( FetchMode.JOIN );
				collection.setLazy( false );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SELECT ) {
				collection.setFetchMode( FetchMode.SELECT );
			}
			else if ( fetch.value() == org.hibernate.annotations.FetchMode.SUBSELECT ) {
				collection.setFetchMode( FetchMode.SELECT );
				collection.setSubselectLoadable( true );
				collection.getOwner().setSubselectLoadableCollections( true );
			}
			else {
				throw new AssertionFailure( "Unknown FetchMode: " + fetch.value() );
			}
		}
		else {
			collection.setFetchMode( AnnotationBinder.getFetchMode( fetchType ) );
		}
	}

	private XClass getCollectionType() {
		if ( AnnotationBinder.isDefault( targetEntity, mappings ) ) {
			if ( collectionType != null ) {
				return collectionType;
			}
			else {
				String errorMsg = "Collection has neither generic type or OneToMany.targetEntity() defined: "
						+ safeCollectionRole();
				throw new AnnotationException( errorMsg );
			}
		}
		else {
			return targetEntity;
		}
	}

	public SecondPass getSecondPass(
			final Ejb3JoinColumn[] fkJoinColumns, final Ejb3JoinColumn[] keyColumns,
			final Ejb3JoinColumn[] inverseColumns,
			final Ejb3Column[] elementColumns,
			final Ejb3Column[] mapKeyColumns, final Ejb3JoinColumn[] mapKeyManyToManyColumns, final boolean isEmbedded,
			final XProperty property, final XClass collType,
			final boolean ignoreNotFound, final boolean unique,
			final TableBinder assocTableBinder, final ExtendedMappings mappings
	) {
		return new CollectionSecondPass( mappings, collection ) {

			public void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
					throws MappingException {
				bindStarToManySecondPass(
						persistentClasses, collType, fkJoinColumns, keyColumns, inverseColumns, elementColumns,
						isEmbedded, property, unique, assocTableBinder, ignoreNotFound, mappings
				);

			}
		};
	}

	/**
	 * return true if it's a Fk, false if it's an association table
	 */
	protected boolean bindStarToManySecondPass(
			Map persistentClasses, XClass collType, Ejb3JoinColumn[] fkJoinColumns,
			Ejb3JoinColumn[] keyColumns, Ejb3JoinColumn[] inverseColumns, Ejb3Column[] elementColumns,
			boolean isEmbedded,
			XProperty property, boolean unique,
			TableBinder associationTableBinder,
			boolean ignoreNotFound, ExtendedMappings mappings
	) {
		PersistentClass persistentClass = (PersistentClass) persistentClasses.get( collType.getName() );
		boolean reversePropertyInJoin = false;
		if ( persistentClass != null && StringHelper.isNotEmpty( this.mappedBy ) ) {
			try {
				reversePropertyInJoin = 0 != persistentClass.getJoinNumber(
						persistentClass.getRecursiveProperty( this.mappedBy )
				);
			}
			catch (MappingException e) {
				StringBuilder error = new StringBuilder( 80 );
				error.append( "mappedBy reference an unknown target entity property: " )
						.append( collType ).append( "." ).append( this.mappedBy )
						.append( " in " )
						.append( collection.getOwnerEntityName() )
						.append( "." )
						.append( property.getName() );
				throw new AnnotationException( error.toString() );
			}
		}
		if ( persistentClass != null
				&& !reversePropertyInJoin
				&& oneToMany
				&& !this.isExplicitAssociationTable
				&& ( joinColumns[0].isImplicit() && !BinderHelper.isDefault( this.mappedBy ) //implicit @JoinColumn
				|| !fkJoinColumns[0].isImplicit() ) //this is an explicit @JoinColumn
				) {
			//this is a Foreign key
			bindOneToManySecondPass(
					getCollection(),
					persistentClasses,
					fkJoinColumns,
					collType,
					cascadeDeleteEnabled,
					ignoreNotFound, hqlOrderBy,
					mappings,
					inheritanceStatePerClass
			);
			return true;
		}
		else {
			//this is an association table
			bindManyToManySecondPass(
					this.collection,
					persistentClasses,
					keyColumns,
					inverseColumns,
					elementColumns,
					isEmbedded, collType,
					ignoreNotFound, unique,
					cascadeDeleteEnabled,
					associationTableBinder, property, propertyHolder, hqlOrderBy, mappings
			);
			return false;
		}
	}

	protected void bindOneToManySecondPass(
			Collection collection, Map persistentClasses, Ejb3JoinColumn[] fkJoinColumns,
			XClass collectionType,
			boolean cascadeDeleteEnabled, boolean ignoreNotFound, String hqlOrderBy, ExtendedMappings extendedMappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass
	) {

		log.debug("Binding a OneToMany: {}.{} through a foreign key", propertyHolder.getEntityName(), propertyName);
		org.hibernate.mapping.OneToMany oneToMany = new org.hibernate.mapping.OneToMany( collection.getOwner() );
		collection.setElement( oneToMany );
		oneToMany.setReferencedEntityName( collectionType.getName() );
		oneToMany.setIgnoreNotFound( ignoreNotFound );

		String assocClass = oneToMany.getReferencedEntityName();
		PersistentClass associatedClass = (PersistentClass) persistentClasses.get( assocClass );
		String orderBy = buildOrderByClauseFromHql( hqlOrderBy, associatedClass, collection.getRole() );
		if ( orderBy != null ) collection.setOrderBy( orderBy );
		if ( mappings == null ) {
			throw new AssertionFailure(
					"CollectionSecondPass for oneToMany should not be called with null mappings"
			);
		}
		Map<String, Join> joins = mappings.getJoins( assocClass );
		if ( associatedClass == null ) {
			throw new MappingException(
					"Association references unmapped class: " + assocClass
			);
		}
		oneToMany.setAssociatedClass( associatedClass );
		for (Ejb3JoinColumn column : fkJoinColumns) {
			column.setPersistentClass( associatedClass, joins, inheritanceStatePerClass );
			column.setJoins( joins );
			collection.setCollectionTable( column.getTable() );
		}
		log.info(
				"Mapping collection: " + collection.getRole() + " -> " + collection.getCollectionTable().getName()
		);
		bindFilters( false );
		bindCollectionSecondPass( collection, null, fkJoinColumns, cascadeDeleteEnabled, property, mappings );
		if ( !collection.isInverse()
				&& !collection.getKey().isNullable() ) {
			// for non-inverse one-to-many, with a not-null fk, add a backref!
			String entityName = oneToMany.getReferencedEntityName();
			PersistentClass referenced = mappings.getClass( entityName );
			Backref prop = new Backref();
			prop.setName( '_' + fkJoinColumns[0].getPropertyName() + "Backref" );
			prop.setUpdateable( false );
			prop.setSelectable( false );
			prop.setCollectionRole( collection.getRole() );
			prop.setEntityName( collection.getOwner().getEntityName() );
			prop.setValue( collection.getKey() );
			referenced.addProperty( prop );
		}
	}


	private void bindFilters(boolean hasAssociationTable) {
		Filter simpleFilter = property.getAnnotation( Filter.class );
		//set filtering
		//test incompatible choices
		//if ( StringHelper.isNotEmpty( where ) ) collection.setWhere( where );
		if ( simpleFilter != null ) {
			if ( hasAssociationTable ) {
				collection.addManyToManyFilter( simpleFilter.name(), getCondition( simpleFilter ) );
			}
			else {
				collection.addFilter( simpleFilter.name(), getCondition( simpleFilter ) );
			}
		}
		Filters filters = property.getAnnotation( Filters.class );
		if ( filters != null ) {
			for (Filter filter : filters.value()) {
				if ( hasAssociationTable ) {
					collection.addManyToManyFilter( filter.name(), getCondition( filter ) );
				}
				else {
					collection.addFilter( filter.name(), getCondition( filter ) );
				}
			}
		}
		FilterJoinTable simpleFilterJoinTable = property.getAnnotation( FilterJoinTable.class );
		if ( simpleFilterJoinTable != null ) {
			if ( hasAssociationTable ) {
				collection.addFilter( simpleFilterJoinTable.name(), getCondition( simpleFilterJoinTable ) );
			}
			else {
				throw new AnnotationException(
						"Illegal use of @FilterJoinTable on an association without join table:"
								+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
				);
			}
		}
		FilterJoinTables filterJoinTables = property.getAnnotation( FilterJoinTables.class );
		if ( filterJoinTables != null ) {
			for (FilterJoinTable filter : filterJoinTables.value()) {
				if ( hasAssociationTable ) {
					collection.addFilter( filter.name(), getCondition( filter ) );
				}
				else {
					throw new AnnotationException(
							"Illegal use of @FilterJoinTable on an association without join table:"
									+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
					);
				}
			}
		}

		Where where = property.getAnnotation( Where.class );
		String whereClause = where == null ? null : where.clause();
		if ( StringHelper.isNotEmpty( whereClause ) ) {
			if ( hasAssociationTable ) {
				collection.setManyToManyWhere( whereClause );
			}
			else {
				collection.setWhere( whereClause );
			}
		}

		WhereJoinTable whereJoinTable = property.getAnnotation( WhereJoinTable.class );
		String whereJoinTableClause = whereJoinTable == null ? null : whereJoinTable.clause();
		if ( StringHelper.isNotEmpty( whereJoinTableClause ) ) {
			if ( hasAssociationTable ) {
				collection.setWhere( whereJoinTableClause );
			}
			else {
				throw new AnnotationException(
						"Illegal use of @WhereJoinTable on an association without join table:"
								+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
				);
			}
		}
//		This cannot happen in annotations since the second fetch is hardcoded to join
//		if ( ( ! collection.getManyToManyFilterMap().isEmpty() || collection.getManyToManyWhere() != null ) &&
//		        collection.getFetchMode() == FetchMode.JOIN &&
//		        collection.getElement().getFetchMode() != FetchMode.JOIN ) {
//			throw new MappingException(
//			        "association with join table  defining filter or where without join fetching " +
//			        "not valid within collection using join fetching [" + collection.getRole() + "]"
//				);
//		}
	}

	private String getCondition(FilterJoinTable filter) {
		//set filtering
		String name = filter.name();
		String cond = filter.condition();
		return getCondition( cond, name );
	}

	private String getCondition(Filter filter) {
		//set filtering
		String name = filter.name();
		String cond = filter.condition();
		return getCondition( cond, name );
	}

	private String getCondition(String cond, String name) {
		if ( BinderHelper.isDefault( cond ) ) {
			cond = mappings.getFilterDefinition( name ).getDefaultFilterCondition();
			if ( StringHelper.isEmpty( cond ) ) {
				throw new AnnotationException(
						"no filter condition found for filter " + name + " in "
								+ StringHelper.qualify( propertyHolder.getPath(), propertyName )
				);
			}
		}
		return cond;
	}

	public void setCache(Cache cacheAnn) {
		if ( cacheAnn != null ) {
			cacheRegionName = BinderHelper.isDefault( cacheAnn.region() ) ? null : cacheAnn.region();
			cacheConcurrencyStrategy = EntityBinder.getCacheConcurrencyStrategy( cacheAnn.usage() );
		}
		else {
			cacheConcurrencyStrategy = null;
			cacheRegionName = null;
		}
	}

	public void setOneToMany(boolean oneToMany) {
		this.oneToMany = oneToMany;
	}

	public void setIndexColumn(IndexColumn indexColumn) {
		this.indexColumn = indexColumn;
	}

	public void setMapKey(MapKey key) {
		if ( key != null ) {
			mapKeyPropertyName = key.name();
		}
	}

	private static String buildOrderByClauseFromHql(String hqlOrderBy, PersistentClass associatedClass, String role) {
		String orderByString = null;
		if ( hqlOrderBy != null ) {
			List<String> properties = new ArrayList<String>();
			List<String> ordering = new ArrayList<String>();
			StringBuilder orderByBuffer = new StringBuilder();
			if ( hqlOrderBy.length() == 0 ) {
				//order by id
				Iterator it = associatedClass.getIdentifier().getColumnIterator();
				while ( it.hasNext() ) {
					Selectable col = (Selectable) it.next();
					orderByBuffer.append( col.getText() ).append( " asc" ).append( ", " );
				}
			}
			else {
				StringTokenizer st = new StringTokenizer( hqlOrderBy, " ,", false );
				String currentOrdering = null;
				//FIXME make this code decent
				while ( st.hasMoreTokens() ) {
					String token = st.nextToken();
					if ( isNonPropertyToken( token ) ) {
						if ( currentOrdering != null ) {
							throw new AnnotationException(
									"Error while parsing HQL orderBy clause: " + hqlOrderBy
											+ " (" + role + ")"
							);
						}
						currentOrdering = token;
					}
					else {
						//Add ordering of the previous
						if ( currentOrdering == null ) {
							//default ordering
							ordering.add( "asc" );
						}
						else {
							ordering.add( currentOrdering );
							currentOrdering = null;
						}
						properties.add( token );
					}
				}
				ordering.remove( 0 ); //first one is the algorithm starter
				// add last one ordering
				if ( currentOrdering == null ) {
					//default ordering
					ordering.add( "asc" );
				}
				else {
					ordering.add( currentOrdering );
					currentOrdering = null;
				}
				int index = 0;

				for (String property : properties) {
					Property p = BinderHelper.findPropertyByName( associatedClass, property );
					if ( p == null ) {
						throw new AnnotationException(
								"property from @OrderBy clause not found: "
										+ associatedClass.getEntityName() + "." + property
						);
					}
					PersistentClass pc = p.getPersistentClass();
					String table;
					if ( pc == null ) {
						//we are touching a @IdClass property, the pc is not set
						//this means pc == associatedClass
						//TODO check whether @ManyToOne @JoinTable in @IdClass used for @OrderBy works: doh!
						table = "";
					}
					
					else if (pc == associatedClass
							|| (associatedClass instanceof SingleTableSubclass && pc
									.getMappedClass().isAssignableFrom(
											associatedClass.getMappedClass()))) {
						table = "";
					} else {
						table = pc.getTable().getQuotedName() + ".";
					}
					
					Iterator propertyColumns = p.getColumnIterator();
					while ( propertyColumns.hasNext() ) {
						Selectable column = (Selectable) propertyColumns.next();
						orderByBuffer.append( table )
								.append( column.getText() )
								.append( " " )
								.append( ordering.get( index ) )
								.append( ", " );
					}
					index++;
				}
			}
			orderByString = orderByBuffer.substring( 0, orderByBuffer.length() - 2 );
		}
		return orderByString;
	}

	private static String buildOrderByClauseFromHql(String hqlOrderBy, Component component, String role) {
		String orderByString = null;
		if ( hqlOrderBy != null ) {
			List<String> properties = new ArrayList<String>();
			List<String> ordering = new ArrayList<String>();
			StringBuilder orderByBuffer = new StringBuilder();
			if ( hqlOrderBy.length() == 0 ) {
				//TODO : Check that. Maybe order by key for maps
			}
			else {
				StringTokenizer st = new StringTokenizer( hqlOrderBy, " ,", false );
				String currentOrdering = null;
				//FIXME make this code decent
				while ( st.hasMoreTokens() ) {
					String token = st.nextToken();
					if ( isNonPropertyToken( token ) ) {
						if ( currentOrdering != null ) {
							throw new AnnotationException(
									"Error while parsing HQL orderBy clause: " + hqlOrderBy
											+ " (" + role + ")"
							);
						}
						currentOrdering = token;
					}
					else {
						//Add ordering of the previous
						if ( currentOrdering == null ) {
							//default ordering
							ordering.add( "asc" );
						}
						else {
							ordering.add( currentOrdering );
							currentOrdering = null;
						}
						properties.add( token );
					}
				}
				ordering.remove( 0 ); //first one is the algorithm starter
				// add last one ordering
				if ( currentOrdering == null ) {
					//default ordering
					ordering.add( "asc" );
				}
				else {
					ordering.add( currentOrdering );
					currentOrdering = null;
				}
				int index = 0;

				for (String property : properties) {
					Property p = BinderHelper.findPropertyByName( component, property );
					if ( p == null ) {
						throw new AnnotationException(
								"property from @OrderBy clause not found: "
										+ role + "." + property
						);
					}

					Iterator propertyColumns = p.getColumnIterator();
					while ( propertyColumns.hasNext() ) {
						Selectable column = (Selectable) propertyColumns.next();
						orderByBuffer.append( column.getText() )
								.append( " " )
								.append( ordering.get( index ) )
								.append( ", " );
					}
					index++;
				}

				if ( orderByBuffer.length() >= 2 ) {
					orderByString = orderByBuffer.substring( 0, orderByBuffer.length() - 2 );
				}
			}
		}
		return orderByString;
	}

	private static boolean isNonPropertyToken(String token) {
		if ( " ".equals( token ) ) return true;
		if ( ",".equals( token ) ) return true;
		if ( token.equalsIgnoreCase( "desc" ) ) return true;
		if ( token.equalsIgnoreCase( "asc" ) ) return true;
		return false;
	}

	private static SimpleValue buildCollectionKey(
			Collection collValue, Ejb3JoinColumn[] joinColumns, boolean cascadeDeleteEnabled,
			XProperty property, ExtendedMappings mappings
	) {
		//binding key reference using column
		KeyValue keyVal;
		//give a chance to override the referenced property name
		//has to do that here because the referencedProperty creation happens in a FKSecondPass for Many to one yuk!
		if ( joinColumns.length > 0 && StringHelper.isNotEmpty( joinColumns[0].getMappedBy() ) ) {
			String entityName = joinColumns[0].getManyToManyOwnerSideEntityName() != null ?
					"inverse__" + joinColumns[0].getManyToManyOwnerSideEntityName() :
					joinColumns[0].getPropertyHolder().getEntityName();
			String propRef = mappings.getPropertyReferencedAssociation(
					entityName,
					joinColumns[0].getMappedBy()
			);
			if ( propRef != null ) {
				collValue.setReferencedPropertyName( propRef );
				mappings.addPropertyReference( collValue.getOwnerEntityName(), propRef );
			}
		}
		String propRef = collValue.getReferencedPropertyName();
		if ( propRef == null ) {
			keyVal = collValue.getOwner().getIdentifier();
		}
		else {
			keyVal = (KeyValue) collValue.getOwner()
					.getRecursiveProperty( propRef )
					.getValue();
		}
		DependantValue key = new DependantValue( collValue.getCollectionTable(), keyVal );
		key.setTypeName( null );
		Ejb3Column.checkPropertyConsistency( joinColumns, collValue.getOwnerEntityName() );
		key.setNullable( joinColumns.length == 0 || joinColumns[0].isNullable() );
		key.setUpdateable( joinColumns.length == 0 || joinColumns[0].isUpdatable() );
		key.setCascadeDeleteEnabled( cascadeDeleteEnabled );
		collValue.setKey( key );
		ForeignKey fk = property != null ? property.getAnnotation( ForeignKey.class ) : null;
		String fkName = fk != null ? fk.name() : "";
		if ( !BinderHelper.isDefault( fkName ) ) key.setForeignKeyName( fkName );
		return key;
	}

	protected void bindManyToManySecondPass(
			Collection collValue,
			Map persistentClasses,
			Ejb3JoinColumn[] joinColumns,
			Ejb3JoinColumn[] inverseJoinColumns,
			Ejb3Column[] elementColumns,
			boolean isEmbedded,
			XClass collType,
			boolean ignoreNotFound, boolean unique,
			boolean cascadeDeleteEnabled,
			TableBinder associationTableBinder, XProperty property, PropertyHolder parentPropertyHolder,
			String hqlOrderBy, ExtendedMappings mappings
	) throws MappingException {

		PersistentClass collectionEntity = (PersistentClass) persistentClasses.get( collType.getName() );
		boolean isCollectionOfEntities = collectionEntity != null;
		ManyToAny anyAnn = property.getAnnotation( ManyToAny.class );
		if ( log.isDebugEnabled() ) {
			String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
			if ( isCollectionOfEntities && unique ) {
				log.debug( "Binding a OneToMany: {} through an association table", path );
			}
			else if ( isCollectionOfEntities ) {
				log.debug( "Binding as ManyToMany: {}", path );
			}
			else if ( anyAnn != null ) {
				log.debug( "Binding a ManyToAny: {}", path );
			}
			else {
				log.debug( "Binding a collection of element: {}", path );
			}
		}
		//check for user error
		if ( !isCollectionOfEntities ) {
			if ( property.isAnnotationPresent( ManyToMany.class ) || property.isAnnotationPresent( OneToMany.class ) ) {
				String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
				throw new AnnotationException(
						"Use of @OneToMany or @ManyToMany targeting an unmapped class: " + path + "[" + collType + "]"
				);
			}
			else if ( anyAnn != null ) {
				if ( parentPropertyHolder.getJoinTable( property ) == null ) {
					String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
					throw new AnnotationException(
							"@JoinTable is mandatory when @ManyToAny is used: " + path
					);
				}
			}
			else {
				JoinTable joinTableAnn = parentPropertyHolder.getJoinTable( property );
				if ( joinTableAnn != null && joinTableAnn.inverseJoinColumns().length > 0 ) {
					String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
					throw new AnnotationException(
							"Use of @JoinTable.inverseJoinColumns targeting an unmapped class: " + path + "[" + collType + "]"
					);
				}
			}
		}

		boolean mappedBy = !BinderHelper.isDefault( joinColumns[0].getMappedBy() );
		if ( mappedBy ) {
			if ( !isCollectionOfEntities ) {
				StringBuilder error = new StringBuilder( 80 )
						.append(
								"Collection of elements must not have mappedBy or association reference an unmapped entity: "
						)
						.append( collValue.getOwnerEntityName() )
						.append( "." )
						.append( joinColumns[0].getPropertyName() );
				throw new AnnotationException( error.toString() );
			}
			Property otherSideProperty;
			try {
				otherSideProperty = collectionEntity.getRecursiveProperty( joinColumns[0].getMappedBy() );
			}
			catch (MappingException e) {
				StringBuilder error = new StringBuilder( 80 );
				error.append( "mappedBy reference an unknown target entity property: " )
						.append( collType ).append( "." ).append( joinColumns[0].getMappedBy() )
						.append( " in " )
						.append( collValue.getOwnerEntityName() )
						.append( "." )
						.append( joinColumns[0].getPropertyName() );
				throw new AnnotationException( error.toString() );
			}
			Table table;
			if ( otherSideProperty.getValue() instanceof Collection ) {
				//this is a collection on the other side
				table = ( (Collection) otherSideProperty.getValue() ).getCollectionTable();
			}
			else {
				//This is a ToOne with a @JoinTable or a regular property
				table = otherSideProperty.getValue().getTable();
			}
			collValue.setCollectionTable( table );
			String entityName = collectionEntity.getEntityName();
			for (Ejb3JoinColumn column : joinColumns) {
				//column.setDefaultColumnHeader( joinColumns[0].getMappedBy() ); //seems not to be used, make sense
				column.setManyToManyOwnerSideEntityName( entityName );
			}
		}
		else {
			//TODO: only for implicit columns?
			//FIXME NamingStrategy
			for (Ejb3JoinColumn column : joinColumns) {
				String mappedByProperty = mappings.getFromMappedBy(
						collValue.getOwnerEntityName(), column.getPropertyName()
				);
				Table ownerTable = collValue.getOwner().getTable();
				column.setMappedBy(
						collValue.getOwner().getEntityName(), mappings.getLogicalTableName( ownerTable ),
						mappedByProperty
				);
//				String header = ( mappedByProperty == null ) ? mappings.getLogicalTableName( ownerTable ) : mappedByProperty;
//				column.setDefaultColumnHeader( header );
			}
			if ( StringHelper.isEmpty( associationTableBinder.getName() ) ) {
				//default value
				associationTableBinder.setDefaultName(
						collValue.getOwner().getEntityName(),
						mappings.getLogicalTableName( collValue.getOwner().getTable() ),
						collectionEntity != null ? collectionEntity.getEntityName() : null,
						collectionEntity != null ? mappings.getLogicalTableName( collectionEntity.getTable() ) : null,
						joinColumns[0].getPropertyName()
				);
			}
			associationTableBinder.setJPA2ElementCollection( !isCollectionOfEntities && property.isAnnotationPresent( ElementCollection.class ));
			collValue.setCollectionTable( associationTableBinder.bind() );
		}
		bindFilters( isCollectionOfEntities );
		bindCollectionSecondPass( collValue, collectionEntity, joinColumns, cascadeDeleteEnabled, property, mappings );

		ManyToOne element = null;
		if ( isCollectionOfEntities ) {
			element =
					new ManyToOne( collValue.getCollectionTable() );
			collValue.setElement( element );
			element.setReferencedEntityName( collType.getName() );
			//element.setFetchMode( fetchMode );
			//element.setLazy( fetchMode != FetchMode.JOIN );
			//make the second join non lazy
			element.setFetchMode( FetchMode.JOIN );
			element.setLazy( false );
			element.setIgnoreNotFound( ignoreNotFound );
			// as per 11.1.38 of JPA-2 spec, default to primary key if no column is specified by @OrderBy. 
			if ( hqlOrderBy != null ) {
				collValue.setManyToManyOrdering(
						buildOrderByClauseFromHql( hqlOrderBy, collectionEntity, collValue.getRole() )
				);
			}
			ForeignKey fk = property != null ? property.getAnnotation( ForeignKey.class ) : null;
			String fkName = fk != null ? fk.inverseName() : "";
			if ( !BinderHelper.isDefault( fkName ) ) element.setForeignKeyName( fkName );
		}
		else if ( anyAnn != null ) {
			//@ManyToAny
			//Make sure that collTyp is never used during the @ManyToAny branch: it will be set to void.class
			PropertyData inferredData = new PropertyInferredData(null, property, "unsupported", mappings.getReflectionManager() );
			//override the table
			for (Ejb3Column column : inverseJoinColumns) {
				column.setTable( collValue.getCollectionTable() );
			}
			Any any = BinderHelper.buildAnyValue( anyAnn.metaDef(), inverseJoinColumns, anyAnn.metaColumn(),
					inferredData, cascadeDeleteEnabled, Nullability.NO_CONSTRAINT,
					propertyHolder, new EntityBinder(), true, mappings );
			collValue.setElement( any );
		}
		else {
			XClass elementClass;
			AnnotatedClassType classType;

			PropertyHolder holder = null;
			if ( BinderHelper.PRIMITIVE_NAMES.contains( collType.getName() ) ) {
				classType = AnnotatedClassType.NONE;
				elementClass = null;
			}
			else {
				elementClass = collType;
				classType = mappings.getClassType( elementClass );

				holder = PropertyHolderBuilder.buildPropertyHolder(
						collValue,
						collValue.getRole(),
						elementClass,
						property, parentPropertyHolder, mappings
				);
				//force in case of attribute override
				boolean attributeOverride = property.isAnnotationPresent( AttributeOverride.class )
						|| property.isAnnotationPresent( AttributeOverrides.class );
				if ( isEmbedded || attributeOverride ) {
					classType = AnnotatedClassType.EMBEDDABLE;
				}
			}

			if ( AnnotatedClassType.EMBEDDABLE.equals( classType ) ) {
				EntityBinder entityBinder = new EntityBinder();
				PersistentClass owner = collValue.getOwner();
				boolean isPropertyAnnotated;
				//FIXME support @Access for collection of elements
				//String accessType = access != null ? access.value() : null;
				if ( owner.getIdentifierProperty() != null ) {
					isPropertyAnnotated = owner.getIdentifierProperty().getPropertyAccessorName().equals( "property" );
				}
				else if ( owner.getIdentifierMapper() != null && owner.getIdentifierMapper().getPropertySpan() > 0 ) {
					Property prop = (Property) owner.getIdentifierMapper().getPropertyIterator().next();
					isPropertyAnnotated = prop.getPropertyAccessorName().equals( "property" );
				}
				else {
					throw new AssertionFailure( "Unable to guess collection property accessor name" );
				}

				PropertyData inferredData;
				if ( isMap() ) {
					//"value" is the JPA 2 prefix for map values (used to be "element")
					if ( isHibernateExtensionMapping() ) {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "element", elementClass );
					}
					else {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "value", elementClass );
					}
				}
				else {
					if ( isHibernateExtensionMapping() ) {
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "element", elementClass );
					}
					else {
						//"collection&&element" is not a valid property name => placeholder
						inferredData = new PropertyPreloadedData( AccessType.PROPERTY, "collection&&element", elementClass );
					}
				}
				//TODO be smart with isNullable
				Component component = AnnotationBinder.fillComponent(
						holder, inferredData, isPropertyAnnotated ? AccessType.PROPERTY : AccessType.FIELD, true,
						entityBinder, false, false,
						true, mappings, inheritanceStatePerClass
				);

				collValue.setElement( component );

				if ( StringHelper.isNotEmpty( hqlOrderBy ) ) {
					String path = collValue.getOwnerEntityName() + "." + joinColumns[0].getPropertyName();
					String orderBy = buildOrderByClauseFromHql( hqlOrderBy, component, path );
					if ( orderBy != null ) {
						collValue.setOrderBy( orderBy );
					}
				}
			}
			else {
				SimpleValueBinder elementBinder = new SimpleValueBinder();
				elementBinder.setMappings( mappings );
				elementBinder.setReturnedClassName( collType.getName() );
				if ( elementColumns == null || elementColumns.length == 0 ) {
					elementColumns = new Ejb3Column[1];
					Ejb3Column column = new Ejb3Column();
					column.setImplicit( false );
					//not following the spec but more clean
					column.setNullable( true );
					column.setLength( Ejb3Column.DEFAULT_COLUMN_LENGTH );
					column.setLogicalColumnName( Collection.DEFAULT_ELEMENT_COLUMN_NAME );
					//TODO create an EMPTY_JOINS collection
					column.setJoins( new HashMap<String, Join>() );
					column.setMappings( mappings );
					column.bind();
					elementColumns[0] = column;
				}
				//override the table
				for (Ejb3Column column : elementColumns) {
					column.setTable( collValue.getCollectionTable() );
				}
				elementBinder.setColumns( elementColumns );
				elementBinder.setType( property, elementClass );
				collValue.setElement( elementBinder.make() );
			}
		}

		checkFilterConditions( collValue );

		//FIXME: do optional = false
		if ( isCollectionOfEntities ) {
			bindManytoManyInverseFk( collectionEntity, inverseJoinColumns, element, unique, mappings );
		}

	}

	private static void checkFilterConditions(Collection collValue) {
		//for now it can't happen, but sometime soon...
		if ( ( collValue.getFilterMap().size() != 0 || StringHelper.isNotEmpty( collValue.getWhere() ) ) &&
				collValue.getFetchMode() == FetchMode.JOIN &&
				!( collValue.getElement() instanceof SimpleValue ) && //SimpleValue (CollectionOfElements) are always SELECT but it does not matter
				collValue.getElement().getFetchMode() != FetchMode.JOIN ) {
			throw new MappingException(
					"@ManyToMany or @CollectionOfElements defining filter or where without join fetching "
							+ "not valid within collection using join fetching[" + collValue.getRole() + "]"
			);
		}
	}

	private static void bindCollectionSecondPass(
			Collection collValue, PersistentClass collectionEntity, Ejb3JoinColumn[] joinColumns,
			boolean cascadeDeleteEnabled, XProperty property,
			ExtendedMappings mappings
	) {
		BinderHelper.createSyntheticPropertyReference(
				joinColumns, collValue.getOwner(), collectionEntity, collValue, false, mappings
		);
		SimpleValue key = buildCollectionKey( collValue, joinColumns, cascadeDeleteEnabled, property, mappings );
		if ( property.isAnnotationPresent( ElementCollection.class ) && joinColumns.length > 0 ) {
			joinColumns[0].setJPA2ElementCollection( true );
		}
		TableBinder.bindFk( collValue.getOwner(), collectionEntity, joinColumns, key, false, mappings );
	}

	public void setCascadeDeleteEnabled(boolean onDeleteCascade) {
		this.cascadeDeleteEnabled = onDeleteCascade;
	}

	private String safeCollectionRole() {
		if ( propertyHolder != null ) {
			return propertyHolder.getEntityName() + "." + propertyName;
		}
		else {
			return "";
		}
	}


	/**
	 * bind the inverse FK of a ManyToMany
	 * If we are in a mappedBy case, read the columns from the associated
	 * colletion element
	 * Otherwise delegates to the usual algorithm
	 */
	public static void bindManytoManyInverseFk(
			PersistentClass referencedEntity, Ejb3JoinColumn[] columns, SimpleValue value, boolean unique,
			ExtendedMappings mappings
	) {
		final String mappedBy = columns[0].getMappedBy();
		if ( StringHelper.isNotEmpty( mappedBy ) ) {
			final Property property = referencedEntity.getRecursiveProperty( mappedBy );
			Iterator mappedByColumns;
			if ( property.getValue() instanceof Collection ) {
				mappedByColumns = ( (Collection) property.getValue() ).getKey().getColumnIterator();
			}
			else {
				//find the appropriate reference key, can be in a join
				Iterator joinsIt = referencedEntity.getJoinIterator();
				KeyValue key = null;
				while ( joinsIt.hasNext() ) {
					Join join = (Join) joinsIt.next();
					if ( join.containsProperty( property ) ) {
						key = join.getKey();
						break;
					}
				}
				if ( key == null ) key = property.getPersistentClass().getIdentifier();
				mappedByColumns = key.getColumnIterator();
			}
			while ( mappedByColumns.hasNext() ) {
				Column column = (Column) mappedByColumns.next();
				columns[0].linkValueUsingAColumnCopy( column, value );
			}
			String referencedPropertyName =
					mappings.getPropertyReferencedAssociation(
							"inverse__" + referencedEntity.getEntityName(), mappedBy
					);
			if ( referencedPropertyName != null ) {
				//TODO always a many to one?
				( (ManyToOne) value ).setReferencedPropertyName( referencedPropertyName );
				mappings.addUniquePropertyReference( referencedEntity.getEntityName(), referencedPropertyName );
			}
			value.createForeignKey();
		}
		else {
			BinderHelper.createSyntheticPropertyReference( columns, referencedEntity, null, value, true, mappings );
			TableBinder.bindFk( referencedEntity, null, columns, value, unique, mappings );
		}
	}

	public void setFkJoinColumns(Ejb3JoinColumn[] ejb3JoinColumns) {
		this.fkJoinColumns = ejb3JoinColumns;
	}

	public void setExplicitAssociationTable(boolean explicitAssocTable) {
		this.isExplicitAssociationTable = explicitAssocTable;
	}

	public void setElementColumns(Ejb3Column[] elementColumns) {
		this.elementColumns = elementColumns;
	}

	public void setEmbedded(boolean annotationPresent) {
		this.isEmbedded = annotationPresent;
	}

	public void setProperty(XProperty property) {
		this.property = property;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

	public void setMapKeyColumns(Ejb3Column[] mapKeyColumns) {
		this.mapKeyColumns = mapKeyColumns;
	}

	public void setMapKeyManyToManyColumns(Ejb3JoinColumn[] mapJoinColumns) {
		this.mapKeyManyToManyColumns = mapJoinColumns;
	}

	public void setLocalGenerators(HashMap<String, IdGenerator> localGenerators) {
		this.localGenerators = localGenerators;
	}
}