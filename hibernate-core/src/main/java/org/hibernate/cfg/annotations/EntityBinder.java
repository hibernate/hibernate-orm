/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.annotations;

import static org.hibernate.cfg.BinderHelper.toAliasEntityMap;
import static org.hibernate.cfg.BinderHelper.toAliasTableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.RowId;
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
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.cfg.ObjectNameSource;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.Value;
import org.jboss.logging.Logger;


/**
 * Stateful holder and processor for binding Entity information
 *
 * @author Emmanuel Bernard
 */
public class EntityBinder {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, EntityBinder.class.getName());
    private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";
	
	private String name;
	private XClass annotatedClass;
	private PersistentClass persistentClass;
	private Mappings mappings;
	private String discriminatorValue = "";
	private Boolean forceDiscriminator;
	private Boolean insertableDiscriminator;
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
	private String naturalIdCacheRegion;
	private List<Filter> filters = new ArrayList<Filter>();
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
			Entity ejb3Ann,
			XClass annotatedClass,
			PersistentClass persistentClass,
			Mappings mappings) {
		this.mappings = mappings;
		this.persistentClass = persistentClass;
		this.annotatedClass = annotatedClass;
		bindEjb3Annotation( ejb3Ann );
	}

	private void bindEjb3Annotation(Entity ejb3Ann) {
		if ( ejb3Ann == null ) throw new AssertionFailure( "@Entity should always be not null" );
		if ( BinderHelper.isEmptyAnnotationValue( ejb3Ann.name() ) ) {
			name = StringHelper.unqualify( annotatedClass.getName() );
		}
		else {
			name = ejb3Ann.name();
		}
	}

	public boolean isRootEntity() {
		// This is the best option I can think of here since PersistentClass is most likely not yet fully populated
		return persistentClass instanceof RootClass;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public void setForceDiscriminator(boolean forceDiscriminator) {
		this.forceDiscriminator = forceDiscriminator;
	}

	public void setInsertableDiscriminator(boolean insertableDiscriminator) {
		this.insertableDiscriminator = insertableDiscriminator;
	}

	public void bindEntity() {
		persistentClass.setAbstract( annotatedClass.isAbstract() );
		persistentClass.setClassName( annotatedClass.getName() );
		persistentClass.setNodeName( name );
		persistentClass.setJpaEntityName(name);
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
			rootClass.setMutable( mutable );
			rootClass.setExplicitPolymorphism( isExplicitPolymorphism( polymorphismType ) );
			if ( StringHelper.isNotEmpty( where ) ) rootClass.setWhere( where );
			if ( cacheConcurrentStrategy != null ) {
				rootClass.setCacheConcurrencyStrategy( cacheConcurrentStrategy );
				rootClass.setCacheRegionName( cacheRegion );
				rootClass.setLazyPropertiesCacheable( cacheLazyProperty );
			}
			rootClass.setNaturalIdCacheRegionName( naturalIdCacheRegion );
			boolean forceDiscriminatorInSelects = forceDiscriminator == null
					? mappings.forceDiscriminatorInSelectsByDefault()
					: forceDiscriminator;
			rootClass.setForceDiscriminator( forceDiscriminatorInSelects );
			if( insertableDiscriminator != null) {
				rootClass.setDiscriminatorInsertable( insertableDiscriminator );
			}
		}
		else {
            if (explicitHibernateEntityAnnotation) {
				LOG.entityAnnotationOnNonRoot(annotatedClass.getName());
			}
            if (annotatedClass.isAnnotationPresent(Immutable.class)) {
				LOG.immutableAnnotationOnNonRoot(annotatedClass.getName());
			}
		}
		persistentClass.setOptimisticLockStyle( getVersioning( optimisticLockType ) );
		persistentClass.setSelectBeforeUpdate( selectBeforeUpdate );

		//set persister if needed
		Persister persisterAnn = annotatedClass.getAnnotation( Persister.class );
		Class persister = null;
		if ( persisterAnn != null ) {
			persister = persisterAnn.impl();
		}
		if ( persister != null ) {
			persistentClass.setEntityPersisterClass( persister );
		}

		persistentClass.setBatchSize( batchSize );

		//SQL overriding
		SQLInsert sqlInsert = annotatedClass.getAnnotation( SQLInsert.class );
		SQLUpdate sqlUpdate = annotatedClass.getAnnotation( SQLUpdate.class );
		SQLDelete sqlDelete = annotatedClass.getAnnotation( SQLDelete.class );
		SQLDeleteAll sqlDeleteAll = annotatedClass.getAnnotation( SQLDeleteAll.class );
		Loader loader = annotatedClass.getAnnotation( Loader.class );

		if ( sqlInsert != null ) {
			persistentClass.setCustomSQLInsert( sqlInsert.sql().trim(), sqlInsert.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlInsert.check().toString().toLowerCase() )
			);

		}
		if ( sqlUpdate != null ) {
			persistentClass.setCustomSQLUpdate( sqlUpdate.sql(), sqlUpdate.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlUpdate.check().toString().toLowerCase() )
			);
		}
		if ( sqlDelete != null ) {
			persistentClass.setCustomSQLDelete( sqlDelete.sql(), sqlDelete.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlDelete.check().toString().toLowerCase() )
			);
		}
		if ( sqlDeleteAll != null ) {
			persistentClass.setCustomSQLDelete( sqlDeleteAll.sql(), sqlDeleteAll.callable(),
					ExecuteUpdateResultCheckStyle.fromExternalName( sqlDeleteAll.check().toString().toLowerCase() )
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
				persistentClass.addTuplizer( tuplizer.entityModeType(), tuplizer.impl().getName() );
			}
		}
		if ( annotatedClass.isAnnotationPresent( Tuplizer.class ) ) {
			Tuplizer tuplizer = annotatedClass.getAnnotation( Tuplizer.class );
			persistentClass.addTuplizer( tuplizer.entityModeType(), tuplizer.impl().getName() );
		}

		for ( Filter filter : filters ) {
			String filterName = filter.name();
			String cond = filter.condition();
			if ( BinderHelper.isEmptyAnnotationValue( cond ) ) {
				FilterDefinition definition = mappings.getFilterDefinition( filterName );
				cond = definition == null ? null : definition.getDefaultFilterCondition();
				if ( StringHelper.isEmpty( cond ) ) {
					throw new AnnotationException(
							"no filter condition found for filter " + filterName + " in " + this.name
					);
				}
			}
			persistentClass.addFilter(filterName, cond, filter.deduceAliasInjectionPoints(), 
					toAliasTableMap(filter.aliases()), toAliasEntityMap(filter.aliases()));
		}
		LOG.debugf( "Import with entity name %s", name );
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

		processNamedEntityGraphs();
	}

	private void processNamedEntityGraphs() {
		processNamedEntityGraph( annotatedClass.getAnnotation( NamedEntityGraph.class ) );
		final NamedEntityGraphs graphs = annotatedClass.getAnnotation( NamedEntityGraphs.class );
		if ( graphs != null ) {
			for ( NamedEntityGraph graph : graphs.value() ) {
				processNamedEntityGraph( graph );
			}
		}
	}

	private void processNamedEntityGraph(NamedEntityGraph annotation) {
		if ( annotation == null ) {
			return;
		}
		mappings.addNamedEntityGraphDefintion( new NamedEntityGraphDefinition( annotation, name, persistentClass.getEntityName() ) );
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

	OptimisticLockStyle getVersioning(OptimisticLockType type) {
		switch ( type ) {
			case VERSION:
				return OptimisticLockStyle.VERSION;
			case NONE:
				return OptimisticLockStyle.NONE;
			case DIRTY:
				return OptimisticLockStyle.DIRTY;
			case ALL:
				return OptimisticLockStyle.ALL;
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
			String schema,
			String catalog,
			String tableName,
			List<UniqueConstraintHolder> uniqueConstraints,
			String constraints,
			Table denormalizedSuperclassTable) {
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
		final RowId rowId = annotatedClass.getAnnotation( RowId.class );
		if ( rowId != null ) {
			table.setRowId( rowId.value() );
		}

		if ( persistentClass instanceof TableOwner ) {
			LOG.debugf( "Bind entity %s on table %s", persistentClass.getEntityName(), table.getName() );
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
		bindJoinToPersistentClass( join, ejb3JoinColumns, mappings );
	}

	private void bindJoinToPersistentClass(Join join, Ejb3JoinColumn[] ejb3JoinColumns, Mappings mappings) {
		SimpleValue key = new DependantValue( mappings, join.getTable(), persistentClass.getIdentifier() );
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
		if ( matchingTable != null && !BinderHelper.isEmptyAnnotationValue( matchingTable.foreignKey().name() ) ) {
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

		if ( secondaryTable != null ) {
			TableBinder.addIndexes( table, secondaryTable.indexes(), mappings );
		}

			//no check constraints available on joins
		join.setTable( table );

		//somehow keep joins() for later.
		//Has to do the work later because it needs persistentClass id!
		LOG.debugf( "Adding secondary table to entity %s -> %s", persistentClass.getEntityName(), join.getTable().getName() );
		org.hibernate.annotations.Table matchingTable = findMatchingComplimentTableAnnotation( join );
		if ( matchingTable != null ) {
			join.setSequentialSelect( FetchMode.JOIN != matchingTable.fetch() );
			join.setInverse( matchingTable.inverse() );
			join.setOptional( matchingTable.optional() );
			if ( !BinderHelper.isEmptyAnnotationValue( matchingTable.sqlInsert().sql() ) ) {
				join.setCustomSQLInsert( matchingTable.sqlInsert().sql().trim(),
						matchingTable.sqlInsert().callable(),
						ExecuteUpdateResultCheckStyle.fromExternalName(
								matchingTable.sqlInsert().check().toString().toLowerCase()
						)
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( matchingTable.sqlUpdate().sql() ) ) {
				join.setCustomSQLUpdate( matchingTable.sqlUpdate().sql().trim(),
						matchingTable.sqlUpdate().callable(),
						ExecuteUpdateResultCheckStyle.fromExternalName(
								matchingTable.sqlUpdate().check().toString().toLowerCase()
						)
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( matchingTable.sqlDelete().sql() ) ) {
				join.setCustomSQLDelete( matchingTable.sqlDelete().sql().trim(),
						matchingTable.sqlDelete().callable(),
						ExecuteUpdateResultCheckStyle.fromExternalName(
								matchingTable.sqlDelete().check().toString().toLowerCase()
						)
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
			cacheRegion = BinderHelper.isEmptyAnnotationValue( cacheAnn.region() ) ?
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
	
	public void setNaturalIdCache(XClass clazzToProcess, NaturalIdCache naturalIdCacheAnn) {
		if ( naturalIdCacheAnn != null ) {
			if ( BinderHelper.isEmptyAnnotationValue( naturalIdCacheAnn.region() ) ) {
				if (cacheRegion != null) {
					naturalIdCacheRegion = cacheRegion + NATURAL_ID_CACHE_SUFFIX;
				}
				else {
					naturalIdCacheRegion = clazzToProcess.getName() + NATURAL_ID_CACHE_SUFFIX;
				}
			}
			else {
				naturalIdCacheRegion = naturalIdCacheAnn.region();
			}
		}
		else {
			naturalIdCacheRegion = null;
		}
	}

	public static String getCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	public void addFilter(Filter filter) {
		filters.add(filter);
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
	public void processComplementaryTableDefinitions(javax.persistence.Table table) {
		if ( table == null ) return;
		TableBinder.addIndexes( persistentClass.getTable(), table.indexes(), mappings );
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
		if ( !BinderHelper.isEmptyAnnotationValue( table.comment() ) ) hibTable.setComment( table.comment() );
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

		Access access = element.getAnnotation( Access.class );
		if ( access != null ) {
			accessType = AccessType.getAccessStrategy( access.value() );
		}

		return accessType;
	}
}
