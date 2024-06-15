/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.SecondaryRows;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.annotations.View;
import org.hibernate.annotations.Where;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.internal.InheritanceState.ElementsToProcess;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.NamingStrategyHelper;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.spi.CustomSqlDetails;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jpa.event.internal.CallbackDefinitionResolver;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
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
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.spi.NavigablePath;

import org.jboss.logging.Logger;

import jakarta.persistence.Access;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Cacheable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.UniqueConstraint;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.hibernate.annotations.PolymorphismType.EXPLICIT;
import static org.hibernate.annotations.PolymorphismType.IMPLICIT;
import static org.hibernate.boot.model.internal.AnnotatedClassType.MAPPED_SUPERCLASS;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.buildDiscriminatorColumn;
import static org.hibernate.boot.model.internal.AnnotatedJoinColumn.buildInheritanceJoinColumn;
import static org.hibernate.boot.model.internal.BinderHelper.extractFromPackage;
import static org.hibernate.boot.model.internal.BinderHelper.getMappedSuperclassOrNull;
import static org.hibernate.boot.model.internal.BinderHelper.hasToOneAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.noConstraint;
import static org.hibernate.boot.model.internal.BinderHelper.toAliasEntityMap;
import static org.hibernate.boot.model.internal.BinderHelper.toAliasTableMap;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverrideAnnotation;
import static org.hibernate.boot.model.internal.EmbeddableBinder.fillEmbeddable;
import static org.hibernate.boot.model.internal.InheritanceState.getInheritanceStateOfSuperEntity;
import static org.hibernate.boot.model.internal.PropertyBinder.addElementsOfClass;
import static org.hibernate.boot.model.internal.PropertyBinder.hasIdAnnotation;
import static org.hibernate.boot.model.internal.PropertyBinder.processElementAnnotations;
import static org.hibernate.boot.model.internal.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.boot.model.internal.TableBinder.bindForeignKey;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.engine.OptimisticLockStyle.fromLockType;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.fromResultCheckStyle;
import static org.hibernate.internal.util.ReflectHelper.getDefaultSupplier;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;


/**
 * Stateful binder responsible for interpreting information about an {@link Entity} class
 * and producing a {@link PersistentClass} mapping model object.
 *
 * @author Emmanuel Bernard
 */
