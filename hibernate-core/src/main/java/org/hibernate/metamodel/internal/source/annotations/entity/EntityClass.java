/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AccessType;
import javax.persistence.DiscriminatorType;
import javax.persistence.PersistenceException;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.FormulaValue;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.internal.source.annotations.xml.PseudoJpaDotNames;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * Represents an entity or mapped superclass configured via annotations/orm-xml.
 *
 * @author Hardy Ferentschik
 */
public class EntityClass extends ConfiguredClass {
	private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";
	private final IdType idType;
	private final InheritanceType inheritanceType;

	private final String explicitEntityName;
	private final String customLoaderQueryName;
	private final List<String> synchronizedTableNames;
	private final int batchSize;

	private final TableSpecificationSource primaryTableSource;
	private final Set<SecondaryTableSource> secondaryTableSources;
	private final Set<ConstraintSource> constraintSources;

	private boolean isMutable;
	private boolean isExplicitPolymorphism;
	private OptimisticLockStyle optimisticLockStyle;
	private String whereClause;
	private String rowId;
	private Caching caching;
	private Caching naturalIdCaching;
	private boolean isDynamicInsert;
	private boolean isDynamicUpdate;
	private boolean isSelectBeforeUpdate;
	private String customPersister;

	private final CustomSQL customInsert;
	private final CustomSQL customUpdate;
	private final CustomSQL customDelete;

	private boolean isLazy;
	private String proxy;

	private Column discriminatorColumnValues;
	private FormulaValue discriminatorFormula;
	private Class<?> discriminatorType;
	private String discriminatorMatchValue;
	private boolean isDiscriminatorForced = true;
	private boolean isDiscriminatorIncludedInSql = true;

	private final List<JpaCallbackSource> jpaCallbacks;

	private List<ConfiguredClass> mappedSuperclasses;

	public EntityClass(
			ClassInfo classInfo,
			List<ClassInfo> mappedSuperclasses,
			AccessType hierarchyAccessType,
			InheritanceType inheritanceType,
			AnnotationBindingContext context) {
		this( classInfo, ( EntityClass ) null, hierarchyAccessType, inheritanceType, context );
		for(ClassInfo mappedSuperclassInfo : mappedSuperclasses) {
			ConfiguredClass configuredClass = new ConfiguredClass( mappedSuperclassInfo, hierarchyAccessType, null, context );
			this.mappedSuperclasses.add( configuredClass );
		}
	}

