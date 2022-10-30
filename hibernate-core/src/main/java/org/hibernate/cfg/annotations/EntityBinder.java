/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Cacheable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SharedCacheMode;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.SecondaryRows;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.annotations.Tables;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.NamingStrategyHelper;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedClassType;
import org.hibernate.cfg.AnnotatedDiscriminatorColumn;
import org.hibernate.cfg.AnnotatedJoinColumns;
import org.hibernate.cfg.AnnotationBinder;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.CreateKeySecondPass;
import org.hibernate.cfg.IdGeneratorResolverSecondPass;
import org.hibernate.cfg.InheritanceState;
import org.hibernate.cfg.JoinedSubclassFkSecondPass;
import org.hibernate.cfg.PropertyData;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.cfg.PropertyPreloadedData;
import org.hibernate.cfg.SecondPass;
import org.hibernate.cfg.SecondaryTableFromAnnotationSecondPass;
import org.hibernate.cfg.SecondaryTableSecondPass;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.cfg.internal.NullableDiscriminatorColumnSecondPass;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.event.internal.CallbackDefinitionResolverLegacyImpl;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.loader.PropertyPath;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.Value;

import org.hibernate.persister.entity.EntityPersister;
import org.jboss.logging.Logger;

import static org.hibernate.cfg.AnnotatedDiscriminatorColumn.buildDiscriminatorColumn;
import static org.hibernate.cfg.AnnotatedJoinColumn.buildJoinColumn;
import static org.hibernate.cfg.BinderHelper.getMappedSuperclassOrNull;
import static org.hibernate.cfg.BinderHelper.getOverridableAnnotation;
import static org.hibernate.cfg.BinderHelper.hasToOneAnnotation;
import static org.hibernate.cfg.BinderHelper.isEmptyAnnotationValue;
import static org.hibernate.cfg.BinderHelper.makeIdGenerator;
import static org.hibernate.cfg.BinderHelper.toAliasEntityMap;
import static org.hibernate.cfg.BinderHelper.toAliasTableMap;
import static org.hibernate.cfg.InheritanceState.getInheritanceStateOfSuperEntity;
import static org.hibernate.cfg.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.fromExternalName;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.mapping.SimpleValue.DEFAULT_ID_GEN_STRATEGY;


/**
 * Stateful holder and processor for binding Entity information
 *
 * @author Emmanuel Bernard
 */