public class EntityBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, EntityBinder.class.getName() );
	private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";

	private MetadataBuildingContext context;

	private String name;
	private ClassDetails annotatedClass;
	private PersistentClass persistentClass;
	private PolymorphismType polymorphismType;
	private boolean lazy;
	private ClassDetails proxyClass;
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
	private CacheLayout queryCacheLayout;

	private SourceModelBuildingContext getSourceModelContext() {
		return context.getMetadataCollector().getSourceModelBuildingContext();
	}

	/**
	 * Bind an entity class. This can be done in a single pass.
	 */
	public static void bindEntityClass(
			ClassDetails clazzToProcess,
			Map<ClassDetails, InheritanceState> inheritanceStates,
			Map<String, IdentifierGeneratorDefinition> generators,
			MetadataBuildingContext context) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding entity from annotated class: %s", clazzToProcess.getName() );
		}

		//TODO: be more strict with secondary table allowance (not for ids, not for secondary table join columns etc)

		final InheritanceState inheritanceState = inheritanceStates.get( clazzToProcess );
		final PersistentClass superEntity = getSuperEntity( clazzToProcess, inheritanceStates, context, inheritanceState );
		final PersistentClass persistentClass = makePersistentClass( inheritanceState, superEntity, context );
		checkOverrides( clazzToProcess, superEntity, context.getMetadataCollector().getSourceModelBuildingContext() );

		final EntityBinder entityBinder = new EntityBinder( clazzToProcess, persistentClass, context );
		entityBinder.bindEntity();
		entityBinder.bindSubselect(); // has to happen before table binding
		entityBinder.bindTables( inheritanceState, superEntity );
		entityBinder.bindCustomSql(); // has to happen after table binding
		entityBinder.bindSynchronize();
		entityBinder.bindFilters();
		entityBinder.handleCheckConstraints();
		final PropertyHolder holder = buildPropertyHolder(
				clazzToProcess,
				persistentClass,
				entityBinder,
				context,
				inheritanceStates
		);
		entityBinder.handleInheritance( inheritanceState, superEntity, holder );
		entityBinder.handleIdentifier( holder, inheritanceStates, generators, inheritanceState );

		final InFlightMetadataCollector collector = context.getMetadataCollector();
		if ( persistentClass instanceof RootClass ) {
			collector.addSecondPass( new CreateKeySecondPass( (RootClass) persistentClass ) );
			bindSoftDelete( clazzToProcess, (RootClass) persistentClass, inheritanceState, context );
		}
		if ( persistentClass instanceof Subclass) {
			assert superEntity != null;
			superEntity.addSubclass( (Subclass) persistentClass );
		}
		collector.addEntityBinding( persistentClass );
		// process secondary tables and complementary definitions (ie o.h.a.Table)
		collector.addSecondPass( new SecondaryTableFromAnnotationSecondPass( entityBinder, holder ) );
		collector.addSecondPass( new SecondaryTableSecondPass( entityBinder, holder ) );
		// comment, checkConstraint, and indexes are processed here
		entityBinder.processComplementaryTableDefinitions();
		bindCallbacks( clazzToProcess, persistentClass, context );
		entityBinder.callTypeBinders( persistentClass );
	}

	private void bindTables(InheritanceState inheritanceState, PersistentClass superEntity) {
		handleClassTable( inheritanceState, superEntity );
		handleSecondaryTables();
	}

	private static void checkOverrides(ClassDetails clazzToProcess, PersistentClass superEntity, SourceModelBuildingContext sourceModelContext) {
		if ( superEntity != null ) {
			//TODO: correctly handle compound paths (embeddables)
			clazzToProcess.forEachAnnotationUsage( AttributeOverride.class, sourceModelContext, (usage) -> checkOverride(
					superEntity,
					usage.name(),
					clazzToProcess,
					AttributeOverride.class
			) );
			clazzToProcess.forEachAnnotationUsage( AssociationOverride.class, sourceModelContext, (usage) ->  checkOverride(
					superEntity,
					usage.name(),
					clazzToProcess,
					AttributeOverride.class
			) );
		}
	}

	/**
	 * The rule is that an entity can override a field declared by a @MappedSuperclass
	 * if there is no intervening entity which also inherits the field. A wrinkle is
	 * that a mapped superclass can occur in between the root class and a subclass of
	 * an entity hierarchy, and then the subclass can override fields declared by the
	 * mapped superclass even though it cannot override any fields of the root class.
	 */
	private static void checkOverride(
			PersistentClass superEntity, String name, ClassDetails clazzToProcess, Class<?> overrideClass) {
		if ( superEntity.hasProperty( StringHelper.root(name) ) ) {
			throw new AnnotationException("Property '" + name
					+ "' is inherited from entity '" + superEntity.getEntityName()
					+ "' and may not be overridden using '@" + overrideClass.getSimpleName()
					+ "' in entity subclass '" + clazzToProcess.getName() + "'");
		}
	}

	private static void bindSoftDelete(
			ClassDetails classDetails,
			RootClass rootClass,
			InheritanceState inheritanceState,
			MetadataBuildingContext context) {
		// todo (soft-delete) : do we assume all package-level registrations are already available?
		//		or should this be a "second pass"?

		final SoftDelete softDelete = extractSoftDelete( classDetails, inheritanceState, context );
		if ( softDelete != null ) {
			SoftDeleteHelper.bindSoftDeleteIndicator(
					softDelete,
					rootClass,
					rootClass.getRootTable(),
					context
			);
		}
	}

	private static SoftDelete extractSoftDelete(
			ClassDetails classDetails,
			InheritanceState inheritanceState,
			MetadataBuildingContext context) {
		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();
		final SoftDelete fromClass = classDetails.getAnnotationUsage( SoftDelete.class, sourceModelContext );
		if ( fromClass != null ) {
			return fromClass;
		}

		ClassDetails classToCheck = classDetails.getSuperClass();
		while ( classToCheck != null ) {
			final SoftDelete fromSuper = classToCheck.getAnnotationUsage( SoftDelete.class, sourceModelContext );
			if ( fromSuper != null && classToCheck.hasAnnotationUsage( jakarta.persistence.MappedSuperclass.class, sourceModelContext ) ) {
				return fromSuper;
			}

			classToCheck = classToCheck.getSuperClass();
		}

		return extractFromPackage( SoftDelete.class, classDetails, context );
	}

	private void handleCheckConstraints() {
		if ( annotatedClass.hasAnnotationUsage( Checks.class, getSourceModelContext() ) ) {
			// if we have more than one of them they are not overrideable
			final Checks explicitUsage = annotatedClass.getAnnotationUsage( Checks.class, getSourceModelContext() );
			for ( Check check : explicitUsage.value() ) {
				addCheckToEntity( check );
			}
		}
		else {
			final Check check = getOverridableAnnotation( annotatedClass, Check.class, context );
			if ( check != null ) {
				addCheckToEntity( check );
			}
		}
	}

	/**
	 * For now, we store it on the entity.
	 * Later we will come back and figure out which table it belongs to.
	 */
	private void addCheckToEntity(Check check) {
		final String name = check.name();
		final String constraint = check.constraints();
		persistentClass.addCheckConstraint( name.isEmpty()
				? new CheckConstraint( constraint )
				: new CheckConstraint( name, constraint ) );
	}

	private void callTypeBinders(PersistentClass persistentClass) {
		final List<? extends Annotation> metaAnnotatedList = annotatedClass.getMetaAnnotated( TypeBinderType.class, getSourceModelContext() );
		for ( Annotation metaAnnotated : metaAnnotatedList ) {
			applyTypeBinder( metaAnnotated, persistentClass );
		}
	}

	private void applyTypeBinder(Annotation containingAnnotation, PersistentClass persistentClass) {
		final Class<? extends TypeBinder<?>> binderClass = containingAnnotation.annotationType()
				.getAnnotation( TypeBinderType.class )
				.binder();

		try {
			//noinspection rawtypes
			final TypeBinder binder = binderClass.getConstructor().newInstance();
			//noinspection unchecked
			binder.bind( containingAnnotation, context, persistentClass );
		}
		catch ( Exception e ) {
			throw new AnnotationException( "error processing @TypeBinderType annotation '" + containingAnnotation + "'", e );
		}
	}

	private void handleIdentifier(
			PropertyHolder propertyHolder,
			Map<ClassDetails, InheritanceState> inheritanceStates,
			Map<String, IdentifierGeneratorDefinition> generators,
			InheritanceState inheritanceState) {
		final ElementsToProcess elementsToProcess = inheritanceState.postProcess( persistentClass, this );
		final Set<String> idPropertiesIfIdClass = handleIdClass(
				persistentClass,
				inheritanceState,
				context,
				propertyHolder,
				elementsToProcess,
				inheritanceStates
		);
		processIdPropertiesIfNotAlready(
				persistentClass,
				inheritanceState,
				context,
				propertyHolder,
				generators,
				idPropertiesIfIdClass,
				elementsToProcess,
				inheritanceStates
		);
	}

	private void processComplementaryTableDefinitions() {
		final jakarta.persistence.Table jpaTableUsage =
				annotatedClass.getAnnotationUsage( jakarta.persistence.Table.class, getSourceModelContext() );
		if ( jpaTableUsage != null ) {
			final Table table = persistentClass.getTable();
			TableBinder.addJpaIndexes( table, jpaTableUsage.indexes(), context );
			TableBinder.addTableCheck( table, jpaTableUsage.check() );
			TableBinder.addTableComment( table, jpaTableUsage.comment() );
			TableBinder.addTableOptions( table, jpaTableUsage.options() );
		}

		final InFlightMetadataCollector.EntityTableXref entityTableXref = context
				.getMetadataCollector()
				.getEntityTableXref( persistentClass.getEntityName() );

		annotatedClass.forEachAnnotationUsage( jakarta.persistence.SecondaryTable.class, getSourceModelContext(), (usage) -> {
			final Identifier secondaryTableLogicalName = toIdentifier( usage.name() );
			final Table table = entityTableXref.resolveTable( secondaryTableLogicalName );
			assert table != null;

			TableBinder.addJpaIndexes( table, usage.indexes(), context );
		} );
	}

	private Set<String> handleIdClass(
			PersistentClass persistentClass,
			InheritanceState inheritanceState,
			MetadataBuildingContext context,
			PropertyHolder propertyHolder,
			ElementsToProcess elementsToProcess,
			Map<ClassDetails, InheritanceState> inheritanceStates) {
		final Set<String> idPropertiesIfIdClass = new HashSet<>();
		boolean isIdClass = mapAsIdClass(
				inheritanceStates,
				inheritanceState,
				persistentClass,
				propertyHolder,
				elementsToProcess,
				idPropertiesIfIdClass,
				context
		);
		if ( !isIdClass ) {
			setWrapIdsInEmbeddedComponents( elementsToProcess.getIdPropertyCount() > 1 );
		}
		return idPropertiesIfIdClass;
	}

	private boolean mapAsIdClass(
			Map<ClassDetails, InheritanceState> inheritanceStates,
			InheritanceState inheritanceState,
			PersistentClass persistentClass,
			PropertyHolder propertyHolder,
			ElementsToProcess elementsToProcess,
			Set<String> idPropertiesIfIdClass,
			MetadataBuildingContext context) {

		// We are looking for @IdClass
		// In general we map the id class as identifier using the mapping metadata of the main entity's
		// properties and create an identifier mapper containing the id properties of the main entity
		final ClassDetails classWithIdClass = inheritanceState.getClassWithIdClass( false );
		if ( classWithIdClass != null ) {
			final IdClass idClassAnn = classWithIdClass.getDirectAnnotationUsage( IdClass.class );
			final Class<?> idClassValue = idClassAnn.value();
			final ClassDetails compositeClass = context.getMetadataCollector().getSourceModelBuildingContext().getClassDetailsRegistry().resolveClassDetails( idClassValue.getName() );
			final TypeDetails compositeType = new ClassTypeDetailsImpl( compositeClass, TypeDetails.Kind.CLASS );
			final TypeDetails classWithIdType = new ClassTypeDetailsImpl( classWithIdClass, TypeDetails.Kind.CLASS );

			final AccessType accessType = getPropertyAccessType();
			final PropertyData inferredData = new PropertyPreloadedData( accessType, "id", compositeType );
			final PropertyData baseInferredData = new PropertyPreloadedData( accessType, "id", classWithIdType );
			final AccessType propertyAccessor = getPropertyAccessor( compositeClass );

			// In JPA 2, there is a shortcut if the IdClass is the Pk of the associated class pointed to by the id
			// it ought to be treated as an embedded and not a real IdClass (at least in Hibernate's internal way)
			final boolean isFakeIdClass = isIdClassPkOfTheAssociatedEntity(
					elementsToProcess,
					compositeClass,
					inferredData,
					baseInferredData,
					propertyAccessor,
					inheritanceStates,
					context
			);

			if ( isFakeIdClass ) {
				return false;
			}
			else {
				final boolean ignoreIdAnnotations = isIgnoreIdAnnotations();
				setIgnoreIdAnnotations( true );
				final Component idClassComponent = bindIdClass(
						inferredData,
						baseInferredData,
						propertyHolder,
						propertyAccessor,
						context,
						inheritanceStates
				);
				final Component mapper = createMapperProperty(
						inheritanceStates,
						persistentClass,
						propertyHolder,
						context,
						classWithIdClass,
						compositeType,
						baseInferredData,
						propertyAccessor,
						true
				);
				if ( idClassComponent.isSimpleRecord() ) {
					mapper.setSimpleRecord( true );
				}
				setIgnoreIdAnnotations( ignoreIdAnnotations );
				for ( Property property : mapper.getProperties() ) {
					idPropertiesIfIdClass.add( property.getName() );
				}
				return true;
			}
		}
		else {
			return false;
		}
	}

	private Component createMapperProperty(
			Map<ClassDetails, InheritanceState> inheritanceStates,
			PersistentClass persistentClass,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context,
			ClassDetails classWithIdClass,
			TypeDetails compositeClass,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			boolean isIdClass) {
		final Component mapper = createMapper(
				inheritanceStates,
				persistentClass,
				propertyHolder,
				context,
				classWithIdClass,
				compositeClass,
				baseInferredData,
				propertyAccessor,
				isIdClass
		);
		final Property mapperProperty = new Property();
		mapperProperty.setName( NavigablePath.IDENTIFIER_MAPPER_PROPERTY );
		mapperProperty.setUpdateable( false );
		mapperProperty.setInsertable( false );
		mapperProperty.setPropertyAccessorName( "embedded" );
		mapperProperty.setValue( mapper );
		persistentClass.addProperty( mapperProperty);
		return mapper;
	}

	private Component createMapper(
			Map<ClassDetails, InheritanceState> inheritanceStates,
			PersistentClass persistentClass,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context,
			ClassDetails classWithIdClass,
			TypeDetails compositeClass,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			boolean isIdClass) {
		final Component mapper = fillEmbeddable(
				propertyHolder,
				new PropertyPreloadedData(
						propertyAccessor,
						NavigablePath.IDENTIFIER_MAPPER_PROPERTY,
						compositeClass
				),
				baseInferredData,
				propertyAccessor,
				annotatedClass,
				false,
				this,
				true,
				true,
				false,
				null,
				null,
				null,
				context,
				inheritanceStates,
				isIdClass
		);
		persistentClass.setIdentifierMapper( mapper );

		// If id definition is on a mapped superclass, update the mapping
		final MappedSuperclass superclass = getMappedSuperclassOrNull( classWithIdClass, inheritanceStates, context );
		if ( superclass != null ) {
			superclass.setDeclaredIdentifierMapper( mapper );
		}
		else {
			// we are for sure on the entity
			persistentClass.setDeclaredIdentifierMapper( mapper );
		}
		return mapper;
	}

	private static PropertyData getUniqueIdPropertyFromBaseClass(
			PropertyData inferredData,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			MetadataBuildingContext context) {
		final List<PropertyData> baseClassElements = new ArrayList<>();
		final PropertyContainer propContainer = new PropertyContainer(
				baseInferredData.getClassOrElementType().determineRawClass(),
				inferredData.getPropertyType(),
				propertyAccessor
		);
		addElementsOfClass( baseClassElements, propContainer, context );
		//Id properties are on top and there is only one
		return baseClassElements.get( 0 );
	}

	private static boolean isIdClassPkOfTheAssociatedEntity(
			ElementsToProcess elementsToProcess,
			ClassDetails compositeClass,
			PropertyData inferredData,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			Map<ClassDetails, InheritanceState> inheritanceStates,
			MetadataBuildingContext context) {
		if ( elementsToProcess.getIdPropertyCount() == 1 ) {
			final PropertyData idPropertyOnBaseClass = getUniqueIdPropertyFromBaseClass(
					inferredData,
					baseInferredData,
					propertyAccessor,
					context
			);
			final InheritanceState state = inheritanceStates.get( idPropertyOnBaseClass.getClassOrElementType().determineRawClass() );
			if ( state == null ) {
				return false; //while it is likely a user error, let's consider it is something that might happen
			}
			final ClassDetails associatedClassWithIdClass = state.getClassWithIdClass( true );
			if ( associatedClassWithIdClass == null ) {
				//we cannot know for sure here unless we try and find the @EmbeddedId
				//Let's not do this thorough checking but do some extra validation
				return hasToOneAnnotation( idPropertyOnBaseClass.getAttributeMember() );

			}
			else {
				final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();
				final IdClass idClass = associatedClassWithIdClass.getAnnotationUsage( IdClass.class, sourceModelContext );
				return compositeClass.getName().equals( idClass.value().getName() );
			}
		}
		else {
			return false;
		}
	}

	private Component bindIdClass(
			PropertyData inferredData,
			PropertyData baseInferredData,
			PropertyHolder propertyHolder,
			AccessType propertyAccessor,
			MetadataBuildingContext buildingContext,
			Map<ClassDetails, InheritanceState> inheritanceStates) {
		propertyHolder.setInIdClass( true );

		// Fill simple value and property since and Id is a property
		final PersistentClass persistentClass = propertyHolder.getPersistentClass();
		if ( !( persistentClass instanceof RootClass rootClass ) ) {
			throw new AnnotationException( "Entity '" + persistentClass.getEntityName()
					+ "' is a subclass in an entity inheritance hierarchy and may not redefine the identifier of the root entity" );
		}
		final Component id = fillEmbeddable(
				propertyHolder,
				inferredData,
				baseInferredData,
				propertyAccessor,
				annotatedClass,
				false,
				this,
				true,
				false,
				false,
				null,
				null,
				null,
				buildingContext,
				inheritanceStates,
				true
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

		rootClass.setEmbeddedIdentifier( inferredData.getPropertyType() == null );

		propertyHolder.setInIdClass( null );

		return id;
	}

	private void handleSecondaryTables() {
		annotatedClass.forEachRepeatedAnnotationUsages( JpaAnnotations.SECONDARY_TABLE, getSourceModelContext(), (usage) -> {
			addSecondaryTable( usage, null, false );
		} );
	}

	private void handleClassTable(InheritanceState inheritanceState, PersistentClass superEntity) {
		final String schema;
		final String table;
		final String catalog;
		final UniqueConstraint[] uniqueConstraints;
		final jakarta.persistence.Table tableAnnotation = annotatedClass.getAnnotationUsage( jakarta.persistence.Table.class, getSourceModelContext() );
		if ( tableAnnotation != null ) {
			table = tableAnnotation.name();
			schema = tableAnnotation.schema();
			catalog = tableAnnotation.catalog();
			uniqueConstraints = tableAnnotation.uniqueConstraints();
		}
		else {
			//might be no @Table annotation on the annotated class
			schema = "";
			table = "";
			catalog = "";
			uniqueConstraints = new UniqueConstraint[0];
		}

		if ( inheritanceState.hasTable() ) {
			createTable( inheritanceState, superEntity, schema, table, catalog, uniqueConstraints );
		}
		else {
			// if we get here we have SINGLE_TABLE inheritance
			if ( tableAnnotation != null ) {
				throw new AnnotationException( "Entity '" + annotatedClass.getName()
						+ "' is a subclass in a 'SINGLE_TABLE' hierarchy and may not be annotated '@Table'"
						+ " (the root class declares the table mapping for the hierarchy)");
			}
			// we at least need to properly set up the EntityTableXref
			bindTableForDiscriminatedSubclass( superEntity.getEntityName() );
		}
	}

	private void createTable(
			InheritanceState inheritanceState,
			PersistentClass superEntity,
			String schema,
			String table,
			String catalog,
			UniqueConstraint[] uniqueConstraints) {
		final RowId rowId = annotatedClass.getAnnotationUsage( RowId.class, getSourceModelContext() );
		final View view = annotatedClass.getAnnotationUsage( View.class, getSourceModelContext() );
		bindTable(
				schema,
				catalog,
				table,
				uniqueConstraints,
				rowId == null ? null : rowId.value(),
				view == null ? null : view.query(),
				inheritanceState.hasDenormalizedTable()
						? context.getMetadataCollector().getEntityTableXref( superEntity.getEntityName() )
						: null
		);
	}

	private void handleInheritance(
			InheritanceState inheritanceState,
			PersistentClass superEntity,
			PropertyHolder propertyHolder) {
		final boolean isJoinedSubclass;
		switch ( inheritanceState.getType() ) {
			case JOINED:
				joinedInheritance( inheritanceState, superEntity, propertyHolder );
				isJoinedSubclass = inheritanceState.hasParents();
				break;
			case SINGLE_TABLE:
				singleTableInheritance( inheritanceState, propertyHolder );
				isJoinedSubclass = false;
				break;
			case TABLE_PER_CLASS:
				isJoinedSubclass = false;
				break;
			default:
				throw new AssertionFailure( "Unrecognized InheritanceType" );
		}

		bindDiscriminatorValue();

		if ( !isJoinedSubclass ) {
			checkNoJoinColumns( annotatedClass );
			checkNoOnDelete( annotatedClass );
		}
	}

	private void singleTableInheritance(InheritanceState inheritanceState, PropertyHolder holder) {
		final AnnotatedDiscriminatorColumn discriminatorColumn =
				processSingleTableDiscriminatorProperties( inheritanceState );
		// todo : sucks that this is separate from RootClass distinction
		if ( !inheritanceState.hasParents() ) {
			final RootClass rootClass = (RootClass) persistentClass;
			if ( inheritanceState.hasSiblings()
					|| discriminatorColumn != null && !discriminatorColumn.isImplicit() ) {
				bindDiscriminatorColumnToRootPersistentClass( rootClass, discriminatorColumn, holder );
				if ( context.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect() ) {
					rootClass.setForceDiscriminator( true );
				}
			}
		}
	}

	private void joinedInheritance(InheritanceState state, PersistentClass superEntity, PropertyHolder holder) {
		if ( state.hasParents() ) {
			final AnnotatedJoinColumns joinColumns = subclassJoinColumns( annotatedClass, superEntity, context );
			final JoinedSubclass jsc = (JoinedSubclass) persistentClass;
			final DependantValue key = new DependantValue( context, jsc.getTable(), jsc.getIdentifier() );
			jsc.setKey( key );
			handleForeignKeys( annotatedClass, context, key );
			final OnDelete onDelete = annotatedClass.getAnnotationUsage( OnDelete.class, getSourceModelContext() );
			key.setOnDeleteAction( onDelete == null ? null : onDelete.action() );
			//we are never in a second pass at that stage, so queue it
			context.getMetadataCollector()
					.addSecondPass( new JoinedSubclassFkSecondPass( jsc, joinColumns, key, context) );
			context.getMetadataCollector()
					.addSecondPass( new CreateKeySecondPass( jsc ) );
		}

		final AnnotatedDiscriminatorColumn discriminatorColumn = processJoinedDiscriminatorProperties( state );
		if ( !state.hasParents() ) {  // todo : sucks that this is separate from RootClass distinction
			final RootClass rootClass = (RootClass) persistentClass;
			// the class we're processing is the root of the hierarchy, so
			// let's see if we had a discriminator column (it's perfectly
			// valid for joined inheritance to not have a discriminator)
			if ( discriminatorColumn != null ) {
				// we do have a discriminator column
				if ( state.hasSiblings() || !discriminatorColumn.isImplicit() ) {
					bindDiscriminatorColumnToRootPersistentClass( rootClass, discriminatorColumn, holder );
					if ( context.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect() ) {
						rootClass.setForceDiscriminator( true );
					}
				}
			}
		}
	}

	private void checkNoJoinColumns(ClassDetails annotatedClass) {
		if ( annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumns.class, getSourceModelContext() )
				|| annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumn.class, getSourceModelContext() ) ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName()
					+ "' may not specify a '@PrimaryKeyJoinColumn'" );
		}
	}

	private void checkNoOnDelete(ClassDetails annotatedClass) {
		if ( annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumns.class, getSourceModelContext() )
				|| annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumn.class, getSourceModelContext() ) ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName() + "' may not be annotated '@OnDelete'" );
		}
	}

	private void handleForeignKeys(ClassDetails clazzToProcess, MetadataBuildingContext context, DependantValue key) {
		final PrimaryKeyJoinColumn pkJoinColumn = clazzToProcess.getDirectAnnotationUsage( PrimaryKeyJoinColumn.class );
		final PrimaryKeyJoinColumns pkJoinColumns = clazzToProcess.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class );
		final boolean noConstraintByDefault = context.getBuildingOptions().isNoConstraintByDefault();
		if ( pkJoinColumn != null && noConstraint( pkJoinColumn.foreignKey(), noConstraintByDefault )
				|| pkJoinColumns != null && noConstraint( pkJoinColumns.foreignKey(), noConstraintByDefault ) ) {
			key.disableForeignKey();
		}
		else {
			final ForeignKey foreignKey = clazzToProcess.getDirectAnnotationUsage( ForeignKey.class );
			if ( noConstraint( foreignKey, noConstraintByDefault ) ) {
				key.disableForeignKey();
			}
			else if ( foreignKey != null ) {
				key.setForeignKeyName( nullIfEmpty( foreignKey.name() ) );
				key.setForeignKeyDefinition( nullIfEmpty( foreignKey.foreignKeyDefinition() ) );
			}
			else if ( noConstraintByDefault ) {
				key.disableForeignKey();
			}
			else if ( pkJoinColumns != null ) {
				final ForeignKey nestedFk = pkJoinColumns.foreignKey();
				key.setForeignKeyName( nullIfEmpty( nestedFk.name() ) );
				key.setForeignKeyDefinition( nullIfEmpty( nestedFk.foreignKeyDefinition() ) );
			}
			else if ( pkJoinColumn != null ) {
				final ForeignKey nestedFk = pkJoinColumn.foreignKey();
				key.setForeignKeyName( nullIfEmpty( nestedFk.name() ) );
				key.setForeignKeyDefinition( nullIfEmpty( nestedFk.foreignKeyDefinition() ) );
			}
		}
	}

	private void bindDiscriminatorColumnToRootPersistentClass(
			RootClass rootClass,
			AnnotatedDiscriminatorColumn discriminatorColumn,
			PropertyHolder holder) {
		if ( rootClass.getDiscriminator() == null ) {
			if ( discriminatorColumn == null ) {
				throw new AssertionFailure( "discriminator column should have been built" );
			}
			final AnnotatedColumns columns = new AnnotatedColumns();
			columns.setPropertyHolder( holder );
			columns.setBuildingContext( context );
			columns.setJoins( secondaryTables );
//			discriminatorColumn.setJoins( secondaryTables );
//			discriminatorColumn.setPropertyHolder( holder );
			discriminatorColumn.setParent( columns );

			final BasicValue discriminatorColumnBinding = new BasicValue( context, rootClass.getTable() );
			rootClass.setDiscriminator( discriminatorColumnBinding );
			discriminatorColumn.linkWithValue( discriminatorColumnBinding );
			discriminatorColumnBinding.setTypeName( discriminatorColumn.getDiscriminatorTypeName() );
			rootClass.setPolymorphic( true );
			final String rootEntityName = rootClass.getEntityName();
			LOG.tracev( "Setting discriminator for entity {0}", rootEntityName);
			context.getMetadataCollector()
					.addSecondPass( new NullableDiscriminatorColumnSecondPass( rootEntityName ) );
		}
	}

	/**
	 * Process all discriminator-related metadata per rules for "single table" inheritance
	 */
	private AnnotatedDiscriminatorColumn processSingleTableDiscriminatorProperties(InheritanceState inheritanceState) {
		final DiscriminatorColumn discriminatorColumn = annotatedClass.getAnnotationUsage( DiscriminatorColumn.class, getSourceModelContext() );
		final DiscriminatorFormula discriminatorFormula = getOverridableAnnotation( annotatedClass, DiscriminatorFormula.class, context );

		if ( !inheritanceState.hasParents() || annotatedClass.hasAnnotationUsage( Inheritance.class, getSourceModelContext() ) ) {
			return buildDiscriminatorColumn(
					discriminatorColumn,
					discriminatorFormula,
					null,
					DEFAULT_DISCRIMINATOR_COLUMN_NAME,
					context
			);
		}
		else {
			// not a root entity
			if ( discriminatorColumn != null ) {
				throw new AnnotationException( "Entity class '" + annotatedClass.getName()
						+  "' is annotated '@DiscriminatorColumn' but it is not the root of the entity inheritance hierarchy");
			}
			if ( discriminatorFormula != null ) {
				throw new AnnotationException( "Entity class '" + annotatedClass.getName()
						+  "' is annotated '@DiscriminatorFormula' but it is not the root of the entity inheritance hierarchy");
			}
			return null;
		}
	}

	/**
	 * Process all discriminator-related metadata per rules for "joined" inheritance, taking
	 * into account {@value AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}
	 * and {@value AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}.
	 */
	private AnnotatedDiscriminatorColumn processJoinedDiscriminatorProperties(InheritanceState inheritanceState) {
		if ( annotatedClass.hasAnnotationUsage( DiscriminatorFormula.class, getSourceModelContext() ) ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName()
					+  "' has 'JOINED' inheritance and is annotated '@DiscriminatorFormula'" );
		}

		final DiscriminatorColumn discriminatorColumn = annotatedClass.getAnnotationUsage( DiscriminatorColumn.class, getSourceModelContext() );
		if ( !inheritanceState.hasParents() || annotatedClass.hasAnnotationUsage( Inheritance.class, getSourceModelContext() ) ) {
			return useDiscriminatorColumnForJoined( discriminatorColumn )
					? buildDiscriminatorColumn( discriminatorColumn, null, null, DEFAULT_DISCRIMINATOR_COLUMN_NAME, context )
					: null;
		}
		else {
			// not a root entity
			if ( discriminatorColumn != null ) {
				throw new AnnotationException( "Entity class '" + annotatedClass.getName()
						+  "' is annotated '@DiscriminatorColumn' but it is not the root of the entity inheritance hierarchy");
			}
			return null;
		}
	}

	/**
	 * We want to process the discriminator column if either:
	 * <ol>
	 * <li>there is an explicit {@link DiscriminatorColumn} annotation and we are not told to ignore it
	 *     via {@value AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}, or
	 * <li>there is no explicit {@link DiscriminatorColumn} annotation but we are told to create it
	 *     implicitly via {@value AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}.
	 * </ol>
	 */
	private boolean useDiscriminatorColumnForJoined(DiscriminatorColumn discriminatorColumn) {
		if ( discriminatorColumn != null ) {
			boolean ignore = context.getBuildingOptions().ignoreExplicitDiscriminatorsForJoinedInheritance();
			if ( ignore ) {
				LOG.debugf( "Ignoring explicit @DiscriminatorColumn annotation on: %s", annotatedClass.getName() );
			}
			return !ignore;
		}
		else {
			boolean createImplicit = context.getBuildingOptions().createImplicitDiscriminatorsForJoinedInheritance();
			if ( createImplicit ) {
				LOG.debugf( "Inferring implicit @DiscriminatorColumn using defaults for: %s", annotatedClass.getName() );
			}
			return createImplicit;
		}
	}

	private void processIdPropertiesIfNotAlready(
			PersistentClass persistentClass,
			InheritanceState inheritanceState,
			MetadataBuildingContext context,
			PropertyHolder propertyHolder,
			Map<String, IdentifierGeneratorDefinition> generators,
			Set<String> idPropertiesIfIdClass,
			ElementsToProcess elementsToProcess,
			Map<ClassDetails, InheritanceState> inheritanceStates) {
		final Set<String> missingIdProperties = new HashSet<>( idPropertiesIfIdClass );
		final Set<String> missingEntityProperties = new HashSet<>();
		for ( PropertyData propertyAnnotatedElement : elementsToProcess.getElements() ) {
			final String propertyName = propertyAnnotatedElement.getPropertyName();
			if ( !idPropertiesIfIdClass.contains( propertyName ) ) {
				final MemberDetails property = propertyAnnotatedElement.getAttributeMember();
				boolean hasIdAnnotation = hasIdAnnotation( property );
				if ( !idPropertiesIfIdClass.isEmpty() && !isIgnoreIdAnnotations()
						&& hasIdAnnotation ) {
					missingEntityProperties.add( propertyName );
				}
				else {
					boolean subclassAndSingleTableStrategy =
							inheritanceState.getType() == SINGLE_TABLE
									&& inheritanceState.hasParents();
					if ( !hasIdAnnotation && property.hasAnnotationUsage( GeneratedValue.class, getSourceModelContext() ) ) {
						throw new AnnotationException(
								"Property '"
										+ BinderHelper.getPath( propertyHolder, propertyAnnotatedElement )
										+ "' is annotated @GeneratedValue but is not part of an identifier" );
					}
					processElementAnnotations(
							propertyHolder,
							subclassAndSingleTableStrategy
									? Nullability.FORCED_NULL
									: Nullability.NO_CONSTRAINT,
							propertyAnnotatedElement,
							generators,
							this,
							false,
							false,
							false,
							context,
							inheritanceStates
					);
				}
			}
			else {
				missingIdProperties.remove( propertyName );
			}
		}

		if ( !missingIdProperties.isEmpty() ) {
			throw new AnnotationException( "Entity '" + persistentClass.getEntityName()
					+ "' has an '@IdClass' with properties " + getMissingPropertiesString( missingIdProperties )
					+ " which do not match properties of the entity class" );
		}
		else if ( !missingEntityProperties.isEmpty() ) {
			throw new AnnotationException( "Entity '" + persistentClass.getEntityName()
					+ "' has '@Id' annotated properties " + getMissingPropertiesString( missingEntityProperties )
					+ " which do not match properties of the specified '@IdClass'" );
		}
	}

	private static String getMissingPropertiesString(Set<String> propertyNames) {
		final StringBuilder sb = new StringBuilder();
		for ( String property : propertyNames ) {
			if ( sb.length() > 0 ) {
				sb.append( ", " );
			}
			sb.append( "'" ).append( property ).append( "'" );
		}
		return sb.toString();
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

	private static AnnotatedJoinColumns subclassJoinColumns(
			ClassDetails clazzToProcess,
			PersistentClass superEntity,
			MetadataBuildingContext context) {
		//@Inheritance(JOINED) subclass need to link back to the super entity
		final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
		joinColumns.setBuildingContext( context );

		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();

		final PrimaryKeyJoinColumns primaryKeyJoinColumns = clazzToProcess.getAnnotationUsage( PrimaryKeyJoinColumns.class, sourceModelContext );
		if ( primaryKeyJoinColumns != null ) {
			final PrimaryKeyJoinColumn[] columns = primaryKeyJoinColumns.value();
			if ( !ArrayHelper.isEmpty( columns ) ) {
				for ( PrimaryKeyJoinColumn column : columns ) {
					buildInheritanceJoinColumn(
							column,
							null,
							superEntity.getIdentifier(),
							joinColumns,
							context
					);
				}
			}
			else {
				final PrimaryKeyJoinColumn columnAnnotation = clazzToProcess.getAnnotationUsage( PrimaryKeyJoinColumn.class, sourceModelContext );
				buildInheritanceJoinColumn(
						columnAnnotation,
						null,
						superEntity.getIdentifier(),
						joinColumns,
						context
				);
			}
		}
		else {
			buildInheritanceJoinColumn(
					clazzToProcess.getAnnotationUsage( PrimaryKeyJoinColumn.class, sourceModelContext ),
					null,
					superEntity.getIdentifier(),
					joinColumns,
					context
			);
		}
		LOG.trace( "Subclass joined column(s) created" );
		return joinColumns;
	}

	private static PersistentClass getSuperEntity(
			ClassDetails clazzToProcess,
			Map<ClassDetails, InheritanceState> inheritanceStates,
			MetadataBuildingContext context,
			InheritanceState inheritanceState) {
		final InheritanceState superState = getInheritanceStateOfSuperEntity( clazzToProcess, inheritanceStates );
		if ( superState == null ) {
			return null;
		}
		else {
			final PersistentClass superEntity = context.getMetadataCollector()
					.getEntityBinding( superState.getClassDetails().getName() );
			//check if superclass is not a potential persistent class
			if ( superEntity == null && inheritanceState.hasParents() ) {
				throw new AssertionFailure( "Subclass has to be bound after its parent class: "
						+ superState.getClassDetails().getName() );
			}
			return superEntity;
		}
	}

	/**
	 * See {@link JpaEventListener} for a better (?) alternative
	 */
	private static void bindCallbacks(ClassDetails entityClass, PersistentClass persistentClass, MetadataBuildingContext context) {
		for ( CallbackType callbackType : CallbackType.values() ) {
			persistentClass.addCallbackDefinitions( CallbackDefinitionResolver.resolveEntityCallbacks(
					context,
					entityClass,
					callbackType
			) );
		}

		context.getMetadataCollector().addSecondPass( persistentClasses -> {
			for ( Property property : persistentClass.getDeclaredProperties() ) {
				if ( property.isComposite() ) {
					try {
						final Class<?> mappedClass = persistentClass.getMappedClass();
						for ( CallbackType type : CallbackType.values() ) {
							property.addCallbackDefinitions( CallbackDefinitionResolver.resolveEmbeddableCallbacks(
									context,
									mappedClass,
									property,
									type
							) );
						}
					}
					catch (ClassLoadingException ignore) {
						// a dynamic embeddable... cannot define listener methods
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
	public EntityBinder(MetadataBuildingContext context) {
		this.context = context;
	}

	public EntityBinder(ClassDetails annotatedClass, PersistentClass persistentClass, MetadataBuildingContext context) {
		this.context = context;
		this.persistentClass = persistentClass;
		this.annotatedClass = annotatedClass;
	}

	/**
	 * Delegates to {@link PersistentClass#isPropertyDefinedInHierarchy},
	 * after verifying that there is a {@link PersistentClass} available.
	 *
	 * @param name The name of the property to check
	 *
	 * @return {@code true} if a property by that given name does already exist in the super hierarchy.
	 */
	public boolean isPropertyDefinedInSuperHierarchy(String name) {
		// Yes, yes... persistentClass can be null because EntityBinder
		// can be used to bind Components (naturally!)
		return persistentClass != null
			&& persistentClass.isPropertyDefinedInSuperHierarchy( name );
	}

	private void bindRowManagement() {
		final DynamicInsert dynamicInsertAnn = annotatedClass.getAnnotationUsage( DynamicInsert.class, getSourceModelContext() );
		persistentClass.setDynamicInsert( dynamicInsertAnn != null && dynamicInsertAnn.value() );

		final DynamicUpdate dynamicUpdateAnn = annotatedClass.getAnnotationUsage( DynamicUpdate.class, getSourceModelContext() );
		persistentClass.setDynamicUpdate( dynamicUpdateAnn != null && dynamicUpdateAnn.value() );

		if ( persistentClass.useDynamicInsert() && annotatedClass.hasAnnotationUsage( SQLInsert.class, getSourceModelContext() ) ) {
			throw new AnnotationException( "Entity '" + name + "' is annotated both '@DynamicInsert' and '@SQLInsert'" );
		}
		if ( persistentClass.useDynamicUpdate() && annotatedClass.hasAnnotationUsage( SQLUpdate.class, getSourceModelContext() ) ) {
			throw new AnnotationException( "Entity '" + name + "' is annotated both '@DynamicUpdate' and '@SQLUpdate'" );
		}

		final SelectBeforeUpdate selectBeforeUpdateAnn = annotatedClass.getAnnotationUsage( SelectBeforeUpdate.class, getSourceModelContext() );
		persistentClass.setSelectBeforeUpdate( selectBeforeUpdateAnn != null && selectBeforeUpdateAnn.value() );
	}

	private void bindOptimisticLocking() {
		final OptimisticLocking optimisticLockingAnn = annotatedClass.getAnnotationUsage( OptimisticLocking.class, getSourceModelContext() );
		persistentClass.setOptimisticLockStyle( fromLockType( optimisticLockingAnn == null
				? OptimisticLockType.VERSION
				: optimisticLockingAnn.type() ) );
	}

	private void bindPolymorphism() {
		final Polymorphism polymorphismAnn = annotatedClass.getAnnotationUsage( Polymorphism.class, getSourceModelContext() );
		polymorphismType = polymorphismAnn == null ? IMPLICIT : polymorphismAnn.type();
	}

	private void bindEntityAnnotation() {
		final Entity entity = annotatedClass.getAnnotationUsage( Entity.class, getSourceModelContext() );
		if ( entity == null ) {
			throw new AssertionFailure( "@Entity should never be missing" );
		}
		final String entityName = entity.name();
		name = entityName.isEmpty() ? unqualify( annotatedClass.getName() ) : entityName;
	}

	public boolean isRootEntity() {
		// This is the best option I can think of here since
		// PersistentClass is most likely not yet fully populated
		return persistentClass instanceof RootClass;
	}

	public void bindEntity() {
		bindEntityAnnotation();
		bindRowManagement();
		bindOptimisticLocking();
		bindPolymorphism();
		bindProxy();
		bindConcreteProxy();
		bindWhere();
		bindCache();
		bindNaturalIdCache();
		bindFiltersInHierarchy();

		persistentClass.setAbstract( annotatedClass.isAbstract() );
		persistentClass.setClassName( annotatedClass.getClassName() );
		persistentClass.setJpaEntityName( name );
		persistentClass.setEntityName( annotatedClass.getName() );
		persistentClass.setCached( isCached );
		persistentClass.setLazy( lazy );
		persistentClass.setQueryCacheLayout( queryCacheLayout );
		if ( proxyClass != null && proxyClass != ClassDetails.VOID_CLASS_DETAILS ) {
			persistentClass.setProxyInterfaceName( proxyClass.getName() );
		}

		if ( persistentClass instanceof RootClass ) {
			bindRootEntity();
		}
		else if ( !isMutable() ) {
			throw new AnnotationException("Entity class '" + annotatedClass.getName()
					+ "' is annotated '@Immutable' but it is a subclass in an entity inheritance hierarchy"
					+ " (only root classes may declare mutability)");
		}

		ensureNoMutabilityPlan();

		registerImportName();

		processNamedEntityGraphs();
	}

	private void ensureNoMutabilityPlan() {
		if ( annotatedClass.hasAnnotationUsage( Mutability.class, getSourceModelContext() ) ) {
			throw new MappingException( "@Mutability is not allowed on entity" );
		}
	}

	private boolean isMutable() {
		return !annotatedClass.hasAnnotationUsage( Immutable.class, getSourceModelContext() );
	}

	private void registerImportName() {
		LOG.debugf( "Import with entity name %s", name );
		try {
			context.getMetadataCollector().addImport( name, persistentClass.getEntityName() );
			final String entityName = persistentClass.getEntityName();
			if ( !entityName.equals( name ) ) {
				context.getMetadataCollector().addImport( entityName, entityName );
			}
		}
		catch (MappingException me) {
			throw new AnnotationException( "Use of the same entity name twice: " + name, me );
		}
	}

	private void bindRootEntity() {
		final RootClass rootClass = (RootClass) persistentClass;
		rootClass.setMutable( isMutable() );
		rootClass.setExplicitPolymorphism( polymorphismType == EXPLICIT );
		if ( isNotEmpty( where ) ) {
			rootClass.setWhere( where );
		}
		if ( cacheConcurrentStrategy != null ) {
			rootClass.setCacheConcurrencyStrategy( cacheConcurrentStrategy );
			rootClass.setCacheRegionName( cacheRegion );
			rootClass.setLazyPropertiesCacheable( cacheLazyProperty );
		}
		rootClass.setNaturalIdCacheRegionName( naturalIdCacheRegion );
	}

	private void bindCustomSql() {
		final String primaryTableName = persistentClass.getTable().getName();

		SQLInsert sqlInsert = resolveCustomSqlAnnotation( annotatedClass, SQLInsert.class, primaryTableName );
		if ( sqlInsert == null ) {
			sqlInsert = resolveCustomSqlAnnotation( annotatedClass, SQLInsert.class, "" );
		}
		if ( sqlInsert != null ) {
			persistentClass.setCustomSQLInsert(
					sqlInsert.sql().trim(),
					sqlInsert.callable(),
					fromResultCheckStyle( sqlInsert.check() )
			);
			final Class<? extends Expectation> expectationClass = sqlInsert.verify();
			if ( expectationClass != Expectation.class ) {
				persistentClass.setInsertExpectation( getDefaultSupplier(  expectationClass ) );
			}
		}

		SQLUpdate sqlUpdate = resolveCustomSqlAnnotation( annotatedClass, SQLUpdate.class, primaryTableName );
		if ( sqlUpdate == null ) {
			sqlUpdate = resolveCustomSqlAnnotation( annotatedClass, SQLUpdate.class, "" );
		}
		if ( sqlUpdate != null ) {
			persistentClass.setCustomSQLUpdate(
					sqlUpdate.sql().trim(),
					sqlUpdate.callable(),
					fromResultCheckStyle( sqlUpdate.check() )
			);
			final Class<? extends Expectation> expectationClass = sqlUpdate.verify();
			if ( expectationClass != Expectation.class ) {
				persistentClass.setUpdateExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		SQLDelete sqlDelete = resolveCustomSqlAnnotation( annotatedClass, SQLDelete.class, primaryTableName );
		if ( sqlDelete == null ) {
			sqlDelete = resolveCustomSqlAnnotation( annotatedClass, SQLDelete.class, "" );
		}
		if ( sqlDelete != null ) {
			persistentClass.setCustomSQLDelete(
					sqlDelete.sql().trim(),
					sqlDelete.callable(),
					fromResultCheckStyle( sqlDelete.check() )
			);
			final Class<? extends Expectation> expectationClass = sqlDelete.verify();
			if ( expectationClass != Expectation.class ) {
				persistentClass.setDeleteExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		final SQLDeleteAll sqlDeleteAll = resolveCustomSqlAnnotation( annotatedClass, SQLDeleteAll.class, "" );
		if ( sqlDeleteAll != null ) {
			throw new AnnotationException("@SQLDeleteAll does not apply to entities: "
					+ persistentClass.getEntityName());
		}

		final SQLSelect sqlSelect = getOverridableAnnotation( annotatedClass, SQLSelect.class, context );
		if ( sqlSelect != null ) {
			final String loaderName = persistentClass.getEntityName() + "$SQLSelect";
			persistentClass.setLoaderName( loaderName );
			QueryBinder.bindNativeQuery( loaderName, sqlSelect, annotatedClass, context );
		}

		final HQLSelect hqlSelect = annotatedClass.getAnnotationUsage( HQLSelect.class, getSourceModelContext() );
		if ( hqlSelect != null ) {
			final String loaderName = persistentClass.getEntityName() + "$HQLSelect";
			persistentClass.setLoaderName( loaderName );
			QueryBinder.bindQuery( loaderName, hqlSelect, context );
		}
	}

	private void bindSubselect() {
		final Subselect subselect = annotatedClass.getAnnotationUsage( Subselect.class, getSourceModelContext() );
		if ( subselect != null ) {
			this.subselect = subselect.value();
		}
	}

	private <A extends Annotation> A resolveCustomSqlAnnotation(
			ClassDetails annotatedClass,
			Class<A> annotationType,
			String tableName) {
		// E.g. we are looking for SQLInsert...
		// 		- the overrideAnnotation would be DialectOverride.SQLInsert
		//		- we first look for all uses of DialectOverride.SQLInsert, if any, and see if they "match"
		//			- if so, we return the matched override
		//			- if not, we return the normal SQLInsert (if one)
		final Class<Annotation> overrideAnnotation = getOverrideAnnotation( annotationType );
		final Annotation[] dialectOverrides = annotatedClass.getRepeatedAnnotationUsages(
				overrideAnnotation,
				getSourceModelContext()
		);
		if ( isNotEmpty( dialectOverrides ) ) {
			final Dialect dialect = context.getMetadataCollector().getDatabase().getDialect();
			for ( int i = 0; i < dialectOverrides.length; i++ ) {
				//noinspection unchecked
				final DialectOverrider<A> dialectOverride = (DialectOverrider<A>) dialectOverrides[i];
				if ( !dialectOverride.matches( dialect ) ) {
					continue;
				}

				final A override = dialectOverride.override();
				if ( isEmpty( tableName )
						&& isEmpty( ( (CustomSqlDetails) override ).table() ) ) {
					return override;
				}
				else if ( isNotEmpty( tableName )
						&& tableName.equals( ( (CustomSqlDetails) override ).table() ) ) {
					return override;
				}
			}
		}

		return annotatedClass.getNamedAnnotationUsage( annotationType, tableName, "table", getSourceModelContext() );
	}

	private void bindFilters() {
		for ( Filter filter : filters ) {
			final String filterName = filter.name();
			String condition = filter.condition();
			if ( condition.isEmpty() ) {
				condition = getDefaultFilterCondition( filterName );
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

	private String getDefaultFilterCondition(String filterName) {
		final FilterDefinition definition = context.getMetadataCollector().getFilterDefinition( filterName );
		if ( definition == null ) {
			throw new AnnotationException( "Entity '" + name
					+ "' has a '@Filter' for an undefined filter named '" + filterName + "'" );
		}
		final String condition = definition.getDefaultFilterCondition();
		if ( isEmpty( condition ) ) {
			throw new AnnotationException( "Entity '" + name +
					"' has a '@Filter' with no 'condition' and no default condition was given by the '@FilterDef' named '"
					+ filterName + "'" );
		}
		return condition;
	}

	private void bindSynchronize() {
		final Synchronize synchronize = annotatedClass.getAnnotationUsage( Synchronize.class, getSourceModelContext() );
		if ( synchronize == null ) {
			return;
		}

		final JdbcEnvironment jdbcEnvironment = context.getMetadataCollector().getDatabase().getJdbcEnvironment();
		final boolean logical = synchronize.logical();
		final String[] tableNames = synchronize.value();
		for ( String tableName : tableNames ) {
			String physicalName = logical ? toPhysicalName( jdbcEnvironment, tableName ) : tableName;
			persistentClass.addSynchronizedTable( physicalName );
		}
	}

	private String toPhysicalName(JdbcEnvironment jdbcEnvironment, String logicalName) {
		final Identifier identifier =
				jdbcEnvironment.getIdentifierHelper().toIdentifier( logicalName );
		return context.getBuildingOptions().getPhysicalNamingStrategy()
				.toPhysicalTableName( identifier, jdbcEnvironment )
				.render( jdbcEnvironment.getDialect() );
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	private void processNamedEntityGraphs() {
		annotatedClass.forEachAnnotationUsage( NamedEntityGraph.class, getSourceModelContext(), this::processNamedEntityGraph );
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
		final DiscriminatorValue discriminatorValueAnn = annotatedClass.getAnnotationUsage( DiscriminatorValue.class, getSourceModelContext() );
		final String discriminatorValue = discriminatorValueAnn != null
				? discriminatorValueAnn.value()
				: null;
		if ( isEmpty( discriminatorValue ) ) {
			final Value discriminator = persistentClass.getDiscriminator();
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
			persistentClass.setDiscriminatorValue( discriminatorValue );
		}
	}

	public void bindProxy() {
		final Proxy proxy = annotatedClass.getAnnotationUsage( Proxy.class, getSourceModelContext() );
		if ( proxy != null ) {
			lazy = proxy.lazy();
			proxyClass = lazy ? resolveProxyClass( proxy, annotatedClass, getSourceModelContext() ) : null;
		}
		else {
			//needed to allow association lazy loading.
			lazy = true;
			proxyClass = annotatedClass;
		}
	}

	private static ClassDetails resolveProxyClass(
			Proxy proxy,
			ClassDetails annotatedClass,
			SourceModelBuildingContext sourceModelContext) {
		final Class<?> explicitProxyClass = proxy.proxyClass();
		if ( explicitProxyClass == void.class ) {
			return annotatedClass;
		}
		return sourceModelContext.getClassDetailsRegistry().resolveClassDetails( explicitProxyClass.getName() );
	}

	public void bindConcreteProxy() {
		final ConcreteProxy annotationUsage = annotatedClass.getAnnotationUsage( ConcreteProxy.class, getSourceModelContext() );
		if ( annotationUsage != null ) {
			if ( persistentClass.getSuperclass() != null ) {
				throw new AnnotationException( "Entity class '" + persistentClass.getClassName()
						+  "' is annotated '@ConcreteProxy' but it is not the root of the entity inheritance hierarchy" );
			}
			persistentClass.getRootClass().setConcreteProxy( true );
		}
	}

	public void bindWhere() {
		final Where where = getOverridableAnnotation( annotatedClass, Where.class, context );
		if ( where != null ) {
			this.where = where.clause();
		}
		final SQLRestriction restriction = getOverridableAnnotation( annotatedClass, SQLRestriction.class, context );
		if ( restriction != null ) {
			this.where = restriction.value();
		}
	}

	public void setWrapIdsInEmbeddedComponents(boolean wrapIdsInEmbeddedComponents) {
		this.wrapIdsInEmbeddedComponents = wrapIdsInEmbeddedComponents;
	}

	private void bindNaturalIdCache() {
		naturalIdCacheRegion = null;
		final NaturalIdCache naturalIdCacheAnn = annotatedClass.getAnnotationUsage( NaturalIdCache.class, getSourceModelContext() );
		if ( naturalIdCacheAnn == null ) {
			return;
		}

		final String region = naturalIdCacheAnn.region();
		if ( region.isEmpty() ) {
			final Cache explicitCacheAnn = annotatedClass.getAnnotationUsage( Cache.class, getSourceModelContext() );

			naturalIdCacheRegion = explicitCacheAnn != null && isNotEmpty( explicitCacheAnn.region() )
					? explicitCacheAnn.region() + NATURAL_ID_CACHE_SUFFIX
					: annotatedClass.getName() + NATURAL_ID_CACHE_SUFFIX;
		}
		else {
			naturalIdCacheRegion = naturalIdCacheAnn.region();
		}
	}

	private void bindCache() {
		isCached = false;
		cacheConcurrentStrategy = null;
		cacheRegion = null;
		cacheLazyProperty = true;
		queryCacheLayout = null;
		final SharedCacheMode sharedCacheMode  = context.getBuildingOptions().getSharedCacheMode();
		if ( persistentClass instanceof RootClass ) {
			bindRootClassCache( sharedCacheMode, context );
		}
		else {
			bindSubclassCache( sharedCacheMode );
		}
	}

	private void bindSubclassCache(SharedCacheMode sharedCacheMode) {
		if ( annotatedClass.hasAnnotationUsage( Cache.class, getSourceModelContext() ) ) {
			final String className = persistentClass.getClassName() == null
					? annotatedClass.getName()
					: persistentClass.getClassName();
			throw new AnnotationException("Entity class '" + className
					+  "' is annotated '@Cache' but it is a subclass in an entity inheritance hierarchy"
					+" (only root classes may define second-level caching semantics)");
		}

		final Cacheable cacheable = annotatedClass.getAnnotationUsage( Cacheable.class, getSourceModelContext() );
		isCached = cacheable == null && persistentClass.getSuperclass() != null
				// we should inherit the root class caching config
				? persistentClass.getSuperclass().isCached()
				//TODO: is this even correct?
				//      Do we even correctly support selectively enabling caching on subclasses like this?
				: isCacheable( sharedCacheMode, cacheable );
	}

	private void bindRootClassCache(SharedCacheMode sharedCacheMode, MetadataBuildingContext context) {
		final Cache cache = annotatedClass.getAnnotationUsage( Cache.class, getSourceModelContext() );
		final Cacheable cacheable = annotatedClass.getAnnotationUsage( Cacheable.class, getSourceModelContext() );
		final Cache effectiveCache;
		if ( cache != null ) {
			// preserve legacy behavior of circumventing SharedCacheMode when Hibernate's @Cache is used.
			isCached = true;
			effectiveCache = cache;
		}
		else {
			effectiveCache = buildCacheMock( annotatedClass, context );
			isCached = isCacheable( sharedCacheMode, cacheable );
		}
		cacheConcurrentStrategy = resolveCacheConcurrencyStrategy( effectiveCache.usage() );
		cacheRegion = effectiveCache.region();
		cacheLazyProperty = isCacheLazy( effectiveCache, annotatedClass );

		final QueryCacheLayout queryCache = annotatedClass.getAnnotationUsage( QueryCacheLayout.class, getSourceModelContext() );
		queryCacheLayout = queryCache == null ? null : queryCache.layout();
	}

	private static boolean isCacheLazy(Cache effectiveCache, ClassDetails annotatedClass) {
		if ( !effectiveCache.includeLazy() ) {
			return false;
		}
		return switch ( effectiveCache.include().toLowerCase( Locale.ROOT ) ) {
			case "all" -> true;
			case "non-lazy" -> false;
			default -> throw new AnnotationException(
					"Class '" + annotatedClass.getName()
							+ "' has a '@Cache' with undefined option 'include=\"" + effectiveCache.include() + "\"'" );
		};
	}

	private static boolean isCacheable(SharedCacheMode sharedCacheMode, Cacheable explicitCacheableAnn) {
		return switch ( sharedCacheMode ) {
			case ALL ->
				// all entities should be cached
					true;
			case ENABLE_SELECTIVE, UNSPECIFIED ->
				// Hibernate defaults to ENABLE_SELECTIVE, the only sensible setting
				// only entities with @Cacheable(true) should be cached
					explicitCacheableAnn != null && explicitCacheableAnn.value();
			case DISABLE_SELECTIVE ->
				// only entities with @Cacheable(false) should not be cached
					explicitCacheableAnn == null || explicitCacheableAnn.value();
			default ->
				// treat both NONE and UNSPECIFIED the same
					false;
		};
	}

	private static String resolveCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		final org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	private static Cache buildCacheMock(ClassDetails classDetails, MetadataBuildingContext context) {
		final CacheAnnotation cacheUsage = HibernateAnnotations.CACHE.createUsage( context.getMetadataCollector().getSourceModelBuildingContext() );
		cacheUsage.region( classDetails.getName() );
		cacheUsage.usage( determineCacheConcurrencyStrategy( context ) );
		return cacheUsage;
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

		@Override
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
			return jdbcEnvironment( buildingContext ).getIdentifierHelper().toIdentifier( explicitName );
		}

		@Override
		public Identifier toPhysicalName(Identifier logicalName, MetadataBuildingContext buildingContext) {
			return buildingContext.getBuildingOptions().getPhysicalNamingStrategy()
					.toPhysicalTableName( logicalName, jdbcEnvironment( buildingContext ) );
		}

		private static JdbcEnvironment jdbcEnvironment(MetadataBuildingContext buildingContext) {
			return buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment();
		}
	}

	public void bindTableForDiscriminatedSubclass(String entityName) {
		if ( !(persistentClass instanceof SingleTableSubclass) ) {
			throw new AssertionFailure(
					"Was expecting a discriminated subclass [" + SingleTableSubclass.class.getName() +
							"] but found [" + persistentClass.getClass().getName() + "] for entity [" +
							persistentClass.getEntityName() + "]"
			);
		}

		final InFlightMetadataCollector collector = context.getMetadataCollector();
		final InFlightMetadataCollector.EntityTableXref superTableXref =
				collector.getEntityTableXref( entityName );
		final Table primaryTable = superTableXref.getPrimaryTable();
		collector.addEntityTableXref(
				persistentClass.getEntityName(),
				collector.getDatabase().toIdentifier( collector.getLogicalTableName( primaryTable ) ),
				primaryTable,
				superTableXref
		);
	}

	public void bindTable(
			String schema,
			String catalog,
			String tableName,
			UniqueConstraint[] uniqueConstraints,
			String rowId,
			String viewQuery,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {

		final String entityName = persistentClass.getEntityName();

		final EntityTableNamingStrategyHelper namingStrategyHelper =
				new EntityTableNamingStrategyHelper( persistentClass.getClassName(), entityName, name );
		final Identifier logicalName =
				isNotEmpty( tableName )
						? namingStrategyHelper.handleExplicitName( tableName, context )
						: namingStrategyHelper.determineImplicitName( context );

		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				logicalName,
				persistentClass.isAbstract(),
				uniqueConstraints,
				context,
				subselect,
				denormalizedSuperTableXref
		);

		table.setRowId( rowId );
		table.setViewQuery( viewQuery );

//		final Comment comment = annotatedClass.getAnnotation( Comment.class );
//		if ( comment != null ) {
//			table.setComment( comment.value() );
//		}

		context.getMetadataCollector()
				.addEntityTableXref( entityName, logicalName, table, denormalizedSuperTableXref );

		if ( persistentClass instanceof TableOwner ) {
			LOG.debugf( "Bind entity %s on table %s", entityName, table.getName() );
			( (TableOwner) persistentClass ).setTable( table );
		}
		else {
			throw new AssertionFailure( "binding a table for a subclass" );
		}
	}

	public void finalSecondaryTableBinding(PropertyHolder propertyHolder) {
		 // This operation has to be done after the id definition of the persistence class.
		 // ie after the properties parsing
		final Iterator<Object> joinColumns = secondaryTableJoins.values().iterator();
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

	private void createPrimaryColumnsToSecondaryTable(
			Object incoming,
			PropertyHolder propertyHolder,
			Join join) {
		// `incoming` will be an array of some sort of annotation
		final Annotation[] joinColumnSource = (Annotation[]) incoming;
		final AnnotatedJoinColumns annotatedJoinColumns;

		if ( CollectionHelper.isEmpty( joinColumnSource ) ) {
			annotatedJoinColumns = createDefaultJoinColumn( propertyHolder );
		}
		else {
			final PrimaryKeyJoinColumn[] pkJoinColumns;
			final JoinColumn[] joinColumns;

			final Annotation first = joinColumnSource[0];
			if ( first instanceof PrimaryKeyJoinColumn ) {
				pkJoinColumns = (PrimaryKeyJoinColumn[]) joinColumnSource;
				joinColumns = null;
			}
			else if ( first instanceof JoinColumn ) {
				pkJoinColumns = null;
				joinColumns = (JoinColumn[]) joinColumnSource;
			}
			else {
				throw new IllegalArgumentException(
						"Expecting list of AnnotationUsages for either @JoinColumn or @PrimaryKeyJoinColumn"
								+ ", but got as list of AnnotationUsages for @"
								+ first.annotationType().getName()

				);
			}

			annotatedJoinColumns = createJoinColumns( propertyHolder, pkJoinColumns, joinColumns );
		}

		for ( AnnotatedJoinColumn joinColumn : annotatedJoinColumns.getJoinColumns() ) {
			joinColumn.forceNotNull();
		}
		bindJoinToPersistentClass( join, annotatedJoinColumns, context );
	}

	private AnnotatedJoinColumns createDefaultJoinColumn(PropertyHolder propertyHolder) {
		final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
		joinColumns.setBuildingContext( context );
		joinColumns.setJoins( secondaryTables );
		joinColumns.setPropertyHolder( propertyHolder );
		buildInheritanceJoinColumn(
				null,
				null,
				persistentClass.getIdentifier(),
				joinColumns,
				context
		);
		return joinColumns;
	}

	private AnnotatedJoinColumns createJoinColumns(
			PropertyHolder propertyHolder,
			PrimaryKeyJoinColumn[] primaryKeyJoinColumns,
			JoinColumn[] joinColumns) {
		final int joinColumnCount = primaryKeyJoinColumns != null ? primaryKeyJoinColumns.length : joinColumns.length;
		if ( joinColumnCount == 0 ) {
			return createDefaultJoinColumn( propertyHolder );
		}
		else {
			final AnnotatedJoinColumns columns = new AnnotatedJoinColumns();
			columns.setBuildingContext( context );
			columns.setJoins( secondaryTables );
			columns.setPropertyHolder( propertyHolder );
			for ( int colIndex = 0; colIndex < joinColumnCount; colIndex++ ) {
				final PrimaryKeyJoinColumn primaryKeyJoinColumn = primaryKeyJoinColumns != null
						? primaryKeyJoinColumns[colIndex]
						: null;
				final JoinColumn joinColumn = joinColumns != null
						? joinColumns[colIndex]
						: null;
				buildInheritanceJoinColumn(
						primaryKeyJoinColumn,
						joinColumn,
						persistentClass.getIdentifier(),
						columns,
						context
				);
			}
			return columns;
		}
	}

	private void bindJoinToPersistentClass(Join join, AnnotatedJoinColumns joinColumns, MetadataBuildingContext context) {
		DependantValue key = new DependantValue( context, join.getTable(), persistentClass.getIdentifier() );
		join.setKey( key );
		setForeignKeyNameIfDefined( join );
		key.setOnDeleteAction( null );
		bindForeignKey( persistentClass, null, joinColumns, key, false, context );
		key.sortProperties();
		join.createPrimaryKey();
		join.createForeignKey();
		persistentClass.addJoin( join );
	}

	private void setForeignKeyNameIfDefined(Join join) {
		final SimpleValue key = (SimpleValue) join.getKey();
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

	private SecondaryTable findMatchingSecondaryTable(Join join) {
		final String nameToMatch = join.getTable().getQuotedName();
		final SecondaryTable secondaryTable = annotatedClass.getDirectAnnotationUsage( SecondaryTable.class );
		if ( secondaryTable != null && nameToMatch.equals( secondaryTable.name() ) ) {
			return secondaryTable;
		}
		final SecondaryTables secondaryTables = annotatedClass.getDirectAnnotationUsage( SecondaryTables.class );
		if ( secondaryTables != null ) {
			final SecondaryTable[] nestedSecondaryTableList = secondaryTables.value();
			for ( SecondaryTable nestedSecondaryTable : nestedSecondaryTableList ) {
				if ( nestedSecondaryTable != null && nameToMatch.equals( nestedSecondaryTable.name() ) ) {
					return nestedSecondaryTable;
				}
			}
		}
		return null;
	}

	private SecondaryRow findMatchingSecondaryRowAnnotation(String tableName) {
		final SecondaryRow row = annotatedClass.getDirectAnnotationUsage( SecondaryRow.class );
		if ( row != null && ( row.table().isEmpty() || tableName.equals( row.table() ) ) ) {
			return row;
		}
		else {
			final SecondaryRows tables = annotatedClass.getDirectAnnotationUsage( SecondaryRows.class );
			if ( tables != null ) {
				final SecondaryRow[] rowList = tables.value();
				for ( SecondaryRow current : rowList ) {
					if ( tableName.equals( current.table() ) ) {
						return current;
					}
				}
			}
			return null;
		}
	}

	//Used for @*ToMany @JoinTable
	public Join addJoinTable(JoinTable joinTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		final Join join = addJoin(
				holder,
				noDelayInPkColumnCreation,
				false,
				joinTable.name(),
				joinTable.schema(),
				joinTable.catalog(),
				joinTable.joinColumns(),
				joinTable.uniqueConstraints()
		);
		final Table table = join.getTable();
		TableBinder.addTableCheck( table, joinTable.check() );
		TableBinder.addTableComment( table, joinTable.comment() );
		TableBinder.addTableOptions( table, joinTable.options() );
		return join;
	}

	public Join addSecondaryTable(SecondaryTable secondaryTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		final Join join = addJoin(
				holder,
				noDelayInPkColumnCreation,
				true,
				secondaryTable.name(),
				secondaryTable.schema(),
				secondaryTable.catalog(),
				secondaryTable.pkJoinColumns(),
				secondaryTable.uniqueConstraints()
		);
		final Table table = join.getTable();
		new IndexBinder( context ).bindIndexes( table, secondaryTable.indexes() );
		TableBinder.addTableCheck( table, secondaryTable.check() );
		TableBinder.addTableComment( table, secondaryTable.comment() );
		TableBinder.addTableOptions( table, secondaryTable.options() );
		return join;
	}

	private Join addJoin(
			PropertyHolder propertyHolder,
			boolean noDelayInPkColumnCreation,
			boolean secondaryTable,
			String name,
			String schema,
			String catalog,
			Object joinColumns,
			UniqueConstraint[] uniqueConstraints) {
		final QualifiedTableName logicalName = logicalTableName( name, schema, catalog );
		return createJoin(
				propertyHolder,
				noDelayInPkColumnCreation,
				secondaryTable,
				joinColumns,
				logicalName,
				TableBinder.buildAndFillTable(
						schema,
						catalog,
						logicalName.getTableName(),
						false,
						uniqueConstraints,
						context
				)
		);
	}

	private QualifiedTableName logicalTableName(String name, String schema, String catalog) {
		return new QualifiedTableName(
				toIdentifier( catalog ),
				toIdentifier( schema ),
				context.getMetadataCollector()
						.getDatabase()
						.getJdbcEnvironment()
						.getIdentifierHelper()
						.toIdentifier( name )
		);
	}

	Join createJoin(
			PropertyHolder propertyHolder,
			boolean noDelayInPkColumnCreation,
			boolean secondaryTable,
			Object joinColumns,
			QualifiedTableName logicalName,
			Table table) {
		final Join join = new Join();
		persistentClass.addJoin( join );

		final String entityName = persistentClass.getEntityName();
		final InFlightMetadataCollector.EntityTableXref tableXref
				= context.getMetadataCollector().getEntityTableXref( entityName );
		assert tableXref != null : "Could not locate EntityTableXref for entity [" + entityName + "]";
		tableXref.addSecondaryTable( logicalName, join );

		// No check constraints available on joins
		join.setTable( table );

		// Somehow keep joins() for later.
		// Has to do the work later because it needs PersistentClass id!
		LOG.debugf( "Adding secondary table to entity %s -> %s",
				entityName, join.getTable().getName() );

		handleSecondaryRowManagement( join );
		processSecondaryTableCustomSql( join );

		if ( noDelayInPkColumnCreation ) {
			// A non-null propertyHolder means than we process the Pk creation without delay
			createPrimaryColumnsToSecondaryTable( joinColumns, propertyHolder, join );
		}
		else {
			final String quotedName = table.getQuotedName();
			if ( secondaryTable ) {
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

	private void handleSecondaryRowManagement(Join join) {
		final String tableName = join.getTable().getQuotedName();
		final SecondaryRow matchingRow = findMatchingSecondaryRowAnnotation( tableName );
		if ( matchingRow != null ) {
			join.setInverse( !matchingRow.owned() );
			join.setOptional( matchingRow.optional() );
		}
		else {
			//default
			join.setInverse( false );
			join.setOptional( true ); //perhaps not quite per-spec, but a Good Thing anyway
		}
	}

	private void processSecondaryTableCustomSql(Join join) {
		final String tableName = join.getTable().getQuotedName();
		final SQLInsert sqlInsert = resolveCustomSqlAnnotation( annotatedClass, SQLInsert.class, tableName );
		if ( sqlInsert != null ) {
			join.setCustomSQLInsert(
					sqlInsert.sql().trim(),
					sqlInsert.callable(),
					fromResultCheckStyle( sqlInsert.check() )
			);
			final Class<? extends Expectation> expectationClass = sqlInsert.verify();
			if ( expectationClass != Expectation.class ) {
				join.setInsertExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		final SQLUpdate sqlUpdate = resolveCustomSqlAnnotation( annotatedClass, SQLUpdate.class, tableName );
		if ( sqlUpdate != null ) {
			join.setCustomSQLUpdate(
					sqlUpdate.sql().trim(),
					sqlUpdate.callable(),
					fromResultCheckStyle( sqlUpdate.check() )
			);
			final Class<? extends Expectation> expectationClass = sqlUpdate.verify();
			if ( expectationClass != Expectation.class ) {
				join.setUpdateExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		final SQLDelete sqlDelete = resolveCustomSqlAnnotation( annotatedClass, SQLDelete.class, tableName );
		if ( sqlDelete != null ) {
			join.setCustomSQLDelete(
					sqlDelete.sql().trim(),
					sqlDelete.callable(),
					fromResultCheckStyle( sqlDelete.check() )
			);
			final Class<? extends Expectation> expectationClass = sqlDelete.verify();
			if ( expectationClass != Expectation.class ) {
				join.setDeleteExpectation( getDefaultSupplier( expectationClass ) );
			}
		}
	}

	public java.util.Map<String, Join> getSecondaryTables() {
		return secondaryTables;
	}

	public static String getCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		final org.hibernate.cache.spi.access.AccessType accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	public void addFilter(Filter filter) {
		filters.add( filter );
	}

	public boolean isIgnoreIdAnnotations() {
		return ignoreIdAnnotations;
	}

	public void setIgnoreIdAnnotations(boolean ignoreIdAnnotations) {
		this.ignoreIdAnnotations = ignoreIdAnnotations;
	}

	public AccessType getPropertyAccessType() {
		return propertyAccessType;
	}

	public void setPropertyAccessType(AccessType propertyAccessType) {
		this.propertyAccessType = getExplicitAccessType( annotatedClass );
		// only set the access type if there is no explicit access type for this class
		if ( this.propertyAccessType == null ) {
			this.propertyAccessType = propertyAccessType;
		}
	}

	public AccessType getPropertyAccessor(AnnotationTarget element) {
		final AccessType accessType = getExplicitAccessType( element );
		return accessType == null ? propertyAccessType : accessType;
	}

	public AccessType getExplicitAccessType(AnnotationTarget element) {
		AccessType accessType = null;
		if ( element != null ) {
			final Access access = element.getAnnotationUsage( Access.class, getSourceModelContext() );
			if ( access != null ) {
				accessType = AccessType.getAccessStrategy( access.value() );
			}
		}
		return accessType;
	}

	/**
	 * Process the filters defined on the given class, as well as all filters
	 * defined on the MappedSuperclass(es) in the inheritance hierarchy
	 */
	public void bindFiltersInHierarchy() {

		bindFilters( annotatedClass );

		ClassDetails classToProcess = annotatedClass.getSuperClass();
		while ( classToProcess != null ) {
			final AnnotatedClassType classType = context.getMetadataCollector().getClassType( classToProcess );
			if ( classType == MAPPED_SUPERCLASS ) {
				bindFilters( classToProcess );
			}
			else {
				break;
			}
			classToProcess = classToProcess.getSuperClass();
		}
	}

	private void bindFilters(AnnotationTarget element) {
		final Filters filters = getOverridableAnnotation( element, Filters.class, context );
		if ( filters != null ) {
			for ( Filter filter : filters.value() ) {
				addFilter( filter );
			}
		}
		final Filter filter = element.getDirectAnnotationUsage( Filter.class );
		if ( filter != null ) {
			addFilter( filter );
		}
	}
}