	public EntityClass(
			ClassInfo classInfo,
			EntityClass parent,
			AccessType hierarchyAccessType,
			InheritanceType inheritanceType,
			AnnotationBindingContext context) {
		super( classInfo, hierarchyAccessType, parent, context );
		this.inheritanceType = inheritanceType;
		this.idType = determineIdType();

		final boolean hasOwnTable = definesItsOwnTable();

		this.explicitEntityName = determineExplicitEntityName();

		this.constraintSources = new HashSet<ConstraintSource>();
		this.primaryTableSource = hasOwnTable ? createPrimaryTableSource() : null;
		this.secondaryTableSources = createSecondaryTableSources();

		this.customLoaderQueryName = determineCustomLoader();
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.jpaCallbacks = determineEntityListeners();

		this.customInsert = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_INSERT,
				getClassInfo().annotations()
		);
		this.customUpdate = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_UPDATE,
				getClassInfo().annotations()
		);
		this.customDelete = AnnotationParserHelper.processCustomSqlAnnotation(
				HibernateDotNames.SQL_DELETE,
				getClassInfo().annotations()
		);

		this.mappedSuperclasses = new ArrayList<ConfiguredClass>(  );

		processHibernateEntitySpecificAnnotations();
		processProxyGeneration();
		processDiscriminator();
	}

	public Column getDiscriminatorColumnValues() {
		return discriminatorColumnValues;
	}

	public FormulaValue getDiscriminatorFormula() {
		return discriminatorFormula;
	}

	public Class<?> getDiscriminatorType() {
		return discriminatorType;
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

	public Caching getNaturalIdCaching() {
		return naturalIdCaching;
	}

	public TableSpecificationSource getPrimaryTableSource() {
		// todo : this is different from hbm which returns null if "!definesItsOwnTable()"
		return definesItsOwnTable() ? primaryTableSource : ( ( EntityClass ) getParent() ).getPrimaryTableSource();
	}

	public Set<SecondaryTableSource> getSecondaryTableSources() {
		return secondaryTableSources;
	}

	public Set<ConstraintSource> getConstraintSources() {
		return constraintSources;
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

	public boolean isDiscriminatorForced() {
		return isDiscriminatorForced;
	}

	public boolean isDiscriminatorIncludedInSql() {
		return isDiscriminatorIncludedInSql;
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}

	public List<JpaCallbackSource> getJpaCallbacks() {
		return jpaCallbacks;
	}

	public List<ConfiguredClass> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	private String determineExplicitEntityName() {
		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.ENTITY
		);
		return JandexHelper.getValue( jpaEntityAnnotation, "name", String.class );
	}


	private boolean definesItsOwnTable() {
		return !InheritanceType.SINGLE_TABLE.equals( inheritanceType ) || isEntityRoot();
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
			return idAnnotations.size() == 1 ? IdType.SIMPLE : IdType.COMPOSED;
		}
		return IdType.NONE;
	}

	private List<AnnotationInstance> findIdAnnotations(DotName idAnnotationType) {
		List<AnnotationInstance> idAnnotationList = new ArrayList<AnnotationInstance>();
		if ( getClassInfo().annotations().containsKey( idAnnotationType ) ) {
			idAnnotationList.addAll( getClassInfo().annotations().get( idAnnotationType ) );
		}
		ConfiguredClass parent = getParent();
		while ( parent != null ) {
			if ( parent.getClassInfo().annotations().containsKey( idAnnotationType ) ) {
				idAnnotationList.addAll( parent.getClassInfo().annotations().get( idAnnotationType ) );
			}
			parent = parent.getParent();
		}
		return idAnnotationList;
	}

	private void processDiscriminator() {
		if ( !InheritanceType.SINGLE_TABLE.equals( inheritanceType ) ) {
			return;
		}

		final AnnotationInstance discriminatorValueAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.DISCRIMINATOR_VALUE
		);
		if ( discriminatorValueAnnotation != null ) {
			this.discriminatorMatchValue = discriminatorValueAnnotation.value().asString();
		}

		final AnnotationInstance discriminatorColumnAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.DISCRIMINATOR_COLUMN
		);

		final AnnotationInstance discriminatorFormulaAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.DISCRIMINATOR_FORMULA
		);


		Class<?> type = String.class; // string is the discriminator default
		if ( discriminatorFormulaAnnotation != null ) {
			String expression = JandexHelper.getValue( discriminatorFormulaAnnotation, "value", String.class );
			discriminatorFormula = new FormulaValue( null, expression );
		}
		discriminatorColumnValues = new Column( null ); //(stliu) give null here, will populate values below
		discriminatorColumnValues.setNullable( false ); // discriminator column cannot be null
		if ( discriminatorColumnAnnotation != null ) {

			DiscriminatorType discriminatorType = Enum.valueOf(
					DiscriminatorType.class, discriminatorColumnAnnotation.value( "discriminatorType" ).asEnum()
			);
			switch ( discriminatorType ) {
				case STRING: {
					type = String.class;
					break;
				}
				case CHAR: {
					type = Character.class;
					break;
				}
				case INTEGER: {
					type = Integer.class;
					break;
				}
				default: {
					throw new AnnotationException( "Unsupported discriminator type: " + discriminatorType );
				}
			}

			discriminatorColumnValues.setName(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"name",
							String.class
					)
			);
			discriminatorColumnValues.setLength(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"length",
							Integer.class
					)
			);
			discriminatorColumnValues.setColumnDefinition(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"columnDefinition",
							String.class
					)
			);
		}
		discriminatorType = type;

		AnnotationInstance discriminatorOptionsAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.DISCRIMINATOR_OPTIONS
		);
		if ( discriminatorOptionsAnnotation != null ) {
			isDiscriminatorForced = discriminatorOptionsAnnotation.value( "force" ).asBoolean();
			isDiscriminatorIncludedInSql = discriminatorOptionsAnnotation.value( "insert" ).asBoolean();
		}
		else {
			isDiscriminatorForced = false;
			isDiscriminatorIncludedInSql = true;
		}
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

		naturalIdCaching = determineNaturalIdCachingSettings( caching );

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

	private Caching determineNaturalIdCachingSettings(final Caching entityCache) {
		final AnnotationInstance naturalIdCacheAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.NATURAL_ID_CACHE
		);
		if ( naturalIdCacheAnnotation == null ) {
			return null;
		}
		final String region;
		if ( naturalIdCacheAnnotation.value( "region" ) == null || StringHelper.isEmpty(
				naturalIdCacheAnnotation.value(
						"region"
				).asString()
		) ) {
			region = entityCache == null ? getEntityName() + NATURAL_ID_CACHE_SUFFIX : entityCache.getRegion() + NATURAL_ID_CACHE_SUFFIX;
		}
		else {
			region = naturalIdCacheAnnotation.value( "region" ).asString();
		}
		return new Caching( region, null, false );
	}

	private Caching determineCachingSettings() {
		final AnnotationInstance hibernateCacheAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.CACHE
		);
		if ( hibernateCacheAnnotation != null ) {
			final org.hibernate.cache.spi.access.AccessType accessType = hibernateCacheAnnotation.value( "usage" ) == null
					? getLocalBindingContext().getMappingDefaults().getCacheAccessType()
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

		final boolean doCaching;
		switch ( getLocalBindingContext().getMetadataImplementor().getOptions().getSharedCacheMode() ) {
			case ALL: {
				doCaching = true;
				break;
			}
			case ENABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation != null
						&& JandexHelper.getValue( jpaCacheableAnnotation, "value", Boolean.class );
				break;
			}
			case DISABLE_SELECTIVE: {
				doCaching = jpaCacheableAnnotation == null
						|| !JandexHelper.getValue( jpaCacheableAnnotation, "value", Boolean.class );
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
				getLocalBindingContext().getMappingDefaults().getCacheAccessType(),
				true
		);
	}

	private TableSpecificationSource createPrimaryTableSource() {
		AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				JPADotNames.TABLE
		);
		AnnotationInstance subselectAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				JPADotNames.SUBSELECT
		);

		if ( tableAnnotation != null ) {
			return createPrimaryTableSourceAsTable( tableAnnotation );
		}
		else if ( subselectAnnotation != null ) {
			return createPrimaryTableSourceAsInLineView( subselectAnnotation );
		}
		else {
			return new TableSourceImpl( null, null, null );
		}
	}

	private TableSpecificationSource createPrimaryTableSourceAsTable(AnnotationInstance tableAnnotation) {
		final String schemaName = determineSchemaName( tableAnnotation );
		final String catalogName = determineCatalogName( tableAnnotation );

		final String explicitTableName = tableAnnotation == null
				? null
				: JandexHelper.getValue( tableAnnotation, "name", String.class );

		if ( tableAnnotation != null ) {
			createUniqueConstraints( tableAnnotation, null );
		}

		return new TableSourceImpl( schemaName, catalogName, explicitTableName );
	}

	private TableSpecificationSource createPrimaryTableSourceAsInLineView(AnnotationInstance subselectAnnotation) {
		return new InLineViewSourceImpl(
				JandexHelper.getValue( subselectAnnotation, "value", String.class ),
				getEntityName()
		);
	}

	private String determineSchemaName(AnnotationInstance tableAnnotation) {
		return tableAnnotation == null
				? null
				: JandexHelper.getValue( tableAnnotation, "schema", String.class );
	}

	private String determineCatalogName(AnnotationInstance tableAnnotation) {
		return tableAnnotation == null
				? null
				: JandexHelper.getValue( tableAnnotation, "catalog", String.class );
	}

	private void createUniqueConstraints(AnnotationInstance tableAnnotation, String tableName) {
		final AnnotationValue value = tableAnnotation.value( "uniqueConstraints" );
		if ( value == null ) {
			return;
		}

		final AnnotationInstance[] uniqueConstraints = value.asNestedArray();
		for ( final AnnotationInstance unique : uniqueConstraints ) {
			final String name = unique.value( "name" ) == null ? null : unique.value( "name" ).asString();
			final String[] columnNames = unique.value( "columnNames" ).asStringArray();
			final UniqueConstraintSourceImpl uniqueConstraintSource =
					new UniqueConstraintSourceImpl(
							name, tableName, Arrays.asList( columnNames )
					);
			constraintSources.add( uniqueConstraintSource );
		}
	}

	private Set<SecondaryTableSource> createSecondaryTableSources() {
		final Set<SecondaryTableSource> secondaryTableSources = new HashSet<SecondaryTableSource>();

		//	process a singular @SecondaryTable annotation
		{
			AnnotationInstance secondaryTable = JandexHelper.getSingleAnnotation(
					getClassInfo(),
					JPADotNames.SECONDARY_TABLE
			);
			if ( secondaryTable != null ) {
				secondaryTableSources.add( createSecondaryTableSource( secondaryTable ) );
			}
		}
		// process any @SecondaryTables grouping
		{
			AnnotationInstance secondaryTables = JandexHelper.getSingleAnnotation(
					getClassInfo(),
					JPADotNames.SECONDARY_TABLES
			);
			if ( secondaryTables != null ) {
				for ( AnnotationInstance secondaryTable : JandexHelper.getValue(
						secondaryTables,
						"value",
						AnnotationInstance[].class
				) ) {
					secondaryTableSources.add( createSecondaryTableSource( secondaryTable ) );
				}
			}
		}

		return secondaryTableSources;
	}

	private SecondaryTableSource createSecondaryTableSource(AnnotationInstance tableAnnotation) {
		final String schemaName = determineSchemaName( tableAnnotation );
		final String catalogName = determineCatalogName( tableAnnotation );
		final String tableName = JandexHelper.getValue( tableAnnotation, "name", String.class );

		createUniqueConstraints( tableAnnotation, tableName );

		final List<PrimaryKeyJoinColumnSource> keys = collectionSecondaryTableKeys( tableAnnotation );
		return new SecondaryTableSourceImpl( new TableSourceImpl( schemaName, catalogName, tableName ), keys );
	}

	private List<PrimaryKeyJoinColumnSource> collectionSecondaryTableKeys(final AnnotationInstance tableAnnotation) {
		final AnnotationInstance[] joinColumnAnnotations = JandexHelper.getValue(
				tableAnnotation,
				"pkJoinColumns",
				AnnotationInstance[].class
		);

		if ( joinColumnAnnotations == null ) {
			return Collections.emptyList();
		}
		final List<PrimaryKeyJoinColumnSource> keys = new ArrayList<PrimaryKeyJoinColumnSource>();
		for ( final AnnotationInstance joinColumnAnnotation : joinColumnAnnotations ) {
			keys.add( new PrimaryKeyJoinColumnSourceImpl( joinColumnAnnotation ) );
		}
		return keys;
	}

	public boolean hasMultiTenancySourceInformation() {
		return JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.MULTI_TENANT ) != null
				|| JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.TENANT_COLUMN ) != null
				|| JandexHelper.getSingleAnnotation( getClassInfo(), HibernateDotNames.TENANT_FORMULA ) != null;
	}


	private static class PrimaryKeyJoinColumnSourceImpl implements PrimaryKeyJoinColumnSource {
		private final String columnName;
		private final String referencedColumnName;
		private final String columnDefinition;

		private PrimaryKeyJoinColumnSourceImpl(AnnotationInstance joinColumnAnnotation) {
			this(
					JandexHelper.getValue( joinColumnAnnotation, "name", String.class ),
					JandexHelper.getValue( joinColumnAnnotation, "referencedColumnName", String.class ),
					JandexHelper.getValue( joinColumnAnnotation, "columnDefinition", String.class )
			);
		}

		private PrimaryKeyJoinColumnSourceImpl(
				String columnName,
				String referencedColumnName,
				String columnDefinition) {
			this.columnName = columnName;
			this.referencedColumnName = referencedColumnName;
			this.columnDefinition = columnDefinition;
		}

		@Override
		public String getColumnName() {
			return columnName;
		}

		@Override
		public String getReferencedColumnName() {
			return referencedColumnName;
		}

		@Override
		public String getColumnDefinition() {
			return columnDefinition;
		}
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
				proxy = proxyClassValue == null ? getName() : proxyClassValue.asString();
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

	private List<JpaCallbackSource> determineEntityListeners() {
		List<JpaCallbackSource> callbackClassList = new ArrayList<JpaCallbackSource>();

		// Bind default JPA entity listener callbacks (unless excluded), using superclasses first (unless excluded)
		if ( JandexHelper.getSingleAnnotation( getClassInfo(), JPADotNames.EXCLUDE_DEFAULT_LISTENERS ) == null ) {
			List<AnnotationInstance> defaultEntityListenerAnnotations = getLocalBindingContext().getIndex()
					.getAnnotations( PseudoJpaDotNames.DEFAULT_ENTITY_LISTENERS );
			for ( AnnotationInstance annotation : defaultEntityListenerAnnotations ) {
				for ( Type callbackClass : annotation.value().asClassArray() ) {
					String callbackClassName = callbackClass.name().toString();
					try {
						processDefaultJpaCallbacks( callbackClassName, callbackClassList );
					}
					catch ( PersistenceException error ) {
						throw new PersistenceException( error.getMessage() + "default entity listener " + callbackClassName );
					}
				}
			}
		}

		// Bind JPA entity listener callbacks, using superclasses first (unless excluded)
		List<AnnotationInstance> annotationList = getClassInfo().annotations().get( JPADotNames.ENTITY_LISTENERS );
		if ( annotationList != null ) {
			for ( AnnotationInstance annotation : annotationList ) {
				for ( Type callbackClass : annotation.value().asClassArray() ) {
					String callbackClassName = callbackClass.name().toString();
					try {
						processJpaCallbacks( callbackClassName, true, callbackClassList );
					}
					catch ( PersistenceException error ) {
						throw new PersistenceException( error.getMessage() + "entity listener " + callbackClassName );
					}
				}
			}
		}

		// Bind JPA entity.mapped superclass callbacks, using superclasses first (unless excluded)
		try {
			processJpaCallbacks( getName(), false, callbackClassList );
		}
		catch ( PersistenceException error ) {
			throw new PersistenceException(
					error.getMessage() + "entity/mapped superclass " + getClassInfo().name().toString()
			);
		}

		return callbackClassList;
	}

	private void processDefaultJpaCallbacks(String instanceCallbackClassName, List<JpaCallbackSource> jpaCallbackClassList) {
		ClassInfo callbackClassInfo = getLocalBindingContext().getClassInfo( instanceCallbackClassName );

		// Process superclass first if available and not excluded
		if ( JandexHelper.getSingleAnnotation( callbackClassInfo, JPADotNames.EXCLUDE_SUPERCLASS_LISTENERS ) != null ) {
			DotName superName = callbackClassInfo.superName();
			if ( superName != null ) {
				processDefaultJpaCallbacks( instanceCallbackClassName, jpaCallbackClassList );
			}
		}

		String callbackClassName = callbackClassInfo.name().toString();
		Map<Class<?>, String> callbacksByType = new HashMap<Class<?>, String>();
		createDefaultCallback(
				PrePersist.class, PseudoJpaDotNames.DEFAULT_PRE_PERSIST, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PreRemove.class, PseudoJpaDotNames.DEFAULT_PRE_REMOVE, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PreUpdate.class, PseudoJpaDotNames.DEFAULT_PRE_UPDATE, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostLoad.class, PseudoJpaDotNames.DEFAULT_POST_LOAD, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostPersist.class, PseudoJpaDotNames.DEFAULT_POST_PERSIST, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostRemove.class, PseudoJpaDotNames.DEFAULT_POST_REMOVE, callbackClassName, callbacksByType
		);
		createDefaultCallback(
				PostUpdate.class, PseudoJpaDotNames.DEFAULT_POST_UPDATE, callbackClassName, callbacksByType
		);
		if ( !callbacksByType.isEmpty() ) {
			jpaCallbackClassList.add( new JpaCallbackClassImpl( instanceCallbackClassName, callbacksByType, true ) );
		}
	}

	private void processJpaCallbacks(String instanceCallbackClassName, boolean isListener, List<JpaCallbackSource> callbackClassList) {

		ClassInfo callbackClassInfo = getLocalBindingContext().getClassInfo( instanceCallbackClassName );

		// Process superclass first if available and not excluded
		if ( JandexHelper.getSingleAnnotation( callbackClassInfo, JPADotNames.EXCLUDE_SUPERCLASS_LISTENERS ) != null ) {
			DotName superName = callbackClassInfo.superName();
			if ( superName != null ) {
				processJpaCallbacks(
						instanceCallbackClassName,
						isListener,
						callbackClassList
				);
			}
		}

		Map<Class<?>, String> callbacksByType = new HashMap<Class<?>, String>();
		createCallback( PrePersist.class, JPADotNames.PRE_PERSIST, callbacksByType, callbackClassInfo, isListener );
		createCallback( PreRemove.class, JPADotNames.PRE_REMOVE, callbacksByType, callbackClassInfo, isListener );
		createCallback( PreUpdate.class, JPADotNames.PRE_UPDATE, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostLoad.class, JPADotNames.POST_LOAD, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostPersist.class, JPADotNames.POST_PERSIST, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostRemove.class, JPADotNames.POST_REMOVE, callbacksByType, callbackClassInfo, isListener );
		createCallback( PostUpdate.class, JPADotNames.POST_UPDATE, callbacksByType, callbackClassInfo, isListener );
		if ( !callbacksByType.isEmpty() ) {
			callbackClassList.add( new JpaCallbackClassImpl( instanceCallbackClassName, callbacksByType, isListener ) );
		}
	}

	private void createDefaultCallback(Class callbackTypeClass,
									   DotName callbackTypeName,
									   String callbackClassName,
									   Map<Class<?>, String> callbacksByClass) {
		for ( AnnotationInstance callback : getLocalBindingContext().getIndex().getAnnotations( callbackTypeName ) ) {
			MethodInfo methodInfo = ( MethodInfo ) callback.target();
			validateMethod( methodInfo, callbackTypeClass, callbacksByClass, true );
			if ( methodInfo.declaringClass().name().toString().equals( callbackClassName ) ) {
				if ( methodInfo.args().length != 1 ) {
					throw new PersistenceException(
							String.format(
									"Callback method %s must have exactly one argument defined as either Object or %s in ",
									methodInfo.name(),
									getEntityName()
							)
					);
				}
				callbacksByClass.put( callbackTypeClass, methodInfo.name() );
			}
		}
	}

	private void createCallback(Class callbackTypeClass,
								DotName callbackTypeName,
								Map<Class<?>, String> callbacksByClass,
								ClassInfo callbackClassInfo,
								boolean isListener) {
		Map<DotName, List<AnnotationInstance>> annotations = callbackClassInfo.annotations();
		List<AnnotationInstance> annotationInstances = annotations.get( callbackTypeName );
		if ( annotationInstances == null ) {
			return;
		}
		for ( AnnotationInstance callbackAnnotation : annotationInstances ) {
			MethodInfo methodInfo = ( MethodInfo ) callbackAnnotation.target();
			validateMethod( methodInfo, callbackTypeClass, callbacksByClass, isListener );
			callbacksByClass.put( callbackTypeClass, methodInfo.name() );
		}
	}

	private void validateMethod(MethodInfo methodInfo,
								Class callbackTypeClass,
								Map<Class<?>, String> callbacksByClass,
								boolean isListener) {
		if ( methodInfo.returnType().kind() != Kind.VOID ) {
			throw new PersistenceException( "Callback method " + methodInfo.name() + " must have a void return type in " );
		}
		if ( Modifier.isStatic( methodInfo.flags() ) || Modifier.isFinal( methodInfo.flags() ) ) {
			throw new PersistenceException( "Callback method " + methodInfo.name() + " must not be static or final in " );
		}
		Type[] argTypes = methodInfo.args();
		if ( isListener ) {
			if ( argTypes.length != 1 ) {
				throw new PersistenceException( "Callback method " + methodInfo.name() + " must have exactly one argument in " );
			}
			String argTypeName = argTypes[0].name().toString();
			if ( !argTypeName.equals( Object.class.getName() ) && !argTypeName.equals( getName() ) ) {
				throw new PersistenceException(
						"The argument for callback method " + methodInfo.name() +
								" must be defined as either Object or " + getEntityName() + " in "
				);
			}
		}
		else if ( argTypes.length != 0 ) {
			throw new PersistenceException( "Callback method " + methodInfo.name() + " must have no arguments in " );
		}
		if ( callbacksByClass.containsKey( callbackTypeClass ) ) {
			throw new PersistenceException(
					"Only one method may be annotated as a " + callbackTypeClass.getSimpleName() +
							" callback method in "
			);
		}
	}

	// Process JPA callbacks, in superclass-first order (unless superclasses are excluded), using default listeners first
	// (unless default listeners are excluded), then entity listeners, and finally the entity/mapped superclass itself
	private class JpaCallbackClassImpl implements JpaCallbackSource {

		private final Map<Class<?>, String> callbacksByType;
		private final String name;
		private final boolean isListener;

		private JpaCallbackClassImpl(String name,
									 Map<Class<?>, String> callbacksByType,
									 boolean isListener) {
			this.name = name;
			this.callbacksByType = callbacksByType;
			this.isListener = isListener;
		}

		@Override
		public String getCallbackMethod(Class<?> callbackType) {
			return callbacksByType.get( callbackType );
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isListener() {
			return isListener;
		}
	}
}
