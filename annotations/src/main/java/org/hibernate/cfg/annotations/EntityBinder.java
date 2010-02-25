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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForceDiscriminator;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.annotations.Tables;
import org.hibernate.annotations.Tuplizer;
import org.hibernate.annotations.Tuplizers;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.ObjectNameSource;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.engine.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.Versioning;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.Value;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stateful holder and processor for binding Entity information
 *
 * @author Emmanuel Bernard
 */
public class EntityBinder {
	private String name;
	private XClass annotatedClass;
	private PersistentClass persistentClass;
	private ExtendedMappings mappings;
	private Logger log = LoggerFactory.getLogger( EntityBinder.class );
	private String discriminatorValue = "";
	private boolean dynamicInsert;
	private boolean dynamicUpdate;
	private boolean explicitHibernateEntityAnnotation;
	private OptimisticLockType optimisticLockType;
	private PolymorphismType polymorphismType;
	private boolean selectBeforeUpdate;
	private int batchSize;
	private boolean lazy;
	private XClass proxyClass;
	private String where;
	private java.util.Map<String, Join> secondaryTables = new HashMap<String, Join>();
	private java.util.Map<String, Object> secondaryTableJoins = new HashMap<String, Object>();
	private String cacheConcurrentStrategy;
	private String cacheRegion;
	private java.util.Map<String, String> filters = new HashMap<String, String>();
	private InheritanceState inheritanceState;
	private boolean ignoreIdAnnotations;
	private boolean cacheLazyProperty;
	private AccessType propertyAccessType = AccessType.DEFAULT;
	private boolean wrapIdsInEmbeddedComponents;
	private String subselect;
	
	
	public boolean wrapIdsInEmbeddedComponents() {
		return wrapIdsInEmbeddedComponents;
	}

	/**
	 * Use as a fake one for Collection of elements
	 */
	public EntityBinder() {
	}

	public EntityBinder(
			Entity ejb3Ann, org.hibernate.annotations.Entity hibAnn,
			XClass annotatedClass, PersistentClass persistentClass,
			ExtendedMappings mappings
	) {
		this.mappings = mappings;
		this.persistentClass = persistentClass;
		this.annotatedClass = annotatedClass;
		bindEjb3Annotation( ejb3Ann );
		bindHibernateAnnotation( hibAnn );
	}

	private void bindHibernateAnnotation(org.hibernate.annotations.Entity hibAnn) {
		if ( hibAnn != null ) {
			dynamicInsert = hibAnn.dynamicInsert();
			dynamicUpdate = hibAnn.dynamicUpdate();
			optimisticLockType = hibAnn.optimisticLock();
			selectBeforeUpdate = hibAnn.selectBeforeUpdate();
			polymorphismType = hibAnn.polymorphism();
			explicitHibernateEntityAnnotation = true;
			//persister handled in bind
		}
		else {
			//default values when the annotation is not there
			dynamicInsert = false;
			dynamicUpdate = false;
			optimisticLockType = OptimisticLockType.VERSION;
			polymorphismType = PolymorphismType.IMPLICIT;
			selectBeforeUpdate = false;
		}
	}

