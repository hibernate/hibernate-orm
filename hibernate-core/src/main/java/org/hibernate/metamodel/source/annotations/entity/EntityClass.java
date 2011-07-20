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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.binder.TableSource;

/**
 * Represents an entity or mapped superclass configured via annotations/xml.
 *
 * @author Hardy Ferentschik
 */
public class EntityClass extends ConfiguredClass {
	private final IdType idType;
	private final InheritanceType inheritanceType;
	private final TableSource tableSource;
	private final boolean hasOwnTable;
	private final String explicitEntityName;
	private final String customLoaderQueryName;
	private final List<String> synchronizedTableNames;
	private final String customTuplizer;
	private final int batchSize;

	private boolean isMutable;
	private boolean isExplicitPolymorphism;
	private OptimisticLockStyle optimisticLockStyle;
	private String whereClause;
	private String rowId;
	private Caching caching;
	private boolean isDynamicInsert;
	private boolean isDynamicUpdate;
	private boolean isSelectBeforeUpdate;
	private String customPersister;

	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private boolean isLazy;
	private String proxy;

	public EntityClass(
			ClassInfo classInfo,
			EntityClass parent,
			AccessType hierarchyAccessType,
			InheritanceType inheritanceType,
			AnnotationBindingContext context) {
		super( classInfo, hierarchyAccessType, parent, context );
		this.inheritanceType = inheritanceType;
		this.idType = determineIdType();
		this.hasOwnTable = definesItsOwnTable();
		this.explicitEntityName = determineExplicitEntityName();
		this.tableSource = createTableSource();
		this.customLoaderQueryName = determineCustomLoader();
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.customTuplizer = determineCustomTuplizer();
		this.batchSize = determineBatchSize();

		processHibernateEntitySpecificAnnotations();
		processCustomSqlAnnotations();
		processProxyGeneration();
	}

	private String determineExplicitEntityName() {
		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.ENTITY
		);

		return JandexHelper.getValue( jpaEntityAnnotation, "name", String.class );
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	public IdType getIdType() {
		return idType;
	}

	public boolean isExplicitPolymorphism() {
		return isExplicitPolymorphism;
	}

	public boolean isMutable() {
		return isMutable;
	}

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public String getRowId() {
		return rowId;
	}

	public Caching getCaching() {
		return caching;
	}

	public TableSource getTableSource() {
		if ( definesItsOwnTable() ) {
			return tableSource;
		}
		else {
			return ( (EntityClass) getParent() ).getTableSource();
		}
	}

	public String getExplicitEntityName() {
		return explicitEntityName;
	}

	public String getEntityName() {
		return getConfiguredClass().getSimpleName();
	}

	public boolean isDynamicInsert() {
		return isDynamicInsert;
	}

	public boolean isDynamicUpdate() {
		return isDynamicUpdate;
	}

	public boolean isSelectBeforeUpdate() {
		return isSelectBeforeUpdate;
	}