public class EntityBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, EntityBinder.class.getName() );
	private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";

	private MetadataBuildingContext context;

	private String name;
	private XClass annotatedClass;
	private PersistentClass persistentClass;
	private String discriminatorValue = "";
	private Boolean forceDiscriminator;
	private Boolean insertableDiscriminator;
	private boolean dynamicInsert;
	private boolean dynamicUpdate;
	private OptimisticLockType optimisticLockType;
	private PolymorphismType polymorphismType;
	private boolean selectBeforeUpdate;
	private int batchSize;
	private boolean lazy;
	private XClass proxyClass;
	private String where;
	// todo : we should defer to InFlightMetadataCollector.EntityTableXref for secondary table tracking;
	//		atm we use both from here; HBM binding solely uses InFlightMetadataCollector.EntityTableXref
	private final java.util.Map<String, Join> secondaryTables = new HashMap<>();
	private final java.util.Map<String, Object> secondaryTableJoins = new HashMap<>();
	private final java.util.Map<String, Join> secondaryTablesFromAnnotation = new HashMap<>();
	private final java.util.Map<String, Object> secondaryTableFromAnnotationJoins = new HashMap<>();

	private final List<Filter> filters = new ArrayList<>();
	private boolean ignoreIdAnnotations;
	private AccessType propertyAccessType = AccessType.DEFAULT;
	private boolean wrapIdsInEmbeddedComponents;
	private String subselect;

	private boolean isCached;
	private String cacheConcurrentStrategy;
	private String cacheRegion;
	private boolean cacheLazyProperty;
	private String naturalIdCacheRegion;

	/**
	 * Bind an entity class. This can be done in a single pass.
	 */
	public static void bindEntityClass(
			XClass clazzToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			HashMap<String, IdentifierGeneratorDefinition> classGenerators,
			MetadataBuildingContext context) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding entity from annotated class: %s", clazzToProcess.getName() );
		}

		//TODO: be more strict with secondary table allowance (not for ids, not for secondary table join columns etc)
		final InheritanceState inheritanceState = inheritanceStatePerClass.get( clazzToProcess );
		final PersistentClass superEntity = getSuperEntity( clazzToProcess, inheritanceStatePerClass, context, inheritanceState );
		detectedAttributeOverrideProblem(clazzToProcess, superEntity );

		final PersistentClass persistentClass = makePersistentClass( inheritanceState, superEntity, context);
		final EntityBinder entityBinder = new EntityBinder( clazzToProcess, persistentClass, context );

		final AnnotatedJoinColumns inheritanceJoinedColumns =
				makeInheritanceJoinColumns( clazzToProcess, context, inheritanceState, superEntity );
		final AnnotatedDiscriminatorColumn discriminatorColumn =
				handleDiscriminatorColumn( clazzToProcess, context, inheritanceState, entityBinder );

		entityBinder.setProxy( clazzToProcess.getAnnotation( Proxy.class ) );
		entityBinder.setBatchSize( clazzToProcess.getAnnotation( BatchSize.class ) );
		entityBinder.setWhere( getOverridableAnnotation( clazzToProcess, Where.class, context) );
		entityBinder.applyCaching( clazzToProcess, context.getBuildingOptions().getSharedCacheMode(), context );

		bindFiltersAndFilterDefs( clazzToProcess, entityBinder, context );

		entityBinder.bindEntity();

		jakarta.persistence.Table table = handleClassTable(
				clazzToProcess,
				context,
				inheritanceState,
				superEntity,
				entityBinder
		);

		PropertyHolder propertyHolder = buildPropertyHolder(
				clazzToProcess,
				persistentClass,
				entityBinder,
				context,
				inheritanceStatePerClass
		);

		handleSecondaryTables(clazzToProcess, entityBinder );

		handleInheritance(
				clazzToProcess,
				context,
				inheritanceState,
				persistentClass,
				entityBinder,
				inheritanceJoinedColumns,
				discriminatorColumn,
				propertyHolder
		);

		// check properties
		final InheritanceState.ElementsToProcess elementsToProcess = inheritanceState.getElementsToProcess();
		inheritanceState.postProcess( persistentClass, entityBinder );

		final Set<String> idPropertiesIfIdClass = handleIdClass(
				persistentClass,
				inheritanceState,
				context,
				entityBinder,
				propertyHolder,
				elementsToProcess,
				inheritanceStatePerClass
		);

		processIdPropertiesIfNotAlready(
				persistentClass,
				inheritanceState,
				context,
				entityBinder,
				propertyHolder,
				classGenerators,
				idPropertiesIfIdClass,
				elementsToProcess,
				inheritanceStatePerClass
		);

		if ( persistentClass instanceof RootClass ) {
			context.getMetadataCollector().addSecondPass( new CreateKeySecondPass( (RootClass) persistentClass ) );
		}

		if ( persistentClass instanceof Subclass) {
			assert superEntity != null;
			superEntity.addSubclass( (Subclass) persistentClass );
		}

		context.getMetadataCollector().addEntityBinding( persistentClass );

		//Process secondary tables and complementary definitions (ie o.h.a.Table)
		context.getMetadataCollector()
				.addSecondPass( new SecondaryTableFromAnnotationSecondPass( entityBinder, propertyHolder, clazzToProcess) );
		context.getMetadataCollector()
				.addSecondPass( new SecondaryTableSecondPass( entityBinder, propertyHolder, clazzToProcess) );

		processComplementaryTableDefinitions(clazzToProcess, entityBinder, table );

		bindCallbacks(clazzToProcess, persistentClass, context);
	}

	private static void processComplementaryTableDefinitions(XClass clazzToProcess, EntityBinder entityBinder, jakarta.persistence.Table tabAnn) {
		//add process complementary Table definition (index & all)
		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Table.class ) );
		entityBinder.processComplementaryTableDefinitions( clazzToProcess.getAnnotation( org.hibernate.annotations.Tables.class ) );
		entityBinder.processComplementaryTableDefinitions(tabAnn);
	}

	private static void detectedAttributeOverrideProblem(XClass clazzToProcess, PersistentClass superEntity) {
		if ( superEntity != null && (
				clazzToProcess.isAnnotationPresent( AttributeOverride.class ) ||
						clazzToProcess.isAnnotationPresent( AttributeOverrides.class ) ) ) {
			LOG.unsupportedAttributeOverrideWithEntityInheritance( clazzToProcess.getName() );
		}
	}

	private static Set<String> handleIdClass(
			PersistentClass persistentClass,
			InheritanceState inheritanceState,
			MetadataBuildingContext context,
			EntityBinder entityBinder,
			PropertyHolder propertyHolder,
			InheritanceState.ElementsToProcess elementsToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		Set<String> idPropertiesIfIdClass = new HashSet<>();
		boolean isIdClass = mapAsIdClass(
				inheritanceStatePerClass,
				inheritanceState,
				persistentClass,
				entityBinder,
				propertyHolder,
				elementsToProcess,
				idPropertiesIfIdClass,
				context
		);
		if ( !isIdClass ) {
			entityBinder.setWrapIdsInEmbeddedComponents( elementsToProcess.getIdPropertyCount() > 1 );
		}
		return idPropertiesIfIdClass;
	}

	private static boolean mapAsIdClass(
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			InheritanceState inheritanceState,
			PersistentClass persistentClass,
			EntityBinder entityBinder,
			PropertyHolder propertyHolder,
			InheritanceState.ElementsToProcess elementsToProcess,
			Set<String> idPropertiesIfIdClass,
			MetadataBuildingContext context) {

		// We are looking for @IdClass
		// In general we map the id class as identifier using the mapping metadata of the main entity's
		// properties and we create an identifier mapper containing the id properties of the main entity
		final XClass classWithIdClass = inheritanceState.getClassWithIdClass( false );
		if ( classWithIdClass != null ) {
			final IdClass idClass = classWithIdClass.getAnnotation( IdClass.class );
			@SuppressWarnings("unchecked")
			final XClass compositeClass = context.getBootstrapContext().getReflectionManager().toXClass( idClass.value() );
			final PropertyData inferredData = new PropertyPreloadedData(
					entityBinder.getPropertyAccessType(), "id", compositeClass
			);
			final PropertyData baseInferredData = new PropertyPreloadedData(
					entityBinder.getPropertyAccessType(), "id", classWithIdClass
			);
			AccessType propertyAccessor = entityBinder.getPropertyAccessor( compositeClass );

			// In JPA 2, there is a shortcut if the IdClass is the Pk of the associated class pointed to by the id
			// it ought to be treated as an embedded and not a real IdClass (at least in Hibernate's internal way)
			final boolean isFakeIdClass = isIdClassPkOfTheAssociatedEntity(
					elementsToProcess,
					compositeClass,
					inferredData,
					baseInferredData,
					propertyAccessor,
					inheritanceStatePerClass,
					context
			);

			if ( isFakeIdClass ) {
				return false;
			}

			final boolean ignoreIdAnnotations = entityBinder.isIgnoreIdAnnotations();
			entityBinder.setIgnoreIdAnnotations( true );
			propertyHolder.setInIdClass( true );
			bindIdClass(
					inferredData,
					baseInferredData,
					propertyHolder,
					propertyAccessor,
					entityBinder,
					context,
					inheritanceStatePerClass
			);
			propertyHolder.setInIdClass( null );
			final Component mapper = AnnotationBinder.fillComponent(
					propertyHolder,
					new PropertyPreloadedData(
							propertyAccessor,
							PropertyPath.IDENTIFIER_MAPPER_PROPERTY,
							compositeClass
					),
					baseInferredData,
					propertyAccessor,
					false,
					entityBinder,
					true,
					true,
					false,
					null,
					null,
					context,
					inheritanceStatePerClass
			);
			entityBinder.setIgnoreIdAnnotations( ignoreIdAnnotations );
			persistentClass.setIdentifierMapper( mapper );

			// If id definition is on a mapped superclass, update the mapping
			final org.hibernate.mapping.MappedSuperclass superclass = getMappedSuperclassOrNull(
					classWithIdClass,
					inheritanceStatePerClass,
					context
			);
			if ( superclass != null ) {
				superclass.setDeclaredIdentifierMapper( mapper );
			}
			else {
				// we are for sure on the entity
				persistentClass.setDeclaredIdentifierMapper( mapper );
			}

			final Property property = new Property();
			property.setName( PropertyPath.IDENTIFIER_MAPPER_PROPERTY );
			property.setUpdateable( false );
			property.setInsertable( false );
			property.setValue( mapper );
			property.setPropertyAccessorName( "embedded" );
			persistentClass.addProperty( property );
			entityBinder.setIgnoreIdAnnotations( true );

			for ( Property prop : mapper.getProperties() ) {
				idPropertiesIfIdClass.add( prop.getName() );
			}
			return true;
		}
		else {
			return false;
		}
	}

	private static boolean isIdClassPkOfTheAssociatedEntity(
			InheritanceState.ElementsToProcess elementsToProcess,
			XClass compositeClass,
			PropertyData inferredData,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context) {
		if ( elementsToProcess.getIdPropertyCount() == 1 ) {
			final PropertyData idPropertyOnBaseClass = AnnotationBinder.getUniqueIdPropertyFromBaseClass(
					inferredData,
					baseInferredData,
					propertyAccessor,
					context
			);
			final InheritanceState state = inheritanceStatePerClass.get( idPropertyOnBaseClass.getClassOrElement() );
			if ( state == null ) {
				return false; //while it is likely a user error, let's consider it is something that might happen
			}
			final XClass associatedClassWithIdClass = state.getClassWithIdClass( true );
			if ( associatedClassWithIdClass == null ) {
				//we cannot know for sure here unless we try and find the @EmbeddedId
				//Let's not do this thorough checking but do some extra validation
				return hasToOneAnnotation( idPropertyOnBaseClass.getProperty() );

			}
			else {
				IdClass idClass = associatedClassWithIdClass.getAnnotation(IdClass.class);
				//noinspection unchecked
				return context.getBootstrapContext().getReflectionManager().toXClass( idClass.value() )
						.equals( compositeClass );
			}
		}
		else {
			return false;
		}
	}

	private static void bindIdClass(
			PropertyData inferredData,
			PropertyData baseInferredData,
			PropertyHolder propertyHolder,
			AccessType propertyAccessor,
			EntityBinder entityBinder,
			MetadataBuildingContext buildingContext,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {

		// Fill simple value and property since and Id is a property
		final PersistentClass persistentClass = propertyHolder.getPersistentClass();
		if ( !(persistentClass instanceof RootClass) ) {
			throw new AnnotationException( "Entity '" + persistentClass.getEntityName()
					+ "' is a subclass in an entity inheritance hierarchy and may not redefine the identifier of the root entity" );
		}
		final RootClass rootClass = (RootClass) persistentClass;
		final Component id = AnnotationBinder.fillComponent(
				propertyHolder,
				inferredData,
				baseInferredData,
				propertyAccessor,
				false,
				entityBinder,
				true,
				false,
				false,
				null,
				null,
				buildingContext,
				inheritanceStatePerClass
		);
		id.setKey( true );
		if ( rootClass.getIdentifier() != null ) {
			throw new AssertionFailure( "Entity '" + persistentClass.getEntityName()
					+ "' has an '@IdClass' and may not have an identifier property" );
		}
		if ( id.getPropertySpan() == 0 ) {
			throw new AnnotationException( "Class '" + id.getComponentClassName()
					+ " is the '@IdClass' for the entity '" + persistentClass.getEntityName()
					+ "' but has no persistent properties" );
		}

		rootClass.setIdentifier( id );

		if ( buildingContext.getBootstrapContext().getJpaCompliance().isGlobalGeneratorScopeEnabled() ) {
			SecondPass secondPass = new IdGeneratorResolverSecondPass(
					id,
					inferredData.getProperty(),
					DEFAULT_ID_GEN_STRATEGY,
					"",
					buildingContext
			);
			buildingContext.getMetadataCollector().addSecondPass( secondPass );
		}
		else {
			makeIdGenerator(
					id,
					inferredData.getProperty(),
					DEFAULT_ID_GEN_STRATEGY,
					"",
					buildingContext,
					Collections.emptyMap()
			);
		}

		rootClass.setEmbeddedIdentifier( inferredData.getPropertyClass() == null );
	}

	private static AnnotatedDiscriminatorColumn handleDiscriminatorColumn(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			EntityBinder entityBinder) {

		switch ( inheritanceState.getType() ) {
			case SINGLE_TABLE:
				return processSingleTableDiscriminatorProperties(
						clazzToProcess,
						context,
						inheritanceState,
						entityBinder
				);
			case JOINED:
				return processJoinedDiscriminatorProperties(
						clazzToProcess,
						context,
						inheritanceState,
						entityBinder
				);
			default:
				return null;
		}
	}

	private static void handleSecondaryTables(XClass clazzToProcess, EntityBinder entityBinder) {
		SecondaryTable secTable = clazzToProcess.getAnnotation( SecondaryTable.class );
		SecondaryTables secTables = clazzToProcess.getAnnotation( SecondaryTables.class );
		if ( secTables != null ) {
			//loop through it
			for ( SecondaryTable tab : secTables.value() ) {
				entityBinder.addJoin( tab, null, false );
			}
		}
		else if ( secTable != null ) {
			entityBinder.addJoin( secTable, null, false );
		}
	}

	private static jakarta.persistence.Table handleClassTable(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			PersistentClass superEntity,
			EntityBinder entityBinder) {

		List<UniqueConstraintHolder> uniqueConstraints = new ArrayList<>();
		jakarta.persistence.Table tableAnnotation;
		String schema;
		String table;
		String catalog;
		boolean hasTableAnnotation = clazzToProcess.isAnnotationPresent( jakarta.persistence.Table.class );
		if ( hasTableAnnotation ) {
			tableAnnotation = clazzToProcess.getAnnotation( jakarta.persistence.Table.class );
			table = tableAnnotation.name();
			schema = tableAnnotation.schema();
			catalog = tableAnnotation.catalog();
			uniqueConstraints = TableBinder.buildUniqueConstraintHolders( tableAnnotation.uniqueConstraints() );
		}
		else {
			//might be no @Table annotation on the annotated class
			tableAnnotation = null;
			schema = "";
			table = "";
			catalog = "";
		}

		if ( inheritanceState.hasTable() ) {
			Check checkAnn = getOverridableAnnotation( clazzToProcess, Check.class, context );
			String constraints = checkAnn == null
					? null
					: checkAnn.constraints();

			InFlightMetadataCollector.EntityTableXref denormalizedTableXref = inheritanceState.hasDenormalizedTable()
					? context.getMetadataCollector().getEntityTableXref( superEntity.getEntityName() )
					: null;

			entityBinder.bindTable(
					schema,
					catalog,
					table,
					uniqueConstraints,
					constraints,
					denormalizedTableXref
			);
		}
		else {
			if ( hasTableAnnotation ) {
				LOG.invalidTableAnnotation( clazzToProcess.getName() );
			}

			if ( inheritanceState.getType() == InheritanceType.SINGLE_TABLE ) {
				// we at least need to properly set up the EntityTableXref
				entityBinder.bindTableForDiscriminatedSubclass(
						context.getMetadataCollector().getEntityTableXref( superEntity.getEntityName() )
				);
			}
		}

		return tableAnnotation;
	}

	private static void handleInheritance(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			PersistentClass persistentClass,
			EntityBinder entityBinder,
			AnnotatedJoinColumns inheritanceJoinedColumns,
			AnnotatedDiscriminatorColumn discriminatorColumn,
			PropertyHolder propertyHolder) {

		OnDelete onDeleteAnn = clazzToProcess.getAnnotation( OnDelete.class );

		// todo : sucks that this is separate from RootClass distinction
		final boolean isInheritanceRoot = !inheritanceState.hasParents();
		final boolean hasSubclasses = inheritanceState.hasSiblings();

		boolean onDeleteAppropriate = false;

		switch ( inheritanceState.getType() ) {
			case JOINED:
				if ( inheritanceState.hasParents() ) {
					onDeleteAppropriate = true;
					JoinedSubclass jsc = (JoinedSubclass) persistentClass;
					DependantValue key = new DependantValue( context, jsc.getTable(), jsc.getIdentifier() );
					jsc.setKey( key );
					handleForeignKeys( clazzToProcess, context, key );
					key.setCascadeDeleteEnabled( onDeleteAnn != null && OnDeleteAction.CASCADE == onDeleteAnn.action() );
					//we are never in a second pass at that stage, so queue it
					context.getMetadataCollector()
							.addSecondPass( new JoinedSubclassFkSecondPass( jsc, inheritanceJoinedColumns, key, context) );
					context.getMetadataCollector()
							.addSecondPass( new CreateKeySecondPass( jsc ) );
				}

				if ( isInheritanceRoot ) {
					// the class we are processing is the root of the hierarchy, see if we had a discriminator column
					// (it is perfectly valid for joined subclasses to not have discriminators).
					if ( discriminatorColumn != null ) {
						// we have a discriminator column
						if ( hasSubclasses || !discriminatorColumn.isImplicit() ) {
							bindDiscriminatorColumnToRootPersistentClass(
									(RootClass) persistentClass,
									discriminatorColumn,
									entityBinder.getSecondaryTables(),
									propertyHolder,
									context
							);
							//bind it again since the type might have changed
							entityBinder.bindDiscriminatorValue();
						}
					}
				}
				break;
			case SINGLE_TABLE:
				if ( isInheritanceRoot ) {
					if ( hasSubclasses || discriminatorColumn != null && !discriminatorColumn.isImplicit() ) {
						bindDiscriminatorColumnToRootPersistentClass(
								(RootClass) persistentClass,
								discriminatorColumn,
								entityBinder.getSecondaryTables(),
								propertyHolder,
								context
						);
						//bind it again since the type might have changed
						entityBinder.bindDiscriminatorValue();
					}
				}
				break;
		}

		if ( onDeleteAnn != null && !onDeleteAppropriate ) {
			LOG.invalidOnDeleteAnnotation( propertyHolder.getEntityName() );
		}
	}

	private static void handleForeignKeys(XClass clazzToProcess, MetadataBuildingContext context, DependantValue key) {
		ForeignKey foreignKey = clazzToProcess.getAnnotation( ForeignKey.class );
		if ( foreignKey != null && !isEmptyAnnotationValue( foreignKey.name() ) ) {
			key.setForeignKeyName( foreignKey.name() );
		}
		else {
			PrimaryKeyJoinColumn pkJoinColumn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumn.class );
			PrimaryKeyJoinColumns pkJoinColumns = clazzToProcess.getAnnotation( PrimaryKeyJoinColumns.class );
			final boolean noConstraintByDefault = context.getBuildingOptions().isNoConstraintByDefault();
			if ( pkJoinColumns != null && ( pkJoinColumns.foreignKey().value() == ConstraintMode.NO_CONSTRAINT
					|| pkJoinColumns.foreignKey().value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) ) {
				// don't apply a constraint based on ConstraintMode
				key.disableForeignKey();
			}
			else if ( pkJoinColumns != null && !StringHelper.isEmpty( pkJoinColumns.foreignKey().name() ) ) {
				key.setForeignKeyName( pkJoinColumns.foreignKey().name() );
				if ( !isEmptyAnnotationValue( pkJoinColumns.foreignKey().foreignKeyDefinition() ) ) {
					key.setForeignKeyDefinition( pkJoinColumns.foreignKey().foreignKeyDefinition() );
				}
			}
			else if ( pkJoinColumn != null && ( pkJoinColumn.foreignKey().value() == ConstraintMode.NO_CONSTRAINT
					|| pkJoinColumn.foreignKey().value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) ) {
				// don't apply a constraint based on ConstraintMode
				key.disableForeignKey();
			}
			else if ( pkJoinColumn != null && !StringHelper.isEmpty( pkJoinColumn.foreignKey().name() ) ) {
				key.setForeignKeyName( pkJoinColumn.foreignKey().name() );
				if ( !isEmptyAnnotationValue( pkJoinColumn.foreignKey().foreignKeyDefinition() ) ) {
					key.setForeignKeyDefinition( pkJoinColumn.foreignKey().foreignKeyDefinition() );
				}
			}
		}
	}

	private static void bindDiscriminatorColumnToRootPersistentClass(
			RootClass rootClass,
			AnnotatedDiscriminatorColumn discriminatorColumn,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context) {
		if ( rootClass.getDiscriminator() == null ) {
			if ( discriminatorColumn == null ) {
				throw new AssertionFailure( "discriminator column should have been built" );
			}
			discriminatorColumn.setJoins( secondaryTables );
			discriminatorColumn.setPropertyHolder( propertyHolder );
			BasicValue discriminatorColumnBinding = new BasicValue( context, rootClass.getTable() );
			rootClass.setDiscriminator( discriminatorColumnBinding );
			discriminatorColumn.linkWithValue( discriminatorColumnBinding );
			discriminatorColumnBinding.setTypeName( discriminatorColumn.getDiscriminatorTypeName() );
			rootClass.setPolymorphic( true );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Setting discriminator for entity {0}", rootClass.getEntityName() );
			}
			context.getMetadataCollector().addSecondPass(
					new NullableDiscriminatorColumnSecondPass( rootClass.getEntityName() ) );
		}
	}

	/**
	 * Process all discriminator-related metadata per rules for "single table" inheritance
	 */
	private static AnnotatedDiscriminatorColumn processSingleTableDiscriminatorProperties(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			EntityBinder entityBinder) {

		DiscriminatorColumn discAnn = clazzToProcess.getAnnotation( DiscriminatorColumn.class );
		DiscriminatorType discriminatorType = discAnn != null ? discAnn.discriminatorType() : DiscriminatorType.STRING;

		DiscriminatorFormula discFormulaAnn = getOverridableAnnotation( clazzToProcess, DiscriminatorFormula.class, context );

		final boolean isRoot = !inheritanceState.hasParents();
		final AnnotatedDiscriminatorColumn discriminatorColumn = isRoot
				? buildDiscriminatorColumn( discriminatorType, discAnn, discFormulaAnn, context )
				: null;
		if ( discAnn != null && !isRoot ) {
			//TODO: shouldn't this be an error?!
			LOG.invalidDiscriminatorAnnotation( clazzToProcess.getName() );
		}

		final String discriminatorValue = clazzToProcess.isAnnotationPresent( DiscriminatorValue.class )
				? clazzToProcess.getAnnotation( DiscriminatorValue.class ).value()
				: null;
		entityBinder.setDiscriminatorValue( discriminatorValue );

		DiscriminatorOptions discriminatorOptions = clazzToProcess.getAnnotation( DiscriminatorOptions.class );
		if ( discriminatorOptions != null) {
			entityBinder.setForceDiscriminator( discriminatorOptions.force() );
			entityBinder.setInsertableDiscriminator( discriminatorOptions.insert() );
		}

		return discriminatorColumn;
	}

	/**
	 * Process all discriminator-related metadata per rules for "joined" inheritance
	 */
	private static AnnotatedDiscriminatorColumn processJoinedDiscriminatorProperties(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			EntityBinder entityBinder) {
		if ( clazzToProcess.isAnnotationPresent( DiscriminatorFormula.class ) ) {
			throw new MappingException( "@DiscriminatorFormula on joined inheritance not supported at this time" );
		}


		// DiscriminatorValue handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final DiscriminatorValue discriminatorValueAnnotation = clazzToProcess.getAnnotation( DiscriminatorValue.class );
		final String discriminatorValue = discriminatorValueAnnotation != null
				? clazzToProcess.getAnnotation( DiscriminatorValue.class ).value()
				: null;
		entityBinder.setDiscriminatorValue( discriminatorValue );


		// DiscriminatorColumn handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final DiscriminatorColumn discriminatorColumnAnnotation = clazzToProcess.getAnnotation( DiscriminatorColumn.class );
		if ( !inheritanceState.hasParents() ) {
			// we want to process the discriminator column if either:
			//		1) There is an explicit DiscriminatorColumn annotation && we are not told to ignore them
			//		2) There is not an explicit DiscriminatorColumn annotation && we are told to create them implicitly
			final boolean generateDiscriminatorColumn;
			if ( discriminatorColumnAnnotation != null ) {
				generateDiscriminatorColumn = !context.getBuildingOptions().ignoreExplicitDiscriminatorsForJoinedInheritance();
				if (generateDiscriminatorColumn) {
					LOG.applyingExplicitDiscriminatorColumnForJoined(
							clazzToProcess.getName(),
							AvailableSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
					);
				}
				else {
					LOG.debugf( "Ignoring explicit DiscriminatorColumn annotation on: %s", clazzToProcess.getName() );
				}
			}
			else {
				generateDiscriminatorColumn = context.getBuildingOptions().createImplicitDiscriminatorsForJoinedInheritance();
				if ( generateDiscriminatorColumn ) {
					LOG.debug( "Applying implicit DiscriminatorColumn using DiscriminatorColumn defaults" );
				}
				else {
					LOG.debug( "Ignoring implicit (absent) DiscriminatorColumn" );
				}
			}

			if ( generateDiscriminatorColumn ) {
				return buildDiscriminatorColumn(
						discriminatorColumnAnnotation != null
								? discriminatorColumnAnnotation.discriminatorType()
								: DiscriminatorType.STRING,
						discriminatorColumnAnnotation,
						null,
						context
				);
			}
		}
		else {
			if ( discriminatorColumnAnnotation != null ) {
				LOG.invalidDiscriminatorAnnotation( clazzToProcess.getName() );
			}
		}

		return null;
	}

	private static void processIdPropertiesIfNotAlready(
			PersistentClass persistentClass,
			InheritanceState inheritanceState,
			MetadataBuildingContext context,
			EntityBinder entityBinder,
			PropertyHolder propertyHolder,
			HashMap<String, IdentifierGeneratorDefinition> classGenerators,
			Set<String> idPropertiesIfIdClass,
			InheritanceState.ElementsToProcess elementsToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {

		final Set<String> missingIdProperties = new HashSet<>( idPropertiesIfIdClass );
		for ( PropertyData propertyAnnotatedElement : elementsToProcess.getElements() ) {
			String propertyName = propertyAnnotatedElement.getPropertyName();
			if ( !idPropertiesIfIdClass.contains( propertyName ) ) {
				boolean subclassAndSingleTableStrategy =
						inheritanceState.getType() == InheritanceType.SINGLE_TABLE
								&& inheritanceState.hasParents();
				AnnotationBinder.processElementAnnotations(
						propertyHolder,
						subclassAndSingleTableStrategy
								? Nullability.FORCED_NULL
								: Nullability.NO_CONSTRAINT,
						propertyAnnotatedElement,
						classGenerators,
						entityBinder,
						false,
						false,
						false,
						context,
						inheritanceStatePerClass
				);
			}
			else {
				missingIdProperties.remove( propertyName );
			}
		}

		if ( missingIdProperties.size() != 0 ) {
			final StringBuilder missings = new StringBuilder();
			for ( String property : missingIdProperties ) {
				if ( missings.length() > 0 ) {
					missings.append(", ");
				}
				missings.append("'").append( property ).append( "'" );
			}
			throw new AnnotationException( "Entity '" + persistentClass.getEntityName()
					+ "' has an '@IdClass' with properties " + missings
					+ " which do not match properties of the entity class" );
		}
	}

	private static PersistentClass makePersistentClass(
			InheritanceState inheritanceState,
			PersistentClass superEntity,
			MetadataBuildingContext metadataBuildingContext) {
		//we now know what kind of persistent entity it is
		if ( !inheritanceState.hasParents() ) {
			return new RootClass( metadataBuildingContext );
		}
		else {
			switch ( inheritanceState.getType() ) {
				case SINGLE_TABLE:
					return new SingleTableSubclass( superEntity, metadataBuildingContext );
				case JOINED:
					return new JoinedSubclass( superEntity, metadataBuildingContext );
				case TABLE_PER_CLASS:
					return new UnionSubclass( superEntity, metadataBuildingContext );
				default:
					throw new AssertionFailure( "Unknown inheritance type: " + inheritanceState.getType() );
			}
		}
	}

	private static AnnotatedJoinColumns makeInheritanceJoinColumns(
			XClass clazzToProcess,
			MetadataBuildingContext context,
			InheritanceState inheritanceState,
			PersistentClass superEntity) {

		AnnotatedJoinColumn[] inheritanceJoinedColumns = null;
		final boolean hasJoinedColumns = inheritanceState.hasParents()
				&& InheritanceType.JOINED == inheritanceState.getType();
		if ( hasJoinedColumns ) {
			//@Inheritance(JOINED) subclass need to link back to the super entity
			final PrimaryKeyJoinColumns jcsAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumns.class );
			boolean explicitInheritanceJoinedColumns = jcsAnn != null && jcsAnn.value().length != 0;
			if ( explicitInheritanceJoinedColumns ) {
				int nbrOfInhJoinedColumns = jcsAnn.value().length;
				PrimaryKeyJoinColumn jcAnn;
				inheritanceJoinedColumns = new AnnotatedJoinColumn[nbrOfInhJoinedColumns];
				for ( int colIndex = 0; colIndex < nbrOfInhJoinedColumns; colIndex++ ) {
					jcAnn = jcsAnn.value()[colIndex];
					inheritanceJoinedColumns[colIndex] = buildJoinColumn(
							jcAnn,
							null,
							superEntity.getIdentifier(),
							null,
							null,
							context
					);
				}
			}
			else {
				final PrimaryKeyJoinColumn jcAnn = clazzToProcess.getAnnotation( PrimaryKeyJoinColumn.class );
				inheritanceJoinedColumns = new AnnotatedJoinColumn[1];
				inheritanceJoinedColumns[0] = buildJoinColumn(
						jcAnn,
						null,
						superEntity.getIdentifier(),
						null,
						null,
						context
				);
			}
			LOG.trace( "Subclass joined column(s) created" );
		}
		else {
			if ( clazzToProcess.isAnnotationPresent( PrimaryKeyJoinColumns.class )
					|| clazzToProcess.isAnnotationPresent( PrimaryKeyJoinColumn.class ) ) {
				LOG.invalidPrimaryKeyJoinColumnAnnotation( clazzToProcess.getName() );
			}
		}
		if ( inheritanceJoinedColumns == null ) {
			return null;
		}
		else {
			final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
			joinColumns.setBuildingContext( context );
			joinColumns.setColumns( inheritanceJoinedColumns );
			return joinColumns;
		}
	}

	private static PersistentClass getSuperEntity(
			XClass clazzToProcess,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			MetadataBuildingContext context,
			InheritanceState inheritanceState) {
		InheritanceState superEntityState = getInheritanceStateOfSuperEntity(
				clazzToProcess, inheritanceStatePerClass
		);
		PersistentClass superEntity = superEntityState != null
				? context.getMetadataCollector().getEntityBinding( superEntityState.getClazz().getName() )
				: null;
		if ( superEntity == null ) {
			//check if superclass is not a potential persistent class
			if ( inheritanceState.hasParents() ) {
				throw new AssertionFailure(
						"Subclass has to be bound after its parent class: "
								+ superEntityState.getClazz().getName()
				);
			}
		}
		return superEntity;
	}

	private static void bindCallbacks(XClass entityClass, PersistentClass persistentClass,
									  MetadataBuildingContext context) {
		ReflectionManager reflectionManager = context.getBootstrapContext().getReflectionManager();

		for ( CallbackType callbackType : CallbackType.values() ) {
			persistentClass.addCallbackDefinitions( CallbackDefinitionResolverLegacyImpl.resolveEntityCallbacks(
					reflectionManager, entityClass, callbackType ) );
		}

		context.getMetadataCollector().addSecondPass( persistentClasses -> {
			for ( Property property : persistentClass.getDeclaredProperties() ) {
				if ( property.isComposite() ) {
					for ( CallbackType callbackType : CallbackType.values() ) {
						property.addCallbackDefinitions( CallbackDefinitionResolverLegacyImpl.resolveEmbeddableCallbacks(
								reflectionManager, persistentClass.getMappedClass(), property, callbackType ) );
					}
				}
			}
		} );
	}

	public boolean wrapIdsInEmbeddedComponents() {
		return wrapIdsInEmbeddedComponents;
	}

	/**
	 * Use as a fake one for Collection of elements
	 */
	public EntityBinder() {
	}

	public EntityBinder(
			XClass annotatedClass,
			PersistentClass persistentClass,
			MetadataBuildingContext context) {
		this.context = context;
		this.persistentClass = persistentClass;
		this.annotatedClass = annotatedClass;
		bindEntityAnnotation();
		bindHibernateAnnotation();
	}

	/**
	 * For the most part, this is a simple delegation to {@link PersistentClass#isPropertyDefinedInHierarchy},
	 * after verifying that PersistentClass is indeed set here.
	 *
	 * @param name The name of the property to check
	 *
	 * @return {@code true} if a property by that given name does already exist in the super hierarchy.
	 */
	public boolean isPropertyDefinedInSuperHierarchy(String name) {
		// Yes, yes... persistentClass can be null because EntityBinder can be used
		// to bind components as well, of course...
		return  persistentClass != null && persistentClass.isPropertyDefinedInSuperHierarchy( name );
	}

	private void bindHibernateAnnotation() {
		final DynamicInsert dynamicInsertAnn = annotatedClass.getAnnotation( DynamicInsert.class );
		dynamicInsert = dynamicInsertAnn != null && dynamicInsertAnn.value();
		final DynamicUpdate dynamicUpdateAnn = annotatedClass.getAnnotation( DynamicUpdate.class );
		dynamicUpdate = dynamicUpdateAnn != null && dynamicUpdateAnn.value();
		final SelectBeforeUpdate selectBeforeUpdateAnn = annotatedClass.getAnnotation( SelectBeforeUpdate.class );
		selectBeforeUpdate = selectBeforeUpdateAnn != null && selectBeforeUpdateAnn.value();
		final OptimisticLocking optimisticLockingAnn = annotatedClass.getAnnotation( OptimisticLocking.class );
		optimisticLockType = optimisticLockingAnn == null ? OptimisticLockType.VERSION : optimisticLockingAnn.type();
		final Polymorphism polymorphismAnn = annotatedClass.getAnnotation( Polymorphism.class );
		polymorphismType = polymorphismAnn == null ? PolymorphismType.IMPLICIT : polymorphismAnn.type();
	}

	private void bindEntityAnnotation() {
		Entity entity = annotatedClass.getAnnotation( Entity.class );
		if ( entity == null ) {
			throw new AssertionFailure( "@Entity should never be missing" );
		}
		name = isEmptyAnnotationValue( entity.name() ) ? unqualify( annotatedClass.getName() ) : entity.name();
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

			boolean mutable = !annotatedClass.isAnnotationPresent( Immutable.class );
			rootClass.setMutable( mutable );
			rootClass.setExplicitPolymorphism( isExplicitPolymorphism( polymorphismType ) );

			if ( isNotEmpty( where ) ) {
				rootClass.setWhere( where );
			}

			if ( cacheConcurrentStrategy != null ) {
				rootClass.setCacheConcurrencyStrategy( cacheConcurrentStrategy );
				rootClass.setCacheRegionName( cacheRegion );
				rootClass.setLazyPropertiesCacheable( cacheLazyProperty );
			}

			rootClass.setNaturalIdCacheRegionName( naturalIdCacheRegion );

			boolean forceDiscriminatorInSelects = forceDiscriminator == null
					? context.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect()
					: forceDiscriminator;

			rootClass.setForceDiscriminator( forceDiscriminatorInSelects );

			if ( insertableDiscriminator != null ) {
				rootClass.setDiscriminatorInsertable( insertableDiscriminator );
			}
		}
		else {
			if ( annotatedClass.isAnnotationPresent(Immutable.class) ) {
				LOG.immutableAnnotationOnNonRoot( annotatedClass.getName() );
			}
		}

		persistentClass.setCached( isCached );

		persistentClass.setOptimisticLockStyle( getVersioning( optimisticLockType ) );
		persistentClass.setSelectBeforeUpdate( selectBeforeUpdate );

		bindCustomPersister();

		persistentClass.setBatchSize( batchSize );

		bindCustomSql();
		bindSynchronize();
		bindhandleFilters();

		LOG.debugf( "Import with entity name %s", name );
		try {
			context.getMetadataCollector().addImport( name, persistentClass.getEntityName() );
			String entityName = persistentClass.getEntityName();
			if ( !entityName.equals( name ) ) {
				context.getMetadataCollector().addImport( entityName, entityName );
			}
		}
		catch (MappingException me) {
			throw new AnnotationException( "Use of the same entity name twice: " + name, me );
		}

		processNamedEntityGraphs();
	}

	private void bindCustomSql() {
		//SQL overriding
		SQLInsert sqlInsert = annotatedClass.getAnnotation( SQLInsert.class );
		if ( sqlInsert != null ) {
			persistentClass.setCustomSQLInsert(
					sqlInsert.sql().trim(),
					sqlInsert.callable(),
					fromExternalName( sqlInsert.check().toString().toLowerCase(Locale.ROOT) )
			);

		}

		SQLUpdate sqlUpdate = annotatedClass.getAnnotation( SQLUpdate.class );
		if ( sqlUpdate != null ) {
			persistentClass.setCustomSQLUpdate(
					sqlUpdate.sql().trim(),
					sqlUpdate.callable(),
					fromExternalName( sqlUpdate.check().toString().toLowerCase(Locale.ROOT) )
			);
		}

		SQLDelete sqlDelete = annotatedClass.getAnnotation( SQLDelete.class );
		if ( sqlDelete != null ) {
			persistentClass.setCustomSQLDelete(
					sqlDelete.sql().trim(),
					sqlDelete.callable(),
					fromExternalName( sqlDelete.check().toString().toLowerCase(Locale.ROOT) )
			);
		}

		SQLDeleteAll sqlDeleteAll = annotatedClass.getAnnotation( SQLDeleteAll.class );
		if ( sqlDeleteAll != null ) {
			persistentClass.setCustomSQLDelete(
					sqlDeleteAll.sql().trim(),
					sqlDeleteAll.callable(),
					fromExternalName( sqlDeleteAll.check().toString().toLowerCase(Locale.ROOT) )
			);
		}

		Loader loader = annotatedClass.getAnnotation( Loader.class );
		if ( loader != null ) {
			persistentClass.setLoaderName( loader.namedQuery() );
		}

		Subselect subselect = annotatedClass.getAnnotation( Subselect.class );
		if ( subselect != null ) {
			this.subselect = subselect.value();
		}
	}

	private void bindhandleFilters() {
		for ( Filter filter : filters ) {
			final String filterName = filter.name();
			String condition = filter.condition();
			if ( isEmptyAnnotationValue( condition ) ) {
				final FilterDefinition definition = context.getMetadataCollector().getFilterDefinition( filterName );
				if ( definition == null ) {
					throw new AnnotationException( "Entity '" + name
							+ "' has a '@Filter' for an undefined filter named '" + filterName + "'" );
				}
				condition = definition.getDefaultFilterCondition();
				if ( isEmpty( condition ) ) {
					throw new AnnotationException( "Entity '" + name +
							"' has a '@Filter' with no 'condition' and no default condition was given by the '@FilterDef' named '"
							+ filterName + "'" );
				}
			}
			persistentClass.addFilter(
					filterName,
					condition,
					filter.deduceAliasInjectionPoints(),
					toAliasTableMap( filter.aliases() ),
					toAliasEntityMap( filter.aliases() )
			);
		}
	}

	private void bindSynchronize() {
		if ( annotatedClass.isAnnotationPresent( Synchronize.class ) ) {
			final JdbcEnvironment jdbcEnvironment = context.getMetadataCollector().getDatabase().getJdbcEnvironment();
			for ( String table : annotatedClass.getAnnotation(Synchronize.class).value() ) {
				persistentClass.addSynchronizedTable(
						context.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
								jdbcEnvironment.getIdentifierHelper().toIdentifier( table ),
								jdbcEnvironment
						).render( jdbcEnvironment.getDialect() )
				);
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void bindCustomPersister() {
		//set persister if needed
		Persister persisterAnn = annotatedClass.getAnnotation( Persister.class );
		if ( persisterAnn != null ) {
			Class clazz = persisterAnn.impl();
			if ( !EntityPersister.class.isAssignableFrom(clazz) ) {
				throw new AnnotationException( "Persister class '" + clazz.getName()
						+ "'  does not implement EntityPersister" );
			}
			persistentClass.setEntityPersisterClass( clazz );
		}
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
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
		context.getMetadataCollector().addNamedEntityGraph(
				new NamedEntityGraphDefinition( annotation, name, persistentClass.getEntityName() )
		);
	}

	public void bindDiscriminatorValue() {
		if ( isEmpty( discriminatorValue ) ) {
			Value discriminator = persistentClass.getDiscriminator();
			if ( discriminator == null ) {
				persistentClass.setDiscriminatorValue( name );
			}
			else if ( "character".equals( discriminator.getType().getName() ) ) {
				throw new AnnotationException( "Entity '" + name
						+ "' has a discriminator of character type and must specify its '@DiscriminatorValue'" );
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

	public void setProxy(Proxy proxy) {
		if ( proxy != null ) {
			lazy = proxy.lazy();
			if ( !lazy ) {
				proxyClass = null;
			}
			else {
				final ReflectionManager reflectionManager = context.getBootstrapContext().getReflectionManager();
				proxyClass = AnnotationBinder.isDefault( reflectionManager.toXClass(proxy.proxyClass() ), context )
						? annotatedClass
						: reflectionManager.toXClass(proxy.proxyClass());
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

	public void applyCaching(
			XClass clazzToProcess,
			SharedCacheMode sharedCacheMode,
			MetadataBuildingContext context) {
		bindCache( clazzToProcess, sharedCacheMode, context );
		bindNaturalIdCache( clazzToProcess );
	}

	private void bindNaturalIdCache(XClass clazzToProcess) {
		naturalIdCacheRegion = null;
		final NaturalIdCache naturalIdCacheAnn = clazzToProcess.getAnnotation( NaturalIdCache.class );
		if ( naturalIdCacheAnn != null ) {
			if ( isEmptyAnnotationValue( naturalIdCacheAnn.region() ) ) {
				final Cache explicitCacheAnn = clazzToProcess.getAnnotation( Cache.class );
				naturalIdCacheRegion = explicitCacheAnn != null && isNotEmpty( explicitCacheAnn.region() )
						? explicitCacheAnn.region() + NATURAL_ID_CACHE_SUFFIX
						: clazzToProcess.getName() + NATURAL_ID_CACHE_SUFFIX;
			}
			else {
				naturalIdCacheRegion = naturalIdCacheAnn.region();
			}
		}
	}

	private void bindCache(XClass clazzToProcess, SharedCacheMode sharedCacheMode, MetadataBuildingContext context) {
		isCached = false;
		cacheConcurrentStrategy = null;
		cacheRegion = null;
		cacheLazyProperty = true;
		if ( persistentClass instanceof RootClass ) {
			bindRootClassCache( clazzToProcess, sharedCacheMode, context );
		}
		else {
			bindSubclassCache( clazzToProcess, sharedCacheMode );
		}
	}

	private void bindSubclassCache(XClass clazzToProcess, SharedCacheMode sharedCacheMode) {
		final Cache cache = clazzToProcess.getAnnotation( Cache.class );
		final Cacheable cacheable = clazzToProcess.getAnnotation( Cacheable.class );
		if ( cache != null ) {
			LOG.cacheOrCacheableAnnotationOnNonRoot(
					persistentClass.getClassName() == null
							? annotatedClass.getName()
							: persistentClass.getClassName()
			);
		}
		else if ( cacheable == null && persistentClass.getSuperclass() != null ) {
			// we should inherit our super's caching config
			isCached = persistentClass.getSuperclass().isCached();
		}
		else {
			isCached = isCacheable( sharedCacheMode, cacheable );
		}
	}

	private void bindRootClassCache(XClass clazzToProcess, SharedCacheMode sharedCacheMode, MetadataBuildingContext context) {
		final Cache cache = clazzToProcess.getAnnotation( Cache.class );
		final Cacheable cacheable = clazzToProcess.getAnnotation( Cacheable.class );
		final Cache effectiveCache;
		if ( cache != null ) {
			// preserve legacy behavior of circumventing SharedCacheMode when Hibernate's @Cache is used.
			isCached = true;
			effectiveCache = cache;
		}
		else {
			effectiveCache = buildCacheMock( clazzToProcess.getName(), context );
			isCached = isCacheable( sharedCacheMode, cacheable );
		}
		cacheConcurrentStrategy = resolveCacheConcurrencyStrategy( effectiveCache.usage() );
		cacheRegion = effectiveCache.region();
		cacheLazyProperty = isCacheLazy( effectiveCache, annotatedClass );
	}

	private static boolean isCacheLazy(Cache effectiveCache, XClass annotatedClass) {
		switch ( effectiveCache.include().toLowerCase( Locale.ROOT ) ) {
			case "all":
				return true;
			case "non-lazy":
				return false;
			default:
				throw new AnnotationException( "Class '" + annotatedClass.getName()
						+ "' has a '@Cache' with undefined option 'include=\"" + effectiveCache.include() + "\"'" );
		}
	}

	private static boolean isCacheable(SharedCacheMode sharedCacheMode, Cacheable explicitCacheableAnn) {
		switch (sharedCacheMode) {
			case ALL:
				// all entities should be cached
				return true;
			case ENABLE_SELECTIVE:
				// only entities with @Cacheable(true) should be cached
				return explicitCacheableAnn != null && explicitCacheableAnn.value();
			case DISABLE_SELECTIVE:
				// only entities with @Cacheable(false) should not be cached
				return explicitCacheableAnn == null || explicitCacheableAnn.value();
			default:
				// treat both NONE and UNSPECIFIED the same
				return false;
		}
	}

	private static String resolveCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		final org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	private static Cache buildCacheMock(String region, MetadataBuildingContext context) {
		return new LocalCacheAnnotationStub( region, determineCacheConcurrencyStrategy( context ) );
	}

	private static class LocalCacheAnnotationStub implements Cache {
		private final String region;
		private final CacheConcurrencyStrategy usage;

		private LocalCacheAnnotationStub(String region, CacheConcurrencyStrategy usage) {
			this.region = region;
			this.usage = usage;
		}

		public CacheConcurrencyStrategy usage() {
			return usage;
		}

		public String region() {
			return region;
		}

		public String include() {
			return "all";
		}

		public Class<? extends Annotation> annotationType() {
			return Cache.class;
		}
	}

	private static CacheConcurrencyStrategy determineCacheConcurrencyStrategy(MetadataBuildingContext context) {
		return CacheConcurrencyStrategy.fromAccessType( context.getBuildingOptions().getImplicitCacheAccessType() );
	}

	private static class EntityTableNamingStrategyHelper implements NamingStrategyHelper {
		private final String className;
		private final String entityName;
		private final String jpaEntityName;

		private EntityTableNamingStrategyHelper(String className, String entityName, String jpaEntityName) {
			this.className = className;
			this.entityName = entityName;
			this.jpaEntityName = jpaEntityName;
		}

		public Identifier determineImplicitName(final MetadataBuildingContext buildingContext) {
			return buildingContext.getBuildingOptions().getImplicitNamingStrategy().determinePrimaryTableName(
					new ImplicitEntityNameSource() {
						private final EntityNaming entityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return className;
							}

							@Override
							public String getEntityName() {
								return entityName;
							}

							@Override
							public String getJpaEntityName() {
								return jpaEntityName;
							}
						};

						@Override
						public EntityNaming getEntityNaming() {
							return entityNaming;
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return buildingContext;
						}
					}
			);
		}

		@Override
		public Identifier handleExplicitName(String explicitName, MetadataBuildingContext buildingContext) {
			return buildingContext.getMetadataCollector()
					.getDatabase()
					.getJdbcEnvironment()
					.getIdentifierHelper()
					.toIdentifier( explicitName );
		}

		@Override
		public Identifier toPhysicalName(Identifier logicalName, MetadataBuildingContext buildingContext) {
			return buildingContext.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
					logicalName,
					buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment()
			);
		}
	}

	public void bindTableForDiscriminatedSubclass(InFlightMetadataCollector.EntityTableXref superTableXref) {
		if ( !(persistentClass instanceof SingleTableSubclass) ) {
			throw new AssertionFailure(
					"Was expecting a discriminated subclass [" + SingleTableSubclass.class.getName() +
							"] but found [" + persistentClass.getClass().getName() + "] for entity [" +
							persistentClass.getEntityName() + "]"
			);
		}

		context.getMetadataCollector().addEntityTableXref(
				persistentClass.getEntityName(),
				context.getMetadataCollector().getDatabase().toIdentifier(
						context.getMetadataCollector().getLogicalTableName( superTableXref.getPrimaryTable() )
				),
				superTableXref.getPrimaryTable(),
				superTableXref
		);
	}

	public void bindTable(
			String schema,
			String catalog,
			String tableName,
			List<UniqueConstraintHolder> uniqueConstraints,
			String constraints,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {

		final EntityTableNamingStrategyHelper namingStrategyHelper = new EntityTableNamingStrategyHelper(
				persistentClass.getClassName(),
				persistentClass.getEntityName(),
				name
		);
		final Identifier logicalName = isNotEmpty( tableName )
				? namingStrategyHelper.handleExplicitName( tableName, context )
				: namingStrategyHelper.determineImplicitName( context );

		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				logicalName,
				persistentClass.isAbstract(),
				uniqueConstraints,
				null,
				constraints,
				context,
				subselect,
				denormalizedSuperTableXref
		);
		final RowId rowId = annotatedClass.getAnnotation( RowId.class );
		if ( rowId != null ) {
			table.setRowId( rowId.value() );
		}
		final Comment comment = annotatedClass.getAnnotation( Comment.class );
		if ( comment != null ) {
			table.setComment( comment.value() );
		}

		context.getMetadataCollector().addEntityTableXref(
				persistentClass.getEntityName(),
				logicalName,
				table,
				denormalizedSuperTableXref
		);

		if ( persistentClass instanceof TableOwner ) {
			LOG.debugf( "Bind entity %s on table %s", persistentClass.getEntityName(), table.getName() );
			( (TableOwner) persistentClass ).setTable( table );
		}
		else {
			throw new AssertionFailure( "binding a table for a subclass" );
		}
	}

	public void finalSecondaryTableBinding(PropertyHolder propertyHolder) {
		 // This operation has to be done after the id definition of the persistence class.
		 // ie after the properties parsing
		Iterator<Object> joinColumns = secondaryTableJoins.values().iterator();
		for ( Map.Entry<String, Join> entrySet : secondaryTables.entrySet() ) {
			if ( !secondaryTablesFromAnnotation.containsKey( entrySet.getKey() ) ) {
				createPrimaryColumnsToSecondaryTable( joinColumns.next(), propertyHolder, entrySet.getValue() );
			}
		}
	}

	public void finalSecondaryTableFromAnnotationBinding(PropertyHolder propertyHolder) {
		 // This operation has to be done before the end of the FK second pass processing in order
		 // to find the join columns belonging to secondary tables
		Iterator<Object> joinColumns = secondaryTableFromAnnotationJoins.values().iterator();
		for ( Map.Entry<String, Join> entrySet : secondaryTables.entrySet() ) {
			if ( secondaryTablesFromAnnotation.containsKey( entrySet.getKey() ) ) {
				createPrimaryColumnsToSecondaryTable( joinColumns.next(), propertyHolder, entrySet.getValue() );
			}
		}
	}

	private void createPrimaryColumnsToSecondaryTable(Object column, PropertyHolder propertyHolder, Join join) {
		final PrimaryKeyJoinColumn[] pkColumnsAnn = column instanceof PrimaryKeyJoinColumn[]
				? (PrimaryKeyJoinColumn[]) column
				: null;
		final JoinColumn[] joinColumnsAnn = column instanceof JoinColumn[]
				? (JoinColumn[]) column
				: null;
		final AnnotatedJoinColumns annotatedJoinColumns = pkColumnsAnn == null && joinColumnsAnn == null
				? createDefaultJoinColumn( propertyHolder )
				: createJoinColumns( propertyHolder, pkColumnsAnn, joinColumnsAnn );

		for ( AnnotatedJoinColumn joinColumn : annotatedJoinColumns.getColumns() ) {
			joinColumn.forceNotNull();
		}
		bindJoinToPersistentClass( join, annotatedJoinColumns, context );
	}

	private AnnotatedJoinColumns createDefaultJoinColumn(PropertyHolder propertyHolder) {
		final AnnotatedJoinColumn[] annotatedJoinColumns = new AnnotatedJoinColumn[1];
		annotatedJoinColumns[0] = buildJoinColumn(
				null,
				null,
				persistentClass.getIdentifier(),
				secondaryTables,
				propertyHolder,
				context
		);
		final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
		joinColumns.setBuildingContext( context );
		joinColumns.setPropertyHolder( propertyHolder );
		joinColumns.setColumns( annotatedJoinColumns );
		return joinColumns;
	}

	private AnnotatedJoinColumns createJoinColumns(
			PropertyHolder propertyHolder,
			PrimaryKeyJoinColumn[] pkColumnsAnn,
			JoinColumn[] joinColumnsAnn) {
		final int joinColumnCount = pkColumnsAnn != null ? pkColumnsAnn.length : joinColumnsAnn.length;
		if ( joinColumnCount == 0 ) {
			return createDefaultJoinColumn( propertyHolder );
		}
		else {
			final AnnotatedJoinColumn[] annotatedJoinColumns = new AnnotatedJoinColumn[joinColumnCount];
			for (int colIndex = 0; colIndex < joinColumnCount; colIndex++) {
				final PrimaryKeyJoinColumn pkJoinAnn = pkColumnsAnn != null ? pkColumnsAnn[colIndex] : null;
				final JoinColumn joinAnn = joinColumnsAnn != null ? joinColumnsAnn[colIndex] : null;
				annotatedJoinColumns[colIndex] = buildJoinColumn(
						pkJoinAnn,
						joinAnn,
						persistentClass.getIdentifier(),
						secondaryTables,
						propertyHolder,
						context
				);
			}
			final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
			joinColumns.setBuildingContext( context );
			joinColumns.setPropertyHolder( propertyHolder );
			joinColumns.setColumns( annotatedJoinColumns );
			return joinColumns;
		}
	}

	private void bindJoinToPersistentClass(Join join, AnnotatedJoinColumns joinColumns, MetadataBuildingContext context) {
		DependantValue key = new DependantValue( context, join.getTable(), persistentClass.getIdentifier() );
		join.setKey( key );
		setForeignKeyNameIfDefined( join );
		key.setCascadeDeleteEnabled( false );
		TableBinder.bindForeignKey( persistentClass, null, joinColumns, key, false, context );
		key.sortProperties();
		join.createPrimaryKey();
		join.createForeignKey();
		persistentClass.addJoin( join );
	}

	private void setForeignKeyNameIfDefined(Join join) {
		// just awful..
		org.hibernate.annotations.Table matchingTable = findMatchingComplementaryTableAnnotation( join );
		final SimpleValue key = (SimpleValue) join.getKey();
		if ( matchingTable != null && !isEmptyAnnotationValue( matchingTable.foreignKey().name() ) ) {
			key.setForeignKeyName( matchingTable.foreignKey().name() );
		}
		else {
			final SecondaryTable jpaSecondaryTable = findMatchingSecondaryTable( join );
			if ( jpaSecondaryTable != null ) {
				final boolean noConstraintByDefault = context.getBuildingOptions().isNoConstraintByDefault();
				if ( jpaSecondaryTable.foreignKey().value() == ConstraintMode.NO_CONSTRAINT
						|| jpaSecondaryTable.foreignKey().value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) {
					key.disableForeignKey();
				}
				else {
					key.setForeignKeyName( nullIfEmpty( jpaSecondaryTable.foreignKey().name() ) );
					key.setForeignKeyDefinition( nullIfEmpty( jpaSecondaryTable.foreignKey().foreignKeyDefinition() ) );
				}
			}
		}
	}

	private SecondaryTable findMatchingSecondaryTable(Join join) {
		final String nameToMatch = join.getTable().getQuotedName();
		final SecondaryTable secondaryTable = annotatedClass.getAnnotation( SecondaryTable.class );
		if ( secondaryTable != null && nameToMatch.equals( secondaryTable.name() ) ) {
			return secondaryTable;
		}
		final SecondaryTables secondaryTables = annotatedClass.getAnnotation( SecondaryTables.class );
		if ( secondaryTables != null ) {
			for ( SecondaryTable secondaryTablesEntry : secondaryTables.value() ) {
				if ( secondaryTablesEntry != null && nameToMatch.equals( secondaryTablesEntry.name() ) ) {
					return secondaryTablesEntry;
				}
			}
		}
		return null;
	}

	private org.hibernate.annotations.Table findMatchingComplementaryTableAnnotation(Join join) {
		final String tableName = join.getTable().getQuotedName();
		final org.hibernate.annotations.Table table = annotatedClass.getAnnotation( org.hibernate.annotations.Table.class );
		if ( table != null && tableName.equals( table.appliesTo() ) ) {
			return table;
		}
		else {
			Tables tables = annotatedClass.getAnnotation( Tables.class );
			if ( tables != null ) {
				for (org.hibernate.annotations.Table current : tables.value()) {
					if ( tableName.equals( current.appliesTo() ) ) {
						return current;
					}
				}
			}
			return null;
		}
	}

	private SecondaryRow findMatchingComplementarySecondaryRowAnnotation(Join join) {
		final String tableName = join.getTable().getQuotedName();
		final SecondaryRow row = annotatedClass.getAnnotation( SecondaryRow.class );
		if ( row != null && ( row.table().isEmpty() || tableName.equals( row.table() ) ) ) {
			return row;
		}
		else {
			SecondaryRows tables = annotatedClass.getAnnotation( SecondaryRows.class );
			if ( tables != null ) {
				for ( SecondaryRow current : tables.value() ) {
					if ( tableName.equals( current.table() ) ) {
						return current;
					}
				}
			}
			return null;
		}
	}

	//Used for @*ToMany @JoinTable
	public Join addJoin(JoinTable joinTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		return addJoin( null, joinTable, holder, noDelayInPkColumnCreation );
	}

	public Join addJoin(SecondaryTable secondaryTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		return addJoin( secondaryTable, null, holder, noDelayInPkColumnCreation );
	}

	private Join addJoin(
			SecondaryTable secondaryTable,
			JoinTable joinTable,
			PropertyHolder propertyHolder,
			boolean noDelayInPkColumnCreation) {
		// A non-null propertyHolder means than we process the Pk creation without delay
		final Join join = new Join();
		join.setPersistentClass( persistentClass );

		final String schema;
		final String catalog;
		final Object joinColumns;
		final List<UniqueConstraintHolder> uniqueConstraintHolders;

		final QualifiedTableName logicalName;
		if ( secondaryTable != null ) {
			schema = secondaryTable.schema();
			catalog = secondaryTable.catalog();
			logicalName = new QualifiedTableName(
				Identifier.toIdentifier( catalog ),
				Identifier.toIdentifier( schema ),
					context.getMetadataCollector()
					.getDatabase()
					.getJdbcEnvironment()
					.getIdentifierHelper()
					.toIdentifier( secondaryTable.name() )
			);
			joinColumns = secondaryTable.pkJoinColumns();
			uniqueConstraintHolders = TableBinder.buildUniqueConstraintHolders( secondaryTable.uniqueConstraints() );
		}
		else if ( joinTable != null ) {
			schema = joinTable.schema();
			catalog = joinTable.catalog();
			logicalName = new QualifiedTableName(
				Identifier.toIdentifier( catalog ),
				Identifier.toIdentifier( schema ),
				context.getMetadataCollector()
						.getDatabase()
						.getJdbcEnvironment()
						.getIdentifierHelper()
						.toIdentifier( joinTable.name() )
			);
			joinColumns = joinTable.joinColumns();
			uniqueConstraintHolders = TableBinder.buildUniqueConstraintHolders( joinTable.uniqueConstraints() );
		}
		else {
			throw new AssertionFailure( "Both JoinTable and SecondaryTable are null" );
		}

		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				logicalName.getTableName(),
				false,
				uniqueConstraintHolders,
				null,
				null,
				context,
				null,
				null
		);

		final InFlightMetadataCollector.EntityTableXref tableXref
				= context.getMetadataCollector().getEntityTableXref( persistentClass.getEntityName() );
		assert tableXref != null : "Could not locate EntityTableXref for entity [" + persistentClass.getEntityName() + "]";
		tableXref.addSecondaryTable( logicalName, join );

		if ( secondaryTable != null ) {
			TableBinder.addIndexes( table, secondaryTable.indexes(), context );
		}

			//no check constraints available on joins
		join.setTable( table );

		//somehow keep joins() for later.
		//Has to do the work later because it needs persistentClass id!
		LOG.debugf( "Adding secondary table to entity %s -> %s",
				persistentClass.getEntityName(), join.getTable().getName() );
		final SecondaryRow matchingRow = findMatchingComplementarySecondaryRowAnnotation( join );
		final org.hibernate.annotations.Table matchingTable = findMatchingComplementaryTableAnnotation( join );
		if ( matchingRow != null ) {
			join.setInverse( !matchingRow.owned() );
			join.setOptional( matchingRow.optional() );
		}
		else if ( matchingTable != null ) {
			join.setInverse( matchingTable.inverse() );
			join.setOptional( matchingTable.optional() );
			String insertSql = matchingTable.sqlInsert().sql();
			if ( !isEmptyAnnotationValue(insertSql) ) {
				join.setCustomSQLInsert(
						insertSql.trim(),
						matchingTable.sqlInsert().callable(),
						fromExternalName( matchingTable.sqlInsert().check().toString().toLowerCase(Locale.ROOT) )
				);
			}
			String updateSql = matchingTable.sqlUpdate().sql();
			if ( !isEmptyAnnotationValue(updateSql) ) {
				join.setCustomSQLUpdate(
						updateSql.trim(),
						matchingTable.sqlUpdate().callable(),
						fromExternalName( matchingTable.sqlUpdate().check().toString().toLowerCase(Locale.ROOT) )
				);
			}
			String deleteSql = matchingTable.sqlDelete().sql();
			if ( !isEmptyAnnotationValue(deleteSql) ) {
				join.setCustomSQLDelete(
						deleteSql.trim(),
						matchingTable.sqlDelete().callable(),
						fromExternalName( matchingTable.sqlDelete().check().toString().toLowerCase(Locale.ROOT) )
				);
			}
		}
		else {
			//default
			join.setInverse( false );
			join.setOptional( true ); //perhaps not quite per-spec, but a Good Thing anyway
		}

		if ( noDelayInPkColumnCreation ) {
			createPrimaryColumnsToSecondaryTable( joinColumns, propertyHolder, join );
		}
		else {
			final String quotedName = table.getQuotedName();
			if ( secondaryTable != null ) {
				secondaryTablesFromAnnotation.put( quotedName, join );
				secondaryTableFromAnnotationJoins.put( quotedName, joinColumns );
			}
			else {
				secondaryTableJoins.put( quotedName, joinColumns );
			}
			secondaryTables.put( quotedName, join );
		}

		return join;
	}

	public java.util.Map<String, Join> getSecondaryTables() {
		return secondaryTables;
	}

	public static String getCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		final org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	public void addFilter(Filter filter) {
		filters.add(filter);
	}

	public boolean isIgnoreIdAnnotations() {
		return ignoreIdAnnotations;
	}

	public void setIgnoreIdAnnotations(boolean ignoreIdAnnotations) {
		this.ignoreIdAnnotations = ignoreIdAnnotations;
	}

	public void processComplementaryTableDefinitions(jakarta.persistence.Table table) {
		if ( table != null ) {
			TableBinder.addIndexes( persistentClass.getTable(), table.indexes(), context );
		}
	}

	public void processComplementaryTableDefinitions(org.hibernate.annotations.Table table) {
		//comment and index are processed here
		if ( table == null ) return;
		final String appliedTable = table.appliesTo();
		Table hibTable = null;
		for ( Table pcTable : persistentClass.getTableClosure() ) {
			if ( pcTable.getQuotedName().equals( appliedTable ) ) {
				//we are in the correct table to find columns
				hibTable = pcTable;
				break;
			}
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
			throw new AnnotationException( "Entity '" + name
					+ "' has a '@org.hibernate.annotations.Table' annotation which 'appliesTo' an unknown table named '"
					+ appliedTable + "'" );
		}
		if ( !isEmptyAnnotationValue( table.comment() ) ) {
			hibTable.setComment( table.comment() );
		}
		if ( !isEmptyAnnotationValue( table.checkConstraint() ) ) {
			hibTable.addCheckConstraint( table.checkConstraint() );
		}
		TableBinder.addIndexes( hibTable, table.indexes(), context );
	}

	public void processComplementaryTableDefinitions(Tables tables) {
		if ( tables != null ) {
			for ( org.hibernate.annotations.Table table : tables.value() ) {
				processComplementaryTableDefinitions( table );
			}
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
		final Access access = element.getAnnotation( Access.class );
		if ( access != null ) {
			accessType = AccessType.getAccessStrategy( access.value() );
		}
		return accessType;
	}

	/**
	 * Process the filters defined on the given class, as well as all filters
	 * defined on the MappedSuperclass(es) in the inheritance hierarchy
	 */
	public static void bindFiltersAndFilterDefs(
			XClass annotatedClass,
			EntityBinder entityBinder,
			MetadataBuildingContext context) {

		bindFilters( annotatedClass, entityBinder, context );

		XClass classToProcess = annotatedClass.getSuperclass();
		while ( classToProcess != null ) {
			AnnotatedClassType classType = context.getMetadataCollector().getClassType( classToProcess );
			if ( AnnotatedClassType.MAPPED_SUPERCLASS == classType ) {
				bindFilters( classToProcess, entityBinder, context );
			}
			else {
				break;
			}
			classToProcess = classToProcess.getSuperclass();
		}
	}

	private static void bindFilters(XAnnotatedElement annotatedElement, EntityBinder entityBinder, MetadataBuildingContext context) {
		final Filters filtersAnn = getOverridableAnnotation( annotatedElement, Filters.class, context );
		if ( filtersAnn != null ) {
			for ( Filter filter : filtersAnn.value() ) {
				entityBinder.addFilter(filter);
			}
		}

		final Filter filterAnn = annotatedElement.getAnnotation( Filter.class );
		if ( filterAnn != null ) {
			entityBinder.addFilter(filterAnn);
		}
	}
}