	private void bindEjb3Annotation(Entity ejb3Ann) {
		if ( ejb3Ann == null ) throw new AssertionFailure( "@Entity should always be not null" );
		if ( BinderHelper.isDefault( ejb3Ann.name() ) ) {
			name = StringHelper.unqualify( annotatedClass.getName() );
		}
		else {
			name = ejb3Ann.name();
		}
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public void bindEntity() {
		persistentClass.setAbstract( annotatedClass.isAbstract() );
		persistentClass.setClassName( annotatedClass.getName() );
		persistentClass.setNodeName( name );
		//persistentClass.setDynamic(false); //no longer needed with the Entity name refactoring?
		persistentClass.setEntityName( annotatedClass.getName() );
		bindDiscriminatorValue();

		persistentClass.setLazy( lazy );
		if ( proxyClass != null ) {
			persistentClass.setProxyInterfaceName( proxyClass.getName() );
		}
		persistentClass.setDynamicInsert( dynamicInsert );
		persistentClass.setDynamicUpdate( dynamicUpdate );

		if ( persistentClass instanceof RootClass ) {
			RootClass rootClass = (RootClass) persistentClass;
			boolean mutable = true;
			//priority on @Immutable, then @Entity.mutable()
			if ( annotatedClass.isAnnotationPresent( Immutable.class ) ) {
				mutable = false;
			}
			else {
				org.hibernate.annotations.Entity entityAnn =
						annotatedClass.getAnnotation( org.hibernate.annotations.Entity.class );
				if ( entityAnn != null ) {
					mutable = entityAnn.mutable();
				}
			}
			rootClass.setMutable( mutable );
			rootClass.setExplicitPolymorphism( isExplicitPolymorphism( polymorphismType ) );
			if ( StringHelper.isNotEmpty( where ) ) rootClass.setWhere( where );
			if ( cacheConcurrentStrategy != null ) {
				rootClass.setCacheConcurrencyStrategy( cacheConcurrentStrategy );
				rootClass.setCacheRegionName( cacheRegion );
				rootClass.setLazyPropertiesCacheable( cacheLazyProperty );
			}
			rootClass.setForceDiscriminator( annotatedClass.isAnnotationPresent( ForceDiscriminator.class ) );
		}
		else {
			if ( explicitHibernateEntityAnnotation ) {
				log.warn( "@org.hibernate.annotations.Entity used on a non root entity: ignored for {}",
						annotatedClass.getName() );
			}
			if ( annotatedClass.isAnnotationPresent( Immutable.class ) ) {
				log.warn( "@Immutable used on a non root entity: ignored for {}",
						annotatedClass.getName() );
			}
		}
		persistentClass.setOptimisticLockMode( getVersioning( optimisticLockType ) );
		persistentClass.setSelectBeforeUpdate( selectBeforeUpdate );

		//set persister if needed
		//@Persister has precedence over @Entity.persister
		Persister persisterAnn = annotatedClass.getAnnotation( Persister.class );
		Class persister = null;
		if ( persisterAnn != null ) {
			persister = persisterAnn.impl();
		}
		else {
			org.hibernate.annotations.Entity entityAnn = annotatedClass.getAnnotation( org.hibernate.annotations.Entity.class );
			if ( entityAnn != null && !BinderHelper.isDefault( entityAnn.persister() ) ) {
				try {
					persister = ReflectHelper.classForName( entityAnn.persister() );
				}
				catch (ClassNotFoundException cnfe) {
					throw new AnnotationException( "Could not find persister class: " + persister );
				}
			}
		}
		if ( persister != null ) persistentClass.setEntityPersisterClass( persister );

		persistentClass.setBatchSize( batchSize );

		//SQL overriding
		SQLInsert sqlInsert = annotatedClass.getAnnotation( SQLInsert.class );
		SQLUpdate sqlUpdate = annotatedClass.getAnnotation( SQLUpdate.class );
		SQLDelete sqlDelete = annotatedClass.getAnnotation( SQLDelete.class );
		SQLDeleteAll sqlDeleteAll = annotatedClass.getAnnotation( SQLDeleteAll.class );
		Loader loader = annotatedClass.getAnnotation( Loader.class );
		
		if ( sqlInsert != null ) {
			persistentClass.setCustomSQLInsert( sqlInsert.sql().trim(), sqlInsert.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlInsert.check().toString().toLowerCase() )
			);

		}
		if ( sqlUpdate != null ) {
			persistentClass.setCustomSQLUpdate( sqlUpdate.sql(), sqlUpdate.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlUpdate.check().toString().toLowerCase() )
			);
		}
		if ( sqlDelete != null ) {
			persistentClass.setCustomSQLDelete( sqlDelete.sql(), sqlDelete.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlDelete.check().toString().toLowerCase() )
			);
		}
		if ( sqlDeleteAll != null ) {
			persistentClass.setCustomSQLDelete( sqlDeleteAll.sql(), sqlDeleteAll.callable(),
					ExecuteUpdateResultCheckStyle.parse( sqlDeleteAll.check().toString().toLowerCase() )
			);
		}
		if ( loader != null ) {
			persistentClass.setLoaderName( loader.namedQuery() );
		}

		if ( annotatedClass.isAnnotationPresent( Synchronize.class )) {
			Synchronize synchronizedWith = annotatedClass.getAnnotation(Synchronize.class);
		
			String [] tables = synchronizedWith.value();
			for (String table : tables) {
				persistentClass.addSynchronizedTable(table);
			}
		}
				
		if ( annotatedClass.isAnnotationPresent(Subselect.class )) {
			Subselect subselect = annotatedClass.getAnnotation(Subselect.class);
			this.subselect = subselect.value();
		}	
				
		//tuplizers
		if ( annotatedClass.isAnnotationPresent( Tuplizers.class ) ) {
			for (Tuplizer tuplizer : annotatedClass.getAnnotation( Tuplizers.class ).value()) {
				EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
				persistentClass.addTuplizer( mode, tuplizer.impl().getName() );
			}
		}
		if ( annotatedClass.isAnnotationPresent( Tuplizer.class ) ) {
			Tuplizer tuplizer = annotatedClass.getAnnotation( Tuplizer.class );
			EntityMode mode = EntityMode.parse( tuplizer.entityMode() );
			persistentClass.addTuplizer( mode, tuplizer.impl().getName() );
		}

		if ( !inheritanceState.hasParents() ) {
			for ( Map.Entry<String, String> filter : filters.entrySet() ) {
				String filterName = filter.getKey();
				String cond = filter.getValue();
				if ( BinderHelper.isDefault( cond ) ) {
					FilterDefinition definition = mappings.getFilterDefinition( filterName );
					cond = definition == null ? null : definition.getDefaultFilterCondition();
					if ( StringHelper.isEmpty( cond ) ) {
						throw new AnnotationException(
								"no filter condition found for filter " + filterName + " in " + this.name
						);
					}
				}
				persistentClass.addFilter( filterName, cond );
			}
		}
		else {
			if ( filters.size() > 0 ) {
				log.warn( "@Filter not allowed on subclasses (ignored): {}", persistentClass.getEntityName() );
			}
		}
		log.debug( "Import with entity name {}", name );
		try {
			mappings.addImport( persistentClass.getEntityName(), name );
			String entityName = persistentClass.getEntityName();
			if ( !entityName.equals( name ) ) {
				mappings.addImport( entityName, entityName );
			}
		}
		catch (MappingException me) {
			throw new AnnotationException( "Use of the same entity name twice: " + name, me );
		}
	}

	public void bindDiscriminatorValue() {
		if ( StringHelper.isEmpty( discriminatorValue ) ) {
			Value discriminator = persistentClass.getDiscriminator();
			if ( discriminator == null ) {
				persistentClass.setDiscriminatorValue( name );
			}
			else if ( "character".equals( discriminator.getType().getName() ) ) {
				throw new AnnotationException(
						"Using default @DiscriminatorValue for a discriminator of type CHAR is not safe"
				);
			}
			else if ( "integer".equals( discriminator.getType().getName() ) ) {
				persistentClass.setDiscriminatorValue( String.valueOf( name.hashCode() ) );
			}
			else {
				persistentClass.setDiscriminatorValue( name ); //Spec compliant
			}
		}
		else {
			//persistentClass.getDiscriminator()
			persistentClass.setDiscriminatorValue( discriminatorValue );
		}
	}

	int getVersioning(OptimisticLockType type) {
		switch ( type ) {
			case VERSION:
				return Versioning.OPTIMISTIC_LOCK_VERSION;
			case NONE:
				return Versioning.OPTIMISTIC_LOCK_NONE;
			case DIRTY:
				return Versioning.OPTIMISTIC_LOCK_DIRTY;
			case ALL:
				return Versioning.OPTIMISTIC_LOCK_ALL;
			default:
				throw new AssertionFailure( "optimistic locking not supported: " + type );
		}
	}

	private boolean isExplicitPolymorphism(PolymorphismType type) {
		switch ( type ) {
			case IMPLICIT:
				return false;
			case EXPLICIT:
				return true;
			default:
				throw new AssertionFailure( "Unknown polymorphism type: " + type );
		}
	}

	public void setBatchSize(BatchSize sizeAnn) {
		if ( sizeAnn != null ) {
			batchSize = sizeAnn.size();
		}
		else {
			batchSize = -1;
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void setProxy(Proxy proxy) {
		if ( proxy != null ) {
			lazy = proxy.lazy();
			if ( !lazy ) {
				proxyClass = null;
			}
			else {
				if ( AnnotationBinder.isDefault(
						mappings.getReflectionManager().toXClass( proxy.proxyClass() ), mappings
				) ) {
					proxyClass = annotatedClass;
				}
				else {
					proxyClass = mappings.getReflectionManager().toXClass( proxy.proxyClass() );
				}
			}
		}
		else {
			lazy = true; //needed to allow association lazy loading.
			proxyClass = annotatedClass;
		}
	}

	public void setWhere(Where whereAnn) {
		if ( whereAnn != null ) {
			where = whereAnn.clause();
		}
	}

	public void setWrapIdsInEmbeddedComponents(boolean wrapIdsInEmbeddedComponents) {
		this.wrapIdsInEmbeddedComponents = wrapIdsInEmbeddedComponents;
	}


	private static class EntityTableObjectNameSource implements ObjectNameSource {
		private final String explicitName;
		private final String logicalName;

		private EntityTableObjectNameSource(String explicitName, String entityName) {
			this.explicitName = explicitName;
			this.logicalName = StringHelper.isNotEmpty( explicitName )
					? explicitName
					: StringHelper.unqualify( entityName );
		}

		public String getExplicitName() {
			return explicitName;
		}

		public String getLogicalName() {
			return logicalName;
		}
	}

	private static class EntityTableNamingStrategyHelper implements ObjectNameNormalizer.NamingStrategyHelper {
		private final String entityName;

		private EntityTableNamingStrategyHelper(String entityName) {
			this.entityName = entityName;
		}

		public String determineImplicitName(NamingStrategy strategy) {
			return strategy.classToTableName( entityName );
		}

		public String handleExplicitName(NamingStrategy strategy, String name) {
			return strategy.tableName( name );
		}
	}

	public void bindTable(
			String schema, String catalog,
			String tableName, List<UniqueConstraintHolder> uniqueConstraints,
			String constraints, Table denormalizedSuperclassTable) {
		EntityTableObjectNameSource tableNameContext = new EntityTableObjectNameSource( tableName, name );
		EntityTableNamingStrategyHelper namingStrategyHelper = new EntityTableNamingStrategyHelper( name );
		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				tableNameContext,
				namingStrategyHelper,
				persistentClass.isAbstract(),
				uniqueConstraints,
				constraints,
				denormalizedSuperclassTable,
				mappings,
				this.subselect
		);

		if ( persistentClass instanceof TableOwner ) {
			log.info( "Bind entity {} on table {}", persistentClass.getEntityName(), table.getName() );
			( (TableOwner) persistentClass ).setTable( table );
		}
		else {
			throw new AssertionFailure( "binding a table for a subclass" );
		}
	}

	public void finalSecondaryTableBinding(PropertyHolder propertyHolder) {
		/*
		 * Those operations has to be done after the id definition of the persistence class.
		 * ie after the properties parsing
		 */
		Iterator joins = secondaryTables.values().iterator();
		Iterator joinColumns = secondaryTableJoins.values().iterator();

		while ( joins.hasNext() ) {
			Object uncastedColumn = joinColumns.next();
			Join join = (Join) joins.next();
			createPrimaryColumnsToSecondaryTable( uncastedColumn, propertyHolder, join );
		}
		mappings.addJoins( persistentClass, secondaryTables );
	}

	private void createPrimaryColumnsToSecondaryTable(Object uncastedColumn, PropertyHolder propertyHolder, Join join) {
		Ejb3JoinColumn[] ejb3JoinColumns;
		PrimaryKeyJoinColumn[] pkColumnsAnn = null;
		JoinColumn[] joinColumnsAnn = null;
		if ( uncastedColumn instanceof PrimaryKeyJoinColumn[] ) {
			pkColumnsAnn = (PrimaryKeyJoinColumn[]) uncastedColumn;
		}
		if ( uncastedColumn instanceof JoinColumn[] ) {
			joinColumnsAnn = (JoinColumn[]) uncastedColumn;
		}
		if ( pkColumnsAnn == null && joinColumnsAnn == null ) {
			ejb3JoinColumns = new Ejb3JoinColumn[1];
			ejb3JoinColumns[0] = Ejb3JoinColumn.buildJoinColumn(
					null,
					null,
					persistentClass.getIdentifier(),
					secondaryTables,
					propertyHolder, mappings
			);
		}
		else {
			int nbrOfJoinColumns = pkColumnsAnn != null ?
					pkColumnsAnn.length :
					joinColumnsAnn.length;
			if ( nbrOfJoinColumns == 0 ) {
				ejb3JoinColumns = new Ejb3JoinColumn[1];
				ejb3JoinColumns[0] = Ejb3JoinColumn.buildJoinColumn(
						null,
						null,
						persistentClass.getIdentifier(),
						secondaryTables,
						propertyHolder, mappings
				);
			}
			else {
				ejb3JoinColumns = new Ejb3JoinColumn[nbrOfJoinColumns];
				if ( pkColumnsAnn != null ) {
					for (int colIndex = 0; colIndex < nbrOfJoinColumns; colIndex++) {
						ejb3JoinColumns[colIndex] = Ejb3JoinColumn.buildJoinColumn(
								pkColumnsAnn[colIndex],
								null,
								persistentClass.getIdentifier(),
								secondaryTables,
								propertyHolder, mappings
						);
					}
				}
				else {
					for (int colIndex = 0; colIndex < nbrOfJoinColumns; colIndex++) {
						ejb3JoinColumns[colIndex] = Ejb3JoinColumn.buildJoinColumn(
								null,
								joinColumnsAnn[colIndex],
								persistentClass.getIdentifier(),
								secondaryTables,
								propertyHolder, mappings
						);
					}
				}
			}
		}

		for (Ejb3JoinColumn joinColumn : ejb3JoinColumns) {
			joinColumn.forceNotNull();
		}
		bindJoinToPersistentClass( join, ejb3JoinColumns );
	}

	private void bindJoinToPersistentClass(
			Join join, Ejb3JoinColumn[] ejb3JoinColumns
	) {
		SimpleValue key = new DependantValue( join.getTable(), persistentClass.getIdentifier() );
		join.setKey( key );
		setFKNameIfDefined( join );
		key.setCascadeDeleteEnabled( false );
		TableBinder.bindFk( persistentClass, null, ejb3JoinColumns, key, false, mappings );
		join.createPrimaryKey();
		join.createForeignKey();
		persistentClass.addJoin( join );
	}

	private void setFKNameIfDefined(Join join) {
		org.hibernate.annotations.Table matchingTable = findMatchingComplimentTableAnnotation( join );
		if ( matchingTable != null && !BinderHelper.isDefault( matchingTable.foreignKey().name() ) ) {
			( (SimpleValue) join.getKey() ).setForeignKeyName( matchingTable.foreignKey().name() );
		}
	}

	private org.hibernate.annotations.Table findMatchingComplimentTableAnnotation(Join join) {
		String tableName = join.getTable().getQuotedName();
		org.hibernate.annotations.Table table = annotatedClass.getAnnotation( org.hibernate.annotations.Table.class );
		org.hibernate.annotations.Table matchingTable = null;
		if ( table != null && tableName.equals( table.appliesTo() ) ) {
			matchingTable = table;
		}
		else {
			Tables tables = annotatedClass.getAnnotation( Tables.class );
			if ( tables != null ) {
				for (org.hibernate.annotations.Table current : tables.value()) {
					if ( tableName.equals( current.appliesTo() ) ) {
						matchingTable = current;
						break;
					}
				}
			}
		}
		return matchingTable;
	}

	public void firstLevelSecondaryTablesBinding(
			SecondaryTable secTable, SecondaryTables secTables
	) {
		if ( secTables != null ) {
			//loop through it
			for (SecondaryTable tab : secTables.value()) {
				addJoin( tab, null, null, false );
			}
		}
		else {
			if ( secTable != null ) addJoin( secTable, null, null, false );
		}
	}

	//Used for @*ToMany @JoinTable
	public Join addJoin(JoinTable joinTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		return addJoin( null, joinTable, holder, noDelayInPkColumnCreation );
	}

	private static class SecondaryTableNameSource implements ObjectNameSource {
		// always has an explicit name
		private final String explicitName;

		private SecondaryTableNameSource(String explicitName) {
			this.explicitName = explicitName;
		}

		public String getExplicitName() {
			return explicitName;
		}

		public String getLogicalName() {
			return explicitName;
		}
	}

	private static class SecondaryTableNamingStrategyHelper implements ObjectNameNormalizer.NamingStrategyHelper {
		public String determineImplicitName(NamingStrategy strategy) {
			// todo : throw an error?
			return null;
		}

		public String handleExplicitName(NamingStrategy strategy, String name) {
			return strategy.tableName( name );
		}
	}

	private static SecondaryTableNamingStrategyHelper SEC_TBL_NS_HELPER = new SecondaryTableNamingStrategyHelper();

	private Join addJoin(
			SecondaryTable secondaryTable,
			JoinTable joinTable,
			PropertyHolder propertyHolder,
			boolean noDelayInPkColumnCreation) {
		// A non null propertyHolder means than we process the Pk creation without delay
		Join join = new Join();
		join.setPersistentClass( persistentClass );

		final String schema;
		final String catalog;
		final SecondaryTableNameSource secondaryTableNameContext;
		final Object joinColumns;
		final List<UniqueConstraintHolder> uniqueConstraintHolders;

		if ( secondaryTable != null ) {
			schema = secondaryTable.schema();
			catalog = secondaryTable.catalog();
			secondaryTableNameContext = new SecondaryTableNameSource( secondaryTable.name() );
			joinColumns = secondaryTable.pkJoinColumns();
			uniqueConstraintHolders = TableBinder.buildUniqueConstraintHolders( secondaryTable.uniqueConstraints() );
		}
		else if ( joinTable != null ) {
			schema = joinTable.schema();
			catalog = joinTable.catalog();
			secondaryTableNameContext = new SecondaryTableNameSource( joinTable.name() );
			joinColumns = joinTable.joinColumns();
			uniqueConstraintHolders = TableBinder.buildUniqueConstraintHolders( joinTable.uniqueConstraints() );
		}
		else {
			throw new AssertionFailure( "Both JoinTable and SecondaryTable are null" );
		}

		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				secondaryTableNameContext,
				SEC_TBL_NS_HELPER,
				false,
				uniqueConstraintHolders,
				null,
				null,
				mappings,
				null
		);

		//no check constraints available on joins
		join.setTable( table );

		//somehow keep joins() for later.
		//Has to do the work later because it needs persistentClass id!
		log.info(
				"Adding secondary table to entity {} -> {}", persistentClass.getEntityName(), join.getTable().getName()
		);
		org.hibernate.annotations.Table matchingTable = findMatchingComplimentTableAnnotation( join );
		if ( matchingTable != null ) {
			join.setSequentialSelect( FetchMode.JOIN != matchingTable.fetch() );
			join.setInverse( matchingTable.inverse() );
			join.setOptional( matchingTable.optional() );
			if ( !BinderHelper.isDefault( matchingTable.sqlInsert().sql() ) ) {
				join.setCustomSQLInsert( matchingTable.sqlInsert().sql().trim(),
						matchingTable.sqlInsert().callable(),
						ExecuteUpdateResultCheckStyle.parse( matchingTable.sqlInsert().check().toString().toLowerCase() )
				);
			}
			if ( !BinderHelper.isDefault( matchingTable.sqlUpdate().sql() ) ) {
				join.setCustomSQLUpdate( matchingTable.sqlUpdate().sql().trim(),
						matchingTable.sqlUpdate().callable(),
						ExecuteUpdateResultCheckStyle.parse( matchingTable.sqlUpdate().check().toString().toLowerCase() )
				);
			}
			if ( !BinderHelper.isDefault( matchingTable.sqlDelete().sql() ) ) {
				join.setCustomSQLDelete( matchingTable.sqlDelete().sql().trim(),
						matchingTable.sqlDelete().callable(),
						ExecuteUpdateResultCheckStyle.parse( matchingTable.sqlDelete().check().toString().toLowerCase() )
				);
			}
		}
		else {
			//default
			join.setSequentialSelect( false );
			join.setInverse( false );
			join.setOptional( true ); //perhaps not quite per-spec, but a Good Thing anyway
		}

		if ( noDelayInPkColumnCreation ) {
			createPrimaryColumnsToSecondaryTable( joinColumns, propertyHolder, join );
		}
		else {
			secondaryTables.put( table.getQuotedName(), join );
			secondaryTableJoins.put( table.getQuotedName(), joinColumns );
		}
		return join;
	}

	public java.util.Map<String, Join> getSecondaryTables() {
		return secondaryTables;
	}

	public void setCache(Cache cacheAnn) {
		if ( cacheAnn != null ) {
			cacheRegion = BinderHelper.isDefault( cacheAnn.region() ) ?
					null :
					cacheAnn.region();
			cacheConcurrentStrategy = getCacheConcurrencyStrategy( cacheAnn.usage() );
			if ( "all".equalsIgnoreCase( cacheAnn.include() ) ) {
				cacheLazyProperty = true;
			}
			else if ( "non-lazy".equalsIgnoreCase( cacheAnn.include() ) ) {
				cacheLazyProperty = false;
			}
			else {
				throw new AnnotationException( "Unknown lazy property annotations: " + cacheAnn.include() );
			}
		}
		else {
			cacheConcurrentStrategy = null;
			cacheRegion = null;
			cacheLazyProperty = true;
		}
	}

	public static String getCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		org.hibernate.cache.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getName();
	}

	public void addFilter(String name, String condition) {
		filters.put( name, condition );
	}

	public void setInheritanceState(InheritanceState inheritanceState) {
		this.inheritanceState = inheritanceState;
	}

	public boolean isIgnoreIdAnnotations() {
		return ignoreIdAnnotations;
	}

	public void setIgnoreIdAnnotations(boolean ignoreIdAnnotations) {
		this.ignoreIdAnnotations = ignoreIdAnnotations;
	}

	public void processComplementaryTableDefinitions(org.hibernate.annotations.Table table) {
		//comment and index are processed here
		if ( table == null ) return;
		String appliedTable = table.appliesTo();
		Iterator tables = persistentClass.getTableClosureIterator();
		Table hibTable = null;
		while ( tables.hasNext() ) {
			Table pcTable = (Table) tables.next();
			if ( pcTable.getQuotedName().equals( appliedTable ) ) {
				//we are in the correct table to find columns
				hibTable = pcTable;
				break;
			}
			hibTable = null;
		}
		if ( hibTable == null ) {
			//maybe a join/secondary table
			for ( Join join : secondaryTables.values() ) {
				if ( join.getTable().getQuotedName().equals( appliedTable ) ) {
					hibTable = join.getTable();
					break;
				}
			}
		}
		if ( hibTable == null ) {
			throw new AnnotationException(
					"@org.hibernate.annotations.Table references an unknown table: " + appliedTable
			);
		}
		if ( !BinderHelper.isDefault( table.comment() ) ) hibTable.setComment( table.comment() );
		TableBinder.addIndexes( hibTable, table.indexes(), mappings );
	}

	public void processComplementaryTableDefinitions(Tables tables) {
		if ( tables == null ) return;
		for (org.hibernate.annotations.Table table : tables.value()) {
			processComplementaryTableDefinitions( table );
		}
	}

	public AccessType getPropertyAccessType() {
		return propertyAccessType;
	}

	public void setPropertyAccessType(AccessType propertyAccessor) {
		this.propertyAccessType = getExplicitAccessType( annotatedClass );
		// only set the access type if there is no explicit access type for this class
		if( this.propertyAccessType == null ) {
			this.propertyAccessType = propertyAccessor;
		}
	}

	public AccessType getPropertyAccessor(XAnnotatedElement element) {
		AccessType accessType = getExplicitAccessType( element );
		if ( accessType == null ) {
		   accessType = propertyAccessType;
		}
		return accessType;
	}

	public AccessType getExplicitAccessType(XAnnotatedElement element) {
		AccessType accessType = null;

		AccessType hibernateAccessType = null;
		AccessType jpaAccessType = null;

		org.hibernate.annotations.AccessType accessTypeAnnotation = element.getAnnotation( org.hibernate.annotations.AccessType.class );
		if ( accessTypeAnnotation != null ) {
			hibernateAccessType = AccessType.getAccessStrategy( accessTypeAnnotation.value() );
		}

		Access access = element.getAnnotation( Access.class );
		if ( access != null ) {
			jpaAccessType = AccessType.getAccessStrategy( access.value() );
		}

		if ( hibernateAccessType != null && jpaAccessType != null && hibernateAccessType != jpaAccessType ) {
			throw new MappingException(
					"Found @Access and @AccessType with conflicting values on a property in class " + annotatedClass.toString()
			);
		}

		if ( hibernateAccessType != null ) {
			accessType = hibernateAccessType;
		}
		else if ( jpaAccessType != null ) {
			accessType = jpaAccessType;
		}

		return accessType;
	}
}