	public String getCustomLoaderQueryName() {
		return customLoaderQueryName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public List<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public String getCustomPersister() {
		return customPersister;
	}

	public String getCustomTuplizer() {
		return customTuplizer;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public String getProxy() {
		return proxy;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public boolean isEntityRoot() {
		return getParent() == null;
	}

	private boolean definesItsOwnTable() {
		return !InheritanceType.SINGLE_TABLE.equals( inheritanceType ) || isEntityRoot();
	}

	private String processTableAnnotation() {
		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				JPADotNames.TABLE
		);

		String tableName = null;
		if ( tableAnnotation != null ) {
			String explicitTableName = JandexHelper.getValue( tableAnnotation, "name", String.class );
			if ( StringHelper.isNotEmpty( explicitTableName ) ) {
				tableName = getContext().getNamingStrategy().tableName( explicitTableName );
				if ( getContext().isGloballyQuotedIdentifiers() && !Identifier.isQuoted( explicitTableName ) ) {
					tableName = StringHelper.quote( tableName );
				}
			}
		}
		return tableName;
	}

	private IdType determineIdType() {
		List<AnnotationInstance> idAnnotations = findIdAnnotations( JPADotNames.ID );
		List<AnnotationInstance> embeddedIdAnnotations = findIdAnnotations( JPADotNames.EMBEDDED_ID );

		if ( !idAnnotations.isEmpty() && !embeddedIdAnnotations.isEmpty() ) {
			throw new MappingException(
					"@EmbeddedId and @Id cannot be used together. Check the configuration for " + getName() + "."
			);
		}

		if ( !embeddedIdAnnotations.isEmpty() ) {
			if ( embeddedIdAnnotations.size() == 1 ) {
				return IdType.EMBEDDED;
			}
			else {
				throw new AnnotationException( "Multiple @EmbeddedId annotations are not allowed" );
			}
		}

		if ( !idAnnotations.isEmpty() ) {
			if ( idAnnotations.size() == 1 ) {
				return IdType.SIMPLE;
			}
			else {
				return IdType.COMPOSED;
			}
		}
		return IdType.NONE;
	}

	private List<AnnotationInstance> findIdAnnotations(DotName idAnnotationType) {
		List<AnnotationInstance> idAnnotationList = new ArrayList<AnnotationInstance>();
		if ( getClassInfo().annotations().get( idAnnotationType ) != null ) {
			idAnnotationList.addAll( getClassInfo().annotations().get( idAnnotationType ) );
		}
		ConfiguredClass parent = getParent();
		while ( parent != null && ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( parent.getConfiguredClassType() ) ||
				ConfiguredClassType.NON_ENTITY.equals( parent.getConfiguredClassType() ) ) ) {
			if ( parent.getClassInfo().annotations().get( idAnnotationType ) != null ) {
				idAnnotationList.addAll( parent.getClassInfo().annotations().get( idAnnotationType ) );
			}
			parent = parent.getParent();

		}
		return idAnnotationList;
	}

	private void processHibernateEntitySpecificAnnotations() {
		final AnnotationInstance hibernateEntityAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.ENTITY
		);

		// see HHH-6400
		PolymorphismType polymorphism = PolymorphismType.IMPLICIT;
		if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "polymorphism" ) != null ) {
			polymorphism = PolymorphismType.valueOf( hibernateEntityAnnotation.value( "polymorphism" ).asEnum() );
		}
		isExplicitPolymorphism = polymorphism == PolymorphismType.EXPLICIT;

		// see HHH-6401
		OptimisticLockType optimisticLockType = OptimisticLockType.VERSION;
		if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "optimisticLock" ) != null ) {
			optimisticLockType = OptimisticLockType.valueOf(
					hibernateEntityAnnotation.value( "optimisticLock" )
							.asEnum()
			);
		}
		optimisticLockStyle = OptimisticLockStyle.valueOf( optimisticLockType.name() );

		final AnnotationInstance hibernateImmutableAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.IMMUTABLE
		);
		isMutable = hibernateImmutableAnnotation == null
				&& hibernateEntityAnnotation != null
				&& hibernateEntityAnnotation.value( "mutable" ) != null
				&& hibernateEntityAnnotation.value( "mutable" ).asBoolean();


		final AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.WHERE
		);
		whereClause = whereAnnotation != null && whereAnnotation.value( "clause" ) != null ?
				whereAnnotation.value( "clause" ).asString() : null;

		final AnnotationInstance rowIdAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.ROW_ID
		);
		rowId = rowIdAnnotation != null && rowIdAnnotation.value() != null
				? rowIdAnnotation.value().asString() : null;

		caching = determineCachingSettings();

		// see HHH-6397
		isDynamicInsert =
				hibernateEntityAnnotation != null
						&& hibernateEntityAnnotation.value( "dynamicInsert" ) != null
						&& hibernateEntityAnnotation.value( "dynamicInsert" ).asBoolean();

		// see HHH-6398
		isDynamicUpdate =
				hibernateEntityAnnotation != null
						&& hibernateEntityAnnotation.value( "dynamicUpdate" ) != null
						&& hibernateEntityAnnotation.value( "dynamicUpdate" ).asBoolean();


		// see HHH-6399
		isSelectBeforeUpdate =
				hibernateEntityAnnotation != null
						&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ) != null
						&& hibernateEntityAnnotation.value( "selectBeforeUpdate" ).asBoolean();

		// Custom persister
		final String entityPersisterClass;
		final AnnotationInstance persisterAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.PERSISTER
		);
		if ( persisterAnnotation == null || persisterAnnotation.value( "impl" ) == null ) {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				entityPersisterClass = hibernateEntityAnnotation.value( "persister" ).asString();
			}
			else {
				entityPersisterClass = null;
			}
		}
		else {
			if ( hibernateEntityAnnotation != null && hibernateEntityAnnotation.value( "persister" ) != null ) {
				// todo : error?
			}
			entityPersisterClass = persisterAnnotation.value( "impl" ).asString();
		}
		this.customPersister = entityPersisterClass;
	}

	private Caching determineCachingSettings() {
		final AnnotationInstance hibernateCacheAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.CACHE
		);
		if ( hibernateCacheAnnotation != null ) {
			final org.hibernate.cache.spi.access.AccessType accessType = hibernateCacheAnnotation.value( "usage" ) == null
					? getContext().getMappingDefaults().getCacheAccessType()
					: CacheConcurrencyStrategy.parse( hibernateCacheAnnotation.value( "usage" ).asEnum() )
					.toAccessType();
			return new Caching(
					hibernateCacheAnnotation.value( "region" ) == null
							? getName()
							: hibernateCacheAnnotation.value( "region" ).asString(),
					accessType,
					hibernateCacheAnnotation.value( "include" ) != null
							&& "all".equals( hibernateCacheAnnotation.value( "include" ).asString() )
			);
		}

		final AnnotationInstance jpaCacheableAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.CACHEABLE
		);

		boolean cacheable = true; // true is the default
		if ( jpaCacheableAnnotation != null && jpaCacheableAnnotation.value() != null ) {
			cacheable = jpaCacheableAnnotation.value().asBoolean();
		}

		final boolean doCaching;
		switch ( getContext().getMetadataImplementor().getOptions().getSharedCacheMode() ) {
			case ALL: {
				doCaching = true;
				break;
			}
			case ENABLE_SELECTIVE: {
				doCaching = cacheable;
				break;
			}
			case DISABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation == null || cacheable;
				break;
			}
			default: {
				// treat both NONE and UNSPECIFIED the same
				doCaching = false;
				break;
			}
		}

		if ( !doCaching ) {
			return null;
		}

		return new Caching(
				getName(),
				getContext().getMappingDefaults().getCacheAccessType(),
				true
		);
	}

	private TableSource createTableSource() {
		if ( !hasOwnTable ) {
			return null;
		}

		String schema = getContext().getMappingDefaults().getSchemaName();
		String catalog = getContext().getMappingDefaults().getCatalogName();

		final AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.TABLE
		);
		if ( tableAnnotation != null ) {
			final AnnotationValue schemaValue = tableAnnotation.value( "schema" );
			if ( schemaValue != null ) {
				schema = schemaValue.asString();
			}

			final AnnotationValue catalogValue = tableAnnotation.value( "catalog" );
			if ( catalogValue != null ) {
				catalog = catalogValue.asString();
			}
		}

		if ( getContext().isGloballyQuotedIdentifiers() ) {
			schema = StringHelper.quote( schema );
			catalog = StringHelper.quote( catalog );
		}

		String tableName = processTableAnnotation();
		// use the simple table name as default in case there was no table annotation
		if ( tableName == null ) {
			if ( explicitEntityName == null ) {
				tableName = getConfiguredClass().getSimpleName();
			}
			else {
				tableName = explicitEntityName;
			}
		}
		return new TableSourceImpl( schema, catalog, tableName );
	}

	private String determineCustomLoader() {
		String customLoader = null;
		// Custom sql loader
		final AnnotationInstance sqlLoaderAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.LOADER
		);
		if ( sqlLoaderAnnotation != null ) {
			customLoader = sqlLoaderAnnotation.value( "namedQuery" ).asString();
		}
		return customLoader;
	}

	private CustomSQL createCustomSQL(AnnotationInstance customSqlAnnotation) {
		if ( customSqlAnnotation == null ) {
			return null;
		}

		final String sql = customSqlAnnotation.value( "sql" ).asString();
		final boolean isCallable = customSqlAnnotation.value( "callable" ) != null
				&& customSqlAnnotation.value( "callable" ).asBoolean();

		final ExecuteUpdateResultCheckStyle checkStyle = customSqlAnnotation.value( "check" ) == null
				? isCallable
				? ExecuteUpdateResultCheckStyle.NONE
				: ExecuteUpdateResultCheckStyle.COUNT
				: ExecuteUpdateResultCheckStyle.valueOf( customSqlAnnotation.value( "check" ).asEnum() );

		return new CustomSQL( sql, isCallable, checkStyle );
	}

	private void processCustomSqlAnnotations() {
		// Custom sql insert
		final AnnotationInstance sqlInsertAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.SQL_INSERT
		);
		customInsert = createCustomSQL( sqlInsertAnnotation );

		// Custom sql update
		final AnnotationInstance sqlUpdateAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.SQL_UPDATE
		);
		customUpdate = createCustomSQL( sqlUpdateAnnotation );

		// Custom sql delete
		final AnnotationInstance sqlDeleteAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.SQL_DELETE
		);
		customDelete = createCustomSQL( sqlDeleteAnnotation );
	}

	private List<String> determineSynchronizedTableNames() {
		final AnnotationInstance synchronizeAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.SYNCHRONIZE
		);
		if ( synchronizeAnnotation != null ) {
			final String[] tableNames = synchronizeAnnotation.value().asStringArray();
			return Arrays.asList( tableNames );
		}
		else {
			return Collections.emptyList();
		}
	}

	private String determineCustomTuplizer() {
		// Custom tuplizer
		String customTuplizer = null;
		final AnnotationInstance pojoTuplizerAnnotation = locatePojoTuplizerAnnotation();
		if ( pojoTuplizerAnnotation != null ) {
			customTuplizer = pojoTuplizerAnnotation.value( "impl" ).asString();
		}
		return customTuplizer;
	}

	private AnnotationInstance locatePojoTuplizerAnnotation() {
		final AnnotationInstance tuplizersAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.TUPLIZERS
		);
		if ( tuplizersAnnotation == null ) {
			return null;
		}

		AnnotationInstance[] annotations = JandexHelper.getValue(
				tuplizersAnnotation,
				"value",
				AnnotationInstance[].class
		);
		for ( AnnotationInstance tuplizerAnnotation : annotations ) {
			if ( EntityMode.valueOf( tuplizerAnnotation.value( "entityModeType" ).asEnum() ) == EntityMode.POJO ) {
				return tuplizerAnnotation;
			}
		}
		return null;
	}

	private void processProxyGeneration() {
		// Proxy generation
		final AnnotationInstance hibernateProxyAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.PROXY
		);
		if ( hibernateProxyAnnotation != null ) {
			isLazy = hibernateProxyAnnotation.value( "lazy" ) == null
					|| hibernateProxyAnnotation.value( "lazy" ).asBoolean();
			if ( isLazy ) {
				final AnnotationValue proxyClassValue = hibernateProxyAnnotation.value( "proxyClass" );
				if ( proxyClassValue == null ) {
					proxy = getName();
				}
				else {
					proxy = proxyClassValue.asString();
				}
			}
			else {
				proxy = null;
			}
		}
		else {
			isLazy = true;
			proxy = getName();
		}
	}

	private int determineBatchSize() {
		final AnnotationInstance batchSizeAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.BATCH_SIZE
		);
		return batchSizeAnnotation == null ? -1 : batchSizeAnnotation.value( "size" ).asInt();
	}

	class TableSourceImpl implements TableSource {
		private final String schema;
		private final String catalog;
		private final String tableName;

		TableSourceImpl(String schema, String catalog, String tableName) {
			this.schema = schema;
			this.catalog = catalog;
			this.tableName = tableName;
		}

		@Override
		public String getExplicitSchemaName() {
			return schema;
		}

		@Override
		public String getExplicitCatalogName() {
			return catalog;
		}

		@Override
		public String getExplicitTableName() {
			return tableName;
		}

		@Override
		public String getLogicalName() {
			// todo : (steve) hardy, not sure what to use here... null is ok for the primary table name.  this is part of the secondary table support.
			return null;
		}
	}
}
