/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

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
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.*;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.internal.InheritanceState.ElementsToProcess;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.NamingStrategyHelper;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.CustomSqlDetails;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.spi.NavigablePath;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static java.util.Collections.addAll;
import static org.hibernate.boot.model.internal.AnnotatedClassType.MAPPED_SUPERCLASS;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.buildDiscriminatorColumn;
import static org.hibernate.boot.model.internal.AnnotatedJoinColumn.buildInheritanceJoinColumn;
import static org.hibernate.boot.model.internal.BinderHelper.extractFromPackage;
import static org.hibernate.boot.model.internal.BinderHelper.getMappedSuperclassOrNull;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.handleForeignKeyConstraint;
import static org.hibernate.boot.model.internal.BinderHelper.hasToOneAnnotation;
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
import static org.hibernate.boot.model.internal.QueryBinder.bindNativeQuery;
import static org.hibernate.boot.model.internal.QueryBinder.bindQuery;
import static org.hibernate.boot.model.internal.TableBinder.bindForeignKey;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.engine.OptimisticLockStyle.fromLockType;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.fromResultCheckStyle;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.getDefaultSupplier;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.root;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.jpa.event.internal.CallbackDefinitionResolver.resolveLifecycleCallbacks;
import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;


/**
 * Stateful binder responsible for interpreting information about an {@link Entity} class
 * and producing a {@link PersistentClass} mapping model object.
 *
 * @author Emmanuel Bernard
 */
public class EntityBinder {

	private static final String NATURAL_ID_CACHE_SUFFIX = "##NaturalId";

	private final MetadataBuildingContext context;

	private String name;
	private ClassDetails annotatedClass;
	private PersistentClass persistentClass;
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

	private ModelsContext modelsContext() {
		return context.getBootstrapContext().getModelsContext();
	}

	private InFlightMetadataCollector getMetadataCollector() {
		return context.getMetadataCollector();
	}

	private PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return context.getBuildingOptions().getPhysicalNamingStrategy();
	}

	private Database getDatabase() {
		return context.getMetadataCollector().getDatabase();
	}

	/**
	 * Bind an entity class. This can be done in a single pass.
	 */
	public static void bindEntityClass(
			ClassDetails clazzToProcess,
			Map<ClassDetails, InheritanceState> inheritanceStates,
			MetadataBuildingContext context) {
		if ( CORE_LOGGER.isTraceEnabled() ) {
			CORE_LOGGER.trace( "Binding entity with annotated class: " + clazzToProcess.getName() );
		}

		final var collector = context.getMetadataCollector();
		final var modelsContext = context.getBootstrapContext().getModelsContext();

		//TODO: be more strict with secondary table allowance (not for ids, not for secondary table join columns etc)

		final var inheritanceState = inheritanceStates.get( clazzToProcess );
		final var superEntity = getSuperEntity( clazzToProcess, inheritanceStates, context, inheritanceState );
		final var persistentClass = makePersistentClass( inheritanceState, superEntity, context );
		checkOverrides( clazzToProcess, superEntity, modelsContext );

		final var entityBinder = new EntityBinder( clazzToProcess, persistentClass, context );
		entityBinder.bindEntity();
		entityBinder.bindSubselect(); // has to happen before table binding
		entityBinder.bindTables( inheritanceState, superEntity );
		entityBinder.bindCustomSql(); // has to happen after table binding
		entityBinder.bindSynchronize();
		entityBinder.bindFilters();
		entityBinder.handleCheckConstraints();
		final var holder = buildPropertyHolder(
				clazzToProcess,
				persistentClass,
				entityBinder,
				context,
				inheritanceStates
		);
		entityBinder.handleInheritance( inheritanceState, superEntity, holder );
		entityBinder.handleIdentifier( holder, inheritanceStates, inheritanceState );

		if ( persistentClass instanceof RootClass rootClass ) {
			collector.addSecondPass( new CreateKeySecondPass( rootClass ) );
			bindSoftDelete( clazzToProcess, rootClass, context );
		}
		if ( persistentClass instanceof Subclass subclass ) {
			assert superEntity != null;
			superEntity.addSubclass( subclass );
		}

		collector.addEntityBinding( persistentClass );
		// process secondary tables and complementary definitions (ie o.h.a.Table)
		collector.addSecondPass( new SecondaryTableFromAnnotationSecondPass( entityBinder, holder ) );
		collector.addSecondPass( new SecondaryTableSecondPass( entityBinder, holder ) );
		collector.addSecondPass( ignored -> persistentClass.createConstraints( context ) );
		// comment, checkConstraint, and indexes are processed here
		entityBinder.processComplementaryTableDefinitions();
		resolveLifecycleCallbacks( clazzToProcess, persistentClass, context.getMetadataCollector() );
		entityBinder.callTypeBinders( persistentClass );
	}

	private void bindTables(InheritanceState inheritanceState, PersistentClass superEntity) {
		handleClassTable( inheritanceState, superEntity );
		handleSecondaryTables();
	}

	private static void checkOverrides(ClassDetails clazzToProcess, PersistentClass superEntity, ModelsContext sourceModelContext) {
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
		if ( superEntity.hasProperty( root(name) ) ) {
			throw new AnnotationException("Property '" + name
					+ "' is inherited from entity '" + superEntity.getEntityName()
					+ "' and may not be overridden using '@" + overrideClass.getSimpleName()
					+ "' in entity subclass '" + clazzToProcess.getName() + "'");
		}
	}

	private static void bindSoftDelete(
			ClassDetails classDetails,
			RootClass rootClass,
			MetadataBuildingContext context) {
		// todo (soft-delete) : do we assume all package-level registrations are already available?
		//		or should this be a "second pass"?

		final var softDelete = extractSoftDelete( classDetails, context );
		if ( softDelete != null ) {
			SoftDeleteHelper.bindSoftDeleteIndicator(
					softDelete,
					rootClass,
					rootClass.getRootTable(),
					context
			);
		}
	}

	private static SoftDelete extractSoftDelete(ClassDetails classDetails, MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var fromClass = classDetails.getAnnotationUsage( SoftDelete.class, modelsContext );
		if ( fromClass != null ) {
			return fromClass;
		}

		ClassDetails classToCheck = classDetails.getSuperClass();
		while ( classToCheck != null ) {
			final var fromSuper = classToCheck.getAnnotationUsage( SoftDelete.class, modelsContext );
			if ( fromSuper != null
					&& classToCheck.hasAnnotationUsage( MappedSuperclass.class, modelsContext ) ) {
				return fromSuper;
			}
			classToCheck = classToCheck.getSuperClass();
		}

		return extractFromPackage( SoftDelete.class, classDetails, context );
	}

	private void handleCheckConstraints() {
		if ( annotatedClass.hasAnnotationUsage( Checks.class, modelsContext() ) ) {
			// if we have more than one of them they are not overrideable
			final var explicitUsage = annotatedClass.getAnnotationUsage( Checks.class, modelsContext() );
			for ( var check : explicitUsage.value() ) {
				addCheckToEntity( check );
			}
		}
		else {
			final var check = getOverridableAnnotation( annotatedClass, Check.class, context );
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
		persistentClass.addCheckConstraint( name.isBlank()
				? new CheckConstraint( constraint )
				: new CheckConstraint( name, constraint ) );
	}

	private void callTypeBinders(PersistentClass persistentClass) {
		for ( var metaAnnotated : annotatedClass.getMetaAnnotated( TypeBinderType.class, modelsContext() ) ) {
			applyTypeBinder( metaAnnotated, persistentClass );
		}
	}

	private void applyTypeBinder(Annotation containingAnnotation, PersistentClass persistentClass) {
		final var binderClass =
				containingAnnotation.annotationType()
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
			InheritanceState inheritanceState) {
		final var elementsToProcess = inheritanceState.postProcess( persistentClass, this );
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
				idPropertiesIfIdClass,
				elementsToProcess,
				inheritanceStates
		);
	}

	private void processComplementaryTableDefinitions() {
		final var models = modelsContext();
		final var jpaTableUsage = annotatedClass.getAnnotationUsage( jakarta.persistence.Table.class, models );
		if ( jpaTableUsage != null ) {
			final var table = persistentClass.getTable();
			TableBinder.addJpaIndexes( table, jpaTableUsage.indexes(), context );
			TableBinder.addTableCheck( table, jpaTableUsage.check() );
			TableBinder.addTableComment( table, jpaTableUsage.comment() );
			TableBinder.addTableOptions( table, jpaTableUsage.options() );
		}

		final var entityTableXref = getMetadataCollector().getEntityTableXref( persistentClass.getEntityName() );
		annotatedClass.forEachAnnotationUsage( jakarta.persistence.SecondaryTable.class, models, (usage) -> {
			final Identifier secondaryTableLogicalName = toIdentifier( usage.name() );
			final var table = entityTableXref.resolveTable( secondaryTableLogicalName );
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
		final boolean isIdClass = mapAsIdClass(
				inheritanceStates,
				inheritanceState,
				persistentClass,
				propertyHolder,
				elementsToProcess,
				idPropertiesIfIdClass,
				context
		);
		if ( !isIdClass ) {
			wrapIdsInEmbeddedComponents = elementsToProcess.getIdPropertyCount() > 1;
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
		final var classWithIdClass = inheritanceState.getClassWithIdClass( false );
		if ( classWithIdClass != null ) {
			final var compositeClass = idClassDetails( inheritanceState, classWithIdClass );
			return compositeClass != null
				&& mapAsIdClass( inheritanceStates, persistentClass, propertyHolder, elementsToProcess,
					idPropertiesIfIdClass, context, compositeClass, classWithIdClass );
		}
		else {
			return false;
		}
	}

	private boolean mapAsIdClass(
			Map<ClassDetails, InheritanceState> inheritanceStates,
			PersistentClass persistentClass,
			PropertyHolder propertyHolder,
			ElementsToProcess elementsToProcess,
			Set<String> idPropertiesIfIdClass,
			MetadataBuildingContext context,
			ClassDetails compositeClass,
			ClassDetails classWithIdClass) {
		final TypeDetails compositeType = new ClassTypeDetailsImpl( compositeClass, TypeDetails.Kind.CLASS );
		final TypeDetails classWithIdType = new ClassTypeDetailsImpl( classWithIdClass, TypeDetails.Kind.CLASS );

		final AccessType accessType = getPropertyAccessType();
		final PropertyData inferredData = new PropertyPreloadedData( accessType, "id", compositeType );
		final PropertyData baseInferredData = new PropertyPreloadedData( accessType, "id", classWithIdType );
		final AccessType propertyAccessor = getPropertyAccessor( compositeClass );

		// In JPA 2, there is a shortcut if the IdClass is the PK of the associated class pointed to by the id
		// it ought to be treated as an embedded and not a real IdClass (at least in our internal language)
		final boolean isFakeIdClass = isIdClassPrimaryKeyOfAssociatedEntity(
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
			this.ignoreIdAnnotations = true;
			final var idClassComponent = bindIdClass(
					inferredData,
					baseInferredData,
					propertyHolder,
					propertyAccessor,
					context,
					inheritanceStates
			);
			final var mapper = createMapperProperty(
					inheritanceStates,
					persistentClass,
					propertyHolder,
					context,
					classWithIdClass,
					compositeType,
					baseInferredData,
					propertyAccessor
			);
			if ( idClassComponent.isSimpleRecord() ) {
				mapper.setSimpleRecord( true );
			}
			this.ignoreIdAnnotations = ignoreIdAnnotations;
			for ( var property : mapper.getProperties() ) {
				idPropertiesIfIdClass.add( property.getName() );
			}
			return true;
		}
	}

	private ClassDetails idClassDetails(InheritanceState inheritanceState, ClassDetails classWithIdClass) {
		final var idClass = classWithIdClass.getDirectAnnotationUsage( IdClass.class );
		final var classDetailsRegistry = modelsContext().getClassDetailsRegistry();
		if ( idClass == null ) {
			try {
				// look for an Id class generated by Hibernate Processor as an inner class of static metamodel
				final var javaClass = inheritanceState.getClassDetails().toJavaClass();
				final String generatedIdClassName = getGeneratedClassName( javaClass ) + "$Id";
				return classDetailsRegistry.resolveClassDetails( generatedIdClassName );
			}
			catch (RuntimeException e) {
				return null;
			}
		}
		else {
			return classDetailsRegistry.resolveClassDetails( idClass.value().getName() );
		}
	}

	private static String getGeneratedClassName(Class<?> javaClass) {
		return javaClass.isMemberClass()
				? getGeneratedClassName( javaClass.getEnclosingClass() ) + "$" + javaClass.getSimpleName() + "_"
				: javaClass.getName() + "_";
	}

	private Component createMapperProperty(
			Map<ClassDetails, InheritanceState> inheritanceStates,
			PersistentClass persistentClass,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context,
			ClassDetails classWithIdClass,
			TypeDetails compositeClass,
			PropertyData baseInferredData,
			AccessType propertyAccessor) {
		final var mapper = createMapper(
				inheritanceStates,
				persistentClass,
				propertyHolder,
				context,
				classWithIdClass,
				compositeClass,
				baseInferredData,
				propertyAccessor
		);
		final var mapperProperty = new SyntheticProperty();
		mapperProperty.setName( NavigablePath.IDENTIFIER_MAPPER_PROPERTY );
		mapperProperty.setUpdatable( false );
		mapperProperty.setInsertable( false );
		mapperProperty.setPropertyAccessorName( EMBEDDED.getExternalName() );
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
			AccessType propertyAccessor) {
		final var mapper = fillEmbeddable(
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
				true
		);
		persistentClass.setIdentifierMapper( mapper );

		// If id definition is on a mapped superclass, update the mapping
		final var superclass = getMappedSuperclassOrNull( classWithIdClass, inheritanceStates, context );
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
		final var propertyContainer =
				new PropertyContainer( baseInferredData.getClassOrElementType().determineRawClass(),
						inferredData.getPropertyType(), propertyAccessor );
		final int idPropertyCount =
				addElementsOfClass( baseClassElements, propertyContainer, context, 0 );
		assert idPropertyCount == 1;
		//Id properties are on top and there is only one
		return baseClassElements.get( 0 );
	}

	/**
	 * Given the id class of the current entity, as specified by an
	 * {@link IdClass} annotation, determine if it's actually the
	 * identifier type or {@link IdClass} of some associated entity.
	 */
	private boolean isIdClassPrimaryKeyOfAssociatedEntity(
			ElementsToProcess elementsToProcess,
			ClassDetails compositeClass,
			PropertyData inferredData,
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			Map<ClassDetails, InheritanceState> inheritanceStates,
			MetadataBuildingContext context) {
		if ( elementsToProcess.getIdPropertyCount() == 1 ) {
			// There's only one @Id field, so it might be the @EmbeddedId of an associated
			// entity referenced via a @ManyToOne or @OneToOne association
			final PropertyData idPropertyOnBaseClass =
					getUniqueIdPropertyFromBaseClass( inferredData, baseInferredData, propertyAccessor, context );
			final var idTypeDetails = idPropertyOnBaseClass.getClassOrElementType();
			final var state = inheritanceStates.get( idTypeDetails.determineRawClass() );
			if ( state == null ) {
				// Likely a user error, but treat it as something that might happen
				return false;
			}
			else {
				final var associatedClassWithIdClass = state.getClassWithIdClass( true );
				if ( associatedClassWithIdClass == null ) {
					// If annotated @OneToOne or @ManyToOne, it's an association to another entity
					return hasToOneAnnotation( idPropertyOnBaseClass.getAttributeMember() )
						// determine if the @Id or @EmbeddedId tpe is the same
						&& isIdClassOfAssociatedEntity( compositeClass, propertyAccessor, context, idTypeDetails );
				}
				else {
					// The associated entity has an @IdClass, so check if it's the same
					final var idClass = associatedClassWithIdClass.getAnnotationUsage( IdClass.class, modelsContext() );
					return compositeClass.getName().equals( idClass.value().getName() );
				}
			}
		}
		else {
			// There are multiple @Id fields, so we know for sure that the id class of
			// this entity can't be the identifier type of the associated entity
			return false;
		}
	}

	private static boolean isIdClassOfAssociatedEntity(
			ClassDetails compositeClass,
			AccessType propertyAccessor,
			MetadataBuildingContext context,
			TypeDetails idPropertyType) {
		// Determine the @Id type or @EmbeddedId class of the associated entity
		final var propertyContainer =
				new PropertyContainer( idPropertyType.determineRawClass(), idPropertyType, propertyAccessor );
		final List<PropertyData> idProperties = new ArrayList<>();
		final int idPropertyCount = addElementsOfClass( idProperties, propertyContainer, context, 0 );
		if ( idPropertyCount == 1 ) {
			// Exactly one @Id or @EmbeddedId attribute
			final PropertyData idPropertyOfAssociatedEntity = idProperties.get( 0 );
			return compositeClass.getName()
					.equals( idPropertyOfAssociatedEntity.getPropertyType().getName() );
		}
		else {
			// No id property found in the associated class,
			// or multiple id properties but no @IdClass
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
		final var persistentClass = propertyHolder.getPersistentClass();
		if ( !( persistentClass instanceof RootClass rootClass ) ) {
			throw new AnnotationException( "Entity '" + persistentClass.getEntityName()
					+ "' is a subclass in an entity inheritance hierarchy and may not redefine the identifier of the root entity" );
		}
		final var id = fillEmbeddable(
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
		annotatedClass.forEachRepeatedAnnotationUsages( JpaAnnotations.SECONDARY_TABLE, modelsContext(),
				usage -> addSecondaryTable( usage, null, false ) );
	}

	private void handleClassTable(InheritanceState inheritanceState, PersistentClass superEntity) {
		final String schema;
		final String table;
		final String catalog;
		final UniqueConstraint[] uniqueConstraints;
		final var tableAnnotation =
				annotatedClass.getAnnotationUsage( jakarta.persistence.Table.class, modelsContext() );
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
		final var models = modelsContext();
		final var rowId = annotatedClass.getAnnotationUsage( RowId.class, models );
		final var view = annotatedClass.getAnnotationUsage( View.class, models );
		bindTable(
				schema,
				catalog,
				table,
				uniqueConstraints,
				rowId == null ? null : rowId.value(),
				view == null ? null : view.query(),
				inheritanceState.hasDenormalizedTable()
						? getMetadataCollector().getEntityTableXref( superEntity.getEntityName() )
						: null
		);
	}

	private void handleInheritance(
			InheritanceState inheritanceState,
			PersistentClass superEntity,
			PropertyHolder propertyHolder) {
		switch ( inheritanceState.getType() ) {
			case JOINED:
				joinedInheritance( inheritanceState, superEntity, propertyHolder );
				break;
			case SINGLE_TABLE:
				singleTableInheritance( inheritanceState, propertyHolder );
				break;
			case TABLE_PER_CLASS:
				break;
			default:
				throw new AssertionFailure( "Unrecognized InheritanceType" );
		}

		bindDiscriminatorValue();

		if ( !isJoinedSubclass( inheritanceState ) ) {
			checkNoJoinColumns( annotatedClass );
			checkNoOnDelete( annotatedClass );
		}
	}

	private static boolean isJoinedSubclass(InheritanceState inheritanceState) {
		return inheritanceState.getType() == InheritanceType.JOINED
			&& inheritanceState.hasParents();
	}

	private void singleTableInheritance(InheritanceState inheritanceState, PropertyHolder holder) {
		final var discriminatorColumn = processSingleTableDiscriminatorProperties( inheritanceState );
		// todo : sucks that this is separate from RootClass distinction
		if ( !inheritanceState.hasParents() ) {
			if ( inheritanceState.hasSiblings()
					|| discriminatorColumn != null && !discriminatorColumn.isImplicit() ) {
				final var rootClass = (RootClass) persistentClass;
				bindDiscriminatorColumnToRootPersistentClass( rootClass, discriminatorColumn, holder );
				if ( context.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect() ) {
					rootClass.setForceDiscriminator( true );
				}
			}
		}
	}

	private void joinedInheritance(InheritanceState state, PersistentClass superEntity, PropertyHolder holder) {
		if ( state.hasParents() ) {
			final var joinColumns = subclassJoinColumns( annotatedClass, superEntity, context );
			final var joinedSubclass = (JoinedSubclass) persistentClass;
			final var key = new DependantValue( context, joinedSubclass.getTable(), joinedSubclass.getIdentifier() );
			joinedSubclass.setKey( key );
			handleForeignKey( annotatedClass, context, key );
			final var onDelete = annotatedClass.getAnnotationUsage( OnDelete.class, modelsContext() );
			key.setOnDeleteAction( onDelete == null ? null : onDelete.action() );
			//we are never in a second pass at that stage, so queue it
			final var metadataCollector = getMetadataCollector();
			metadataCollector.addSecondPass( new JoinedSubclassFkSecondPass( joinedSubclass, joinColumns, key, context) );
			metadataCollector.addSecondPass( new CreateKeySecondPass( joinedSubclass ) );
		}

		final var discriminatorColumn = processJoinedDiscriminatorProperties( state );
		// todo : sucks that this is separate from RootClass distinction
		if ( !state.hasParents() ) {
			// the class we're processing is the root of the hierarchy, so
			// let's see if we had a discriminator column (it's perfectly
			// valid for joined inheritance to not have a discriminator)
			if ( discriminatorColumn != null ) {
				// we do have a discriminator column
				if ( state.hasSiblings() || !discriminatorColumn.isImplicit() ) {
					final var rootClass = (RootClass) persistentClass;
					bindDiscriminatorColumnToRootPersistentClass( rootClass, discriminatorColumn, holder );
					if ( context.getBuildingOptions().shouldImplicitlyForceDiscriminatorInSelect() ) {
						rootClass.setForceDiscriminator( true );
					}
				}
			}
		}
	}

	private void checkNoJoinColumns(ClassDetails annotatedClass) {
		if ( annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumns.class, modelsContext() )
				|| annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumn.class, modelsContext() ) ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName()
					+ "' may not specify a '@PrimaryKeyJoinColumn'" );
		}
	}

	private void checkNoOnDelete(ClassDetails annotatedClass) {
		if ( annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumns.class, modelsContext() )
				|| annotatedClass.hasAnnotationUsage( PrimaryKeyJoinColumn.class, modelsContext() ) ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName() + "' may not be annotated '@OnDelete'" );
		}
	}

	private static void handleForeignKey(ClassDetails clazzToProcess, MetadataBuildingContext context, DependantValue key) {
		final var foreignKey = clazzToProcess.getDirectAnnotationUsage( ForeignKey.class );
		handleForeignKeyConstraint( key, foreignKey, nestedForeignKey( clazzToProcess ), context );
	}

	private static ForeignKey nestedForeignKey(ClassDetails clazzToProcess) {
		final var pkJoinColumn = clazzToProcess.getDirectAnnotationUsage( PrimaryKeyJoinColumn.class );
		final var pkJoinColumns = clazzToProcess.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class );
		if ( pkJoinColumn != null ) {
			return pkJoinColumn.foreignKey();
		}
		else if ( pkJoinColumns != null ) {
			return pkJoinColumns.foreignKey();
		}
		else {
			return null;
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
			final var columns = new AnnotatedColumns();
			columns.setPropertyHolder( holder );
			columns.setBuildingContext( context );
			columns.setJoins( secondaryTables );
//			discriminatorColumn.setJoins( secondaryTables );
//			discriminatorColumn.setPropertyHolder( holder );
			discriminatorColumn.setParent( columns );

			final var discriminatorColumnBinding = new BasicValue( context, rootClass.getTable() );
			rootClass.setDiscriminator( discriminatorColumnBinding );
			discriminatorColumn.linkWithValue( discriminatorColumnBinding );
			discriminatorColumnBinding.setTypeName( discriminatorColumn.getDiscriminatorTypeName() );
			rootClass.setPolymorphic( true );
			getMetadataCollector()
					.addSecondPass( new DiscriminatorColumnSecondPass( rootClass.getEntityName(),
							getDatabase().getDialect() ) );
		}
	}

	/**
	 * Process all discriminator-related metadata per rules for "single table" inheritance
	 */
	private AnnotatedDiscriminatorColumn processSingleTableDiscriminatorProperties(InheritanceState inheritanceState) {
		final var discriminatorColumn = annotatedClass.getAnnotationUsage( DiscriminatorColumn.class, modelsContext() );
		final var discriminatorFormula = getOverridableAnnotation( annotatedClass, DiscriminatorFormula.class, context );

		if ( !inheritanceState.hasParents()
				|| annotatedClass.hasAnnotationUsage( Inheritance.class, modelsContext() ) ) {
			return buildDiscriminatorColumn( discriminatorColumn, discriminatorFormula,
					null, DEFAULT_DISCRIMINATOR_COLUMN_NAME, context );
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
		final var modelContext = modelsContext();

		if ( annotatedClass.hasAnnotationUsage( DiscriminatorFormula.class, modelContext ) ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName()
					+  "' has 'JOINED' inheritance and is annotated '@DiscriminatorFormula'" );
		}

		final var discriminatorColumn = annotatedClass.getAnnotationUsage( DiscriminatorColumn.class, modelContext );
		if ( !inheritanceState.hasParents()
				|| annotatedClass.hasAnnotationUsage( Inheritance.class, modelContext ) ) {
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
		final var buildingOptions = context.getBuildingOptions();
		if ( discriminatorColumn != null ) {
			final boolean ignore = buildingOptions.ignoreExplicitDiscriminatorsForJoinedInheritance();
			if ( ignore ) {
				if ( CORE_LOGGER.isTraceEnabled() ) {
					CORE_LOGGER.trace( "Ignoring explicit @DiscriminatorColumn annotation on: "
									+ annotatedClass.getName() );
				}
			}
			return !ignore;
		}
		else {
			final boolean createImplicit = buildingOptions.createImplicitDiscriminatorsForJoinedInheritance();
			if ( createImplicit ) {
				if ( CORE_LOGGER.isTraceEnabled() ) {
					CORE_LOGGER.trace( "Inferring implicit @DiscriminatorColumn using defaults for: "
									+ annotatedClass.getName() );
				}
			}
			return createImplicit;
		}
	}

	private void processIdPropertiesIfNotAlready(
			PersistentClass persistentClass,
			InheritanceState inheritanceState,
			MetadataBuildingContext context,
			PropertyHolder propertyHolder,
			Set<String> idPropertiesIfIdClass,
			ElementsToProcess elementsToProcess,
			Map<ClassDetails, InheritanceState> inheritanceStates) {
		final Set<String> missingIdProperties = new HashSet<>( idPropertiesIfIdClass );
		final Set<String> missingEntityProperties = new HashSet<>();
		for ( var propertyAnnotatedElement : elementsToProcess.getElements() ) {
			final String propertyName = propertyAnnotatedElement.getPropertyName();
			if ( !idPropertiesIfIdClass.contains( propertyName ) ) {
				final var memberDetails = propertyAnnotatedElement.getAttributeMember();
				final boolean hasIdAnnotation = hasIdAnnotation( memberDetails );
				if ( !idPropertiesIfIdClass.isEmpty() && !isIgnoreIdAnnotations() && hasIdAnnotation ) {
					missingEntityProperties.add( propertyName );
				}
				else {
					final boolean subclassAndSingleTableStrategy =
							inheritanceState.getType() == SINGLE_TABLE
									&& inheritanceState.hasParents();
					if ( !hasIdAnnotation && memberDetails.hasAnnotationUsage( GeneratedValue.class, modelsContext() ) ) {
						throw new AnnotationException( "Property '" + getPath( propertyHolder, propertyAnnotatedElement )
												+ "' is annotated '@GeneratedValue' but is not part of an identifier" );
					}
					processElementAnnotations(
							propertyHolder,
							subclassAndSingleTableStrategy
									? Nullability.FORCED_NULL
									: Nullability.NO_CONSTRAINT,
							propertyAnnotatedElement,
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
		final var missingProperties = new StringBuilder();
		for ( String propertyName : propertyNames ) {
			if ( !missingProperties.isEmpty() ) {
				missingProperties.append( ", " );
			}
			missingProperties.append( "'" ).append( propertyName ).append( "'" );
		}
		return missingProperties.toString();
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
			return switch ( inheritanceState.getType() ) {
				case SINGLE_TABLE -> new SingleTableSubclass( superEntity, metadataBuildingContext );
				case JOINED -> new JoinedSubclass( superEntity, metadataBuildingContext );
				case TABLE_PER_CLASS -> new UnionSubclass( superEntity, metadataBuildingContext );
			};
		}
	}

	private static AnnotatedJoinColumns subclassJoinColumns(
			ClassDetails clazzToProcess,
			PersistentClass superEntity,
			MetadataBuildingContext context) {
		//@Inheritance(JOINED) subclass need to link back to the super entity
		final var joinColumns = new AnnotatedJoinColumns();
		joinColumns.setBuildingContext( context );
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		if ( clazzToProcess.hasAnnotationUsage( PrimaryKeyJoinColumn.class, modelsContext ) ) {
			final var columns = clazzToProcess.getRepeatedAnnotationUsages( PrimaryKeyJoinColumn.class, modelsContext );
			if ( columns.length == 0 ) {
				// PrimaryKeyJoinColumns must not be empty according to Javadoc
				throw new AnnotationException( "Empty '@PrimaryKeyJoinColumns' annotation" );
			}
			for ( var column : columns ) {
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
			buildInheritanceJoinColumn(
					null,
					null,
					superEntity.getIdentifier(),
					joinColumns,
					context
			);
		}
		return joinColumns;
	}

	private static PersistentClass getSuperEntity(
			ClassDetails clazzToProcess,
			Map<ClassDetails, InheritanceState> inheritanceStates,
			MetadataBuildingContext context,
			InheritanceState inheritanceState) {
		final var superState = getInheritanceStateOfSuperEntity( clazzToProcess, inheritanceStates );
		if ( superState == null ) {
			return null;
		}
		else {
			final var superEntity =
					context.getMetadataCollector()
							.getEntityBinding( superState.getClassDetails().getName() );
			//check if superclass is not a potential persistent class
			if ( superEntity == null && inheritanceState.hasParents() ) {
				throw new AssertionFailure( "Subclass has to be bound after its parent class: "
						+ superState.getClassDetails().getName() );
			}
			return superEntity;
		}
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
		final var modelsContext = modelsContext();
		final var dynamicInsert = annotatedClass.hasAnnotationUsage( DynamicInsert.class, modelsContext );
		final var dynamicUpdate = annotatedClass.hasAnnotationUsage( DynamicUpdate.class, modelsContext );
		persistentClass.setDynamicInsert( dynamicInsert );
		persistentClass.setDynamicUpdate( dynamicUpdate );

		if ( dynamicInsert && annotatedClass.hasAnnotationUsage( SQLInsert.class, modelsContext ) ) {
			throw new AnnotationException( "Entity '" + name + "' is annotated both '@DynamicInsert' and '@SQLInsert'" );
		}
		if ( dynamicUpdate && annotatedClass.hasAnnotationUsage( SQLUpdate.class, modelsContext ) ) {
			throw new AnnotationException( "Entity '" + name + "' is annotated both '@DynamicUpdate' and '@SQLUpdate'" );
		}
	}

	private void bindOptimisticLocking() {
		final var optimisticLocking = annotatedClass.getAnnotationUsage( OptimisticLocking.class, modelsContext() );
		persistentClass.setOptimisticLockStyle( fromLockType( optimisticLocking == null
				? OptimisticLockType.VERSION
				: optimisticLocking.type() ) );
	}

	private void bindEntityAnnotation() {
		final var entity = annotatedClass.getAnnotationUsage( Entity.class, modelsContext() );
		if ( entity == null ) {
			throw new AssertionFailure( "@Entity should never be missing" );
		}
		final String entityName = entity.name();
		name = entityName.isBlank() ? unqualify( annotatedClass.getName() ) : entityName;
	}

	public boolean isRootEntity() {
		// This is the best option I can think of here since
		// PersistentClass is most likely not yet fully populated
		return persistentClass instanceof RootClass;
	}

	private void bindEntity() {
		bindEntityAnnotation();
		bindRowManagement();
		bindOptimisticLocking();
		bindConcreteProxy();
		bindSqlRestriction();
		bindCache();
		bindNaturalIdCache();
		bindFiltersInHierarchy();

		persistentClass.setAbstract( annotatedClass.isAbstract() );
		persistentClass.setClassName( annotatedClass.getClassName() );
		persistentClass.setJpaEntityName( name );
		persistentClass.setEntityName( annotatedClass.getName() );
		persistentClass.setCached( isCached );
		persistentClass.setLazy( true );
		persistentClass.setQueryCacheLayout( queryCacheLayout );
		persistentClass.setProxyInterfaceName( annotatedClass.getName() );

		if ( persistentClass instanceof RootClass ) {
			bindRootEntity();
		}
		else {
			checkSubclassEntity();
		}

		ensureNoMutabilityPlan();

		registerImportName();

		processNamedEntityGraphs();
	}

	private void checkSubclassEntity() {
		if ( !isMutable() ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName()
					+ "' is annotated '@Immutable' but it is a subclass in an entity inheritance hierarchy"
					+ " (only a root class may declare its mutability)" );
		}
		if ( isNotBlank( where ) ) {
			throw new AnnotationException( "Entity class '" + annotatedClass.getName()
					+ "' specifies an '@SQLRestriction' but it is a subclass in an entity inheritance hierarchy"
					+ " (only a root class may be specify a restriction)" );
		}
	}

	private void ensureNoMutabilityPlan() {
		if ( annotatedClass.hasAnnotationUsage( Mutability.class, modelsContext() ) ) {
			throw new MappingException( "@Mutability is not allowed on entity" );
		}
	}

	private boolean isMutable() {
		return !annotatedClass.hasAnnotationUsage( Immutable.class, modelsContext() );
	}

	private void registerImportName() {
		try {
			final var metadataCollector = getMetadataCollector();
			metadataCollector.addImport( name, persistentClass.getEntityName() );
			final String entityName = persistentClass.getEntityName();
			if ( !entityName.equals( name ) ) {
				metadataCollector.addImport( entityName, entityName );
			}
		}
		catch (MappingException me) {
			throw new AnnotationException( "Use of the same entity name twice: " + name, me );
		}
	}

	private void bindRootEntity() {
		final var rootClass = (RootClass) persistentClass;
		rootClass.setMutable( isMutable() );
		if ( isNotBlank( where ) ) {
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

		var sqlInsert = resolveCustomSqlAnnotation( annotatedClass, SQLInsert.class, primaryTableName );
		if ( sqlInsert == null ) {
			sqlInsert = resolveCustomSqlAnnotation( annotatedClass, SQLInsert.class, "" );
		}
		if ( sqlInsert != null ) {
			persistentClass.setCustomSQLInsert(
					sqlInsert.sql().trim(),
					sqlInsert.callable(),
					fromResultCheckStyle( sqlInsert.check() )
			);
			final var expectationClass = sqlInsert.verify();
			if ( expectationClass != Expectation.class ) {
				persistentClass.setInsertExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		var sqlUpdate = resolveCustomSqlAnnotation( annotatedClass, SQLUpdate.class, primaryTableName );
		if ( sqlUpdate == null ) {
			sqlUpdate = resolveCustomSqlAnnotation( annotatedClass, SQLUpdate.class, "" );
		}
		if ( sqlUpdate != null ) {
			persistentClass.setCustomSQLUpdate(
					sqlUpdate.sql().trim(),
					sqlUpdate.callable(),
					fromResultCheckStyle( sqlUpdate.check() )
			);
			final var expectationClass = sqlUpdate.verify();
			if ( expectationClass != Expectation.class ) {
				persistentClass.setUpdateExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		var sqlDelete = resolveCustomSqlAnnotation( annotatedClass, SQLDelete.class, primaryTableName );
		if ( sqlDelete == null ) {
			sqlDelete = resolveCustomSqlAnnotation( annotatedClass, SQLDelete.class, "" );
		}
		if ( sqlDelete != null ) {
			persistentClass.setCustomSQLDelete(
					sqlDelete.sql().trim(),
					sqlDelete.callable(),
					fromResultCheckStyle( sqlDelete.check() )
			);
			final var expectationClass = sqlDelete.verify();
			if ( expectationClass != Expectation.class ) {
				persistentClass.setDeleteExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		final var sqlDeleteAll = resolveCustomSqlAnnotation( annotatedClass, SQLDeleteAll.class, "" );
		if ( sqlDeleteAll != null ) {
			throw new AnnotationException("@SQLDeleteAll does not apply to entities: "
					+ persistentClass.getEntityName());
		}

		final var sqlSelect = getOverridableAnnotation( annotatedClass, SQLSelect.class, context );
		if ( sqlSelect != null ) {
			final String loaderName = persistentClass.getEntityName() + "$SQLSelect";
			persistentClass.setLoaderName( loaderName );
			bindNativeQuery( loaderName, sqlSelect, annotatedClass, context );
		}

		final var hqlSelect = annotatedClass.getAnnotationUsage( HQLSelect.class, modelsContext() );
		if ( hqlSelect != null ) {
			final String loaderName = persistentClass.getEntityName() + "$HQLSelect";
			persistentClass.setLoaderName( loaderName );
			bindQuery( loaderName, hqlSelect, context );
		}
	}

	private void bindSubselect() {
		final var subselect = annotatedClass.getAnnotationUsage( Subselect.class, modelsContext() );
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
		final Annotation[] dialectOverrides =
				annotatedClass.getRepeatedAnnotationUsages( overrideAnnotation, modelsContext() );
		if ( isNotEmpty( dialectOverrides ) ) {
			final var dialect = getDatabase().getDialect();
			for ( var annotation : dialectOverrides ) {
				//noinspection unchecked
				final var dialectOverride = (DialectOverrider<A>) annotation;
				if ( dialectOverride.matches( dialect ) ) {
					final A override = dialectOverride.override();
					final String table = ((CustomSqlDetails) override).table();
					if ( isBlank( tableName ) && isBlank( table )
							|| Objects.equals( tableName, table ) ) {
						return override;
					}
				}
			}
		}

		return annotatedClass.getNamedAnnotationUsage( annotationType, tableName, "table", modelsContext() );
	}

	private void bindFilters() {
		for ( var filter : filters ) {
			final String filterName = filter.name();
			String condition = filter.condition();
			if ( condition.isBlank() ) {
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
		final var definition = getMetadataCollector().getFilterDefinition( filterName );
		if ( definition == null ) {
			throw new AnnotationException( "Entity '" + name
					+ "' has a '@Filter' for an undefined filter named '" + filterName + "'" );
		}
		final String condition = definition.getDefaultFilterCondition();
		if ( isBlank( condition ) ) {
			throw new AnnotationException( "Entity '" + name +
					"' has a '@Filter' with no 'condition' and no default condition was given by the '@FilterDef' named '"
					+ filterName + "'" );
		}
		return condition;
	}

	private void bindSynchronize() {
		final var synchronize = annotatedClass.getAnnotationUsage( Synchronize.class, modelsContext() );
		if ( synchronize != null ) {
			final boolean logical = synchronize.logical();
			for ( String tableName : synchronize.value() ) {
				final String physicalName = logical ? toPhysicalName( tableName ) : tableName;
				persistentClass.addSynchronizedTable( physicalName );
			}
		}
	}

	private String toPhysicalName(String logicalName) {
		final var jdbcEnvironment = getDatabase().getJdbcEnvironment();
		final var identifier = jdbcEnvironment.getIdentifierHelper().toIdentifier( logicalName );
		return getPhysicalNamingStrategy()
				.toPhysicalTableName( identifier, jdbcEnvironment )
				.render( jdbcEnvironment.getDialect() );
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	private void processNamedEntityGraphs() {
		annotatedClass.forEachAnnotationUsage( NamedEntityGraph.class, modelsContext(), this::processNamedEntityGraph );
		processParsedNamedGraphs();
	}

	private void processParsedNamedGraphs() {
		annotatedClass.forEachRepeatedAnnotationUsages(
				HibernateAnnotations.NAMED_ENTITY_GRAPH,
				modelsContext(),
				this::processParsedNamedEntityGraph
		);
	}

	private void processNamedEntityGraph(NamedEntityGraph annotation) {
		if ( annotation != null ) {
			getMetadataCollector()
					.addNamedEntityGraph( namedEntityGraphDefinition( annotation ) );
		}
	}

	private NamedEntityGraphDefinition namedEntityGraphDefinition(NamedEntityGraph annotation) {
		final String explicitName = annotation.name();
		return new NamedEntityGraphDefinition(
				StringHelper.isNotEmpty( explicitName ) ? explicitName : name,
				persistentClass.getEntityName(),
				NamedEntityGraphDefinition.Source.JPA,
				new NamedGraphCreatorJpa( annotation, name ) );
	}

	private void processParsedNamedEntityGraph(org.hibernate.annotations.NamedEntityGraph annotation) {
		if ( annotation != null ) {
			getMetadataCollector()
					.addNamedEntityGraph( namedEntityGraphDefinition( annotation ) );
		}
	}

	private NamedEntityGraphDefinition namedEntityGraphDefinition(org.hibernate.annotations.NamedEntityGraph annotation) {
		final String explicitName = annotation.name();
		return new NamedEntityGraphDefinition(
				StringHelper.isNotEmpty( explicitName ) ? explicitName : persistentClass.getJpaEntityName(),
				persistentClass.getEntityName(),
				NamedEntityGraphDefinition.Source.PARSED,
				new NamedGraphCreatorParsed( persistentClass.getMappedClass(), annotation )
		);
	}

	private void bindDiscriminatorValue() {
		final var discriminatorValue = annotatedClass.getAnnotationUsage( DiscriminatorValue.class, modelsContext() );
		persistentClass.setDiscriminatorValue( discriminatorValueString( discriminatorValue ) );
	}

	private String discriminatorValueString(DiscriminatorValue discriminatorValue) {
		if ( discriminatorValue == null ) {
			final var discriminator = persistentClass.getDiscriminator();
			if ( discriminator == null ) {
				return name;
			}
			else {
				final String discriminatorTypeName = discriminator.getType().getName();
				return switch ( discriminatorTypeName ) {
					case "string" -> name; //Spec compliant
					case "integer"-> String.valueOf( name.hashCode() ); // TODO: pretty nasty, should we just deprecate/disallow this?
					default -> throw new MappingException( "Entity '" + name + "' must explicitly specify its '@DiscriminatorValue'" );
				};
			}
		}
		else {
			return discriminatorValue.value();
		}
	}

	private void bindConcreteProxy() {
		final var concreteProxy = annotatedClass.getAnnotationUsage( ConcreteProxy.class, modelsContext() );
		if ( concreteProxy != null ) {
			if ( persistentClass.getSuperclass() != null ) {
				throw new AnnotationException( "Entity class '" + persistentClass.getClassName()
						+  "' is annotated '@ConcreteProxy' but it is not the root of the entity inheritance hierarchy" );
			}
			persistentClass.getRootClass().setConcreteProxy( true );
		}
	}

	private void bindSqlRestriction() {
		final var restriction = extractSQLRestriction( annotatedClass );
		if ( restriction != null ) {
			where = restriction.value();
		}
	}

	private SQLRestriction extractSQLRestriction(ClassDetails classDetails) {
		final var modelsContext = modelsContext();
		final var fromClass = getOverridableAnnotation( classDetails, SQLRestriction.class, context );
		if ( fromClass != null ) {
			return fromClass;
		}
		// as a special favor to users, we allow @SQLRestriction to be declared on a @MappedSuperclass
		ClassDetails classToCheck = classDetails.getSuperClass();
		while ( classToCheck != null
				&& classToCheck.hasAnnotationUsage( MappedSuperclass.class, modelsContext ) ) {
			final var fromSuper = getOverridableAnnotation( classToCheck, SQLRestriction.class, context );
			if ( fromSuper != null ) {
				return fromSuper;
			}
			classToCheck = classToCheck.getSuperClass();
		}
		return null;
	}

	private void bindNaturalIdCache() {
		final var naturalIdCache = annotatedClass.getAnnotationUsage( NaturalIdCache.class, modelsContext() );
		if ( naturalIdCache != null ) {
			final String region = naturalIdCache.region();
			if ( region.isBlank() ) {
				final var explicitCache = annotatedClass.getAnnotationUsage( Cache.class, modelsContext() );
				naturalIdCacheRegion =
						explicitCache != null && isNotBlank( explicitCache.region() )
								? explicitCache.region() + NATURAL_ID_CACHE_SUFFIX
								: annotatedClass.getName() + NATURAL_ID_CACHE_SUFFIX;
			}
			else {
				naturalIdCacheRegion = region;
			}
		}
		else {
			naturalIdCacheRegion = null;
		}
	}

	private void bindCache() {
		isCached = false;
		cacheConcurrentStrategy = null;
		cacheRegion = null;
		cacheLazyProperty = true;
		queryCacheLayout = null;
		if ( isRootEntity() ) {
			bindRootClassCache();
		}
		else {
			bindSubclassCache();
		}
	}

	private void bindSubclassCache() {
		if ( annotatedClass.hasAnnotationUsage( Cache.class, modelsContext() ) ) {
			final String className = persistentClass.getClassName() == null
					? annotatedClass.getName()
					: persistentClass.getClassName();
			throw new AnnotationException("Entity class '" + className
					+  "' is annotated '@Cache' but it is a subclass in an entity inheritance hierarchy"
					+" (only root classes may define second-level caching semantics)");
		}

		final var cacheable = annotatedClass.getAnnotationUsage( Cacheable.class, modelsContext() );
		isCached = cacheable == null && persistentClass.getSuperclass() != null
				// we should inherit the root class caching config
				? persistentClass.getSuperclass().isCached()
				//TODO: is this even correct?
				//      Do we even correctly support selectively enabling caching on subclasses like this?
				: isCacheable( cacheable );
	}

	private void bindRootClassCache() {
		final var sourceModelContext = modelsContext();

		final var cache = annotatedClass.getAnnotationUsage( Cache.class, sourceModelContext );
		final var cacheable = annotatedClass.getAnnotationUsage( Cacheable.class, sourceModelContext );

		// preserve legacy behavior of circumventing SharedCacheMode when Hibernate @Cache is used
		final var effectiveCache = cache != null ? cache : buildCacheMock( annotatedClass );
		isCached = cache != null || isCacheable( cacheable );

		cacheConcurrentStrategy = getCacheConcurrencyStrategy( effectiveCache.usage() );
		cacheRegion = effectiveCache.region();
		cacheLazyProperty = effectiveCache.includeLazy();

		final var queryCache = annotatedClass.getAnnotationUsage( QueryCacheLayout.class, sourceModelContext );
		queryCacheLayout = queryCache == null ? null : queryCache.layout();
	}

	private boolean isCacheable(Cacheable explicitCacheableAnn) {
		return switch ( context.getBuildingOptions().getSharedCacheMode() ) {
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

	private Cache buildCacheMock(ClassDetails classDetails) {
		final var cacheUsage = HibernateAnnotations.CACHE.createUsage( modelsContext() );
		cacheUsage.region( classDetails.getName() );
		cacheUsage.usage( determineCacheConcurrencyStrategy() );
		return cacheUsage;
	}

	private CacheConcurrencyStrategy determineCacheConcurrencyStrategy() {
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

	private void bindTableForDiscriminatedSubclass(String entityName) {
		if ( !(persistentClass instanceof SingleTableSubclass) ) {
			throw new AssertionFailure(
					"Expecting a discriminated subclass [" + SingleTableSubclass.class.getName() +
							"] but found [" + persistentClass.getClass().getName() + "] for entity [" +
							persistentClass.getEntityName() + "]"
			);
		}

		final var collector = getMetadataCollector();
		final var superTableXref = collector.getEntityTableXref( entityName );
		final Table primaryTable = superTableXref.getPrimaryTable();
		collector.addEntityTableXref(
				persistentClass.getEntityName(),
				collector.getDatabase().toIdentifier( collector.getLogicalTableName( primaryTable ) ),
				primaryTable,
				superTableXref
		);
	}

	private void bindTable(
			String schema,
			String catalog,
			String tableName,
			UniqueConstraint[] uniqueConstraints,
			String rowId,
			String viewQuery,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {
		final String entityName = persistentClass.getEntityName();
		final Identifier logicalTableName = logicalTableName( tableName, entityName );
		final Table table = TableBinder.buildAndFillTable(
				schema,
				catalog,
				logicalTableName,
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

		getMetadataCollector()
				.addEntityTableXref( entityName, logicalTableName, table, denormalizedSuperTableXref );

		if ( persistentClass instanceof TableOwner tableOwner ) {
			tableOwner.setTable( table );
		}
		else {
			throw new AssertionFailure( "binding a table for a subclass" );
		}
	}

	private Identifier logicalTableName(String tableName, String entityName) {
		final var namingStrategyHelper =
				new EntityTableNamingStrategyHelper( persistentClass.getClassName(), entityName, name );
		return isNotBlank( tableName )
				? namingStrategyHelper.handleExplicitName( tableName, context )
				: namingStrategyHelper.determineImplicitName( context );
	}

	public void finalSecondaryTableBinding(PropertyHolder propertyHolder) {
		// This operation has to be done after the id definition of the persistence class.
		// ie after the properties parsing
		final var joinColumns = secondaryTableJoins.values().iterator();
		for ( var entry : secondaryTables.entrySet() ) {
			if ( !secondaryTablesFromAnnotation.containsKey( entry.getKey() ) ) {
				createPrimaryColumnsToSecondaryTable( joinColumns.next(), propertyHolder, entry.getValue() );
			}
		}
	}

	public void finalSecondaryTableFromAnnotationBinding(PropertyHolder propertyHolder) {
		// This operation has to be done before the end of the FK second pass processing in order
		// to find the join columns belonging to secondary tables
		final var joinColumns = secondaryTableFromAnnotationJoins.values().iterator();
		for ( var entry : secondaryTables.entrySet() ) {
			if ( secondaryTablesFromAnnotation.containsKey( entry.getKey() ) ) {
				createPrimaryColumnsToSecondaryTable( joinColumns.next(), propertyHolder, entry.getValue() );
			}
		}
	}

	private void createPrimaryColumnsToSecondaryTable(
			Object incoming,
			PropertyHolder propertyHolder,
			Join join) {
		// `incoming` will be an array of some sort of annotation
		final var joinColumnSource = (Annotation[]) incoming;
		final AnnotatedJoinColumns annotatedJoinColumns;
		if ( isEmpty( joinColumnSource ) ) {
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

		for ( var joinColumn : annotatedJoinColumns.getJoinColumns() ) {
			joinColumn.forceNotNull();
		}
		bindJoinToPersistentClass( join, annotatedJoinColumns, context );
	}

	private AnnotatedJoinColumns createDefaultJoinColumn(PropertyHolder propertyHolder) {
		final var joinColumns = new AnnotatedJoinColumns();
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
		final int joinColumnCount =
				primaryKeyJoinColumns != null
						? primaryKeyJoinColumns.length
						: joinColumns.length;
		if ( joinColumnCount == 0 ) {
			return createDefaultJoinColumn( propertyHolder );
		}
		else {
			final var columns = new AnnotatedJoinColumns();
			columns.setBuildingContext( context );
			columns.setJoins( secondaryTables );
			columns.setPropertyHolder( propertyHolder );
			for ( int colIndex = 0; colIndex < joinColumnCount; colIndex++ ) {
				buildInheritanceJoinColumn(
						primaryKeyJoinColumns != null
								? primaryKeyJoinColumns[colIndex]
								: null,
						joinColumns != null
								? joinColumns[colIndex]
								: null,
						persistentClass.getIdentifier(),
						columns,
						context
				);
			}
			return columns;
		}
	}

	private void bindJoinToPersistentClass(
			Join join,
			AnnotatedJoinColumns joinColumns,
			MetadataBuildingContext context) {
		final var key = new DependantValue( context, join.getTable(), persistentClass.getIdentifier() );
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
		final var key = (SimpleValue) join.getKey();
		final var jpaSecondaryTable = findMatchingSecondaryTable( join );
		if ( jpaSecondaryTable != null ) {
			final boolean noConstraintByDefault = context.getBuildingOptions().isNoConstraintByDefault();
			if ( jpaSecondaryTable.foreignKey().value() == ConstraintMode.NO_CONSTRAINT
					|| jpaSecondaryTable.foreignKey().value() == ConstraintMode.PROVIDER_DEFAULT && noConstraintByDefault ) {
				key.disableForeignKey();
			}
			else {
				key.setForeignKeyName( nullIfEmpty( jpaSecondaryTable.foreignKey().name() ) );
				key.setForeignKeyDefinition( nullIfEmpty( jpaSecondaryTable.foreignKey().foreignKeyDefinition() ) );
				key.setForeignKeyOptions( jpaSecondaryTable.foreignKey().options() );
			}
		}
	}

	private SecondaryTable findMatchingSecondaryTable(Join join) {
		final String nameToMatch = join.getTable().getQuotedName();
		final var secondaryTable = annotatedClass.getDirectAnnotationUsage( SecondaryTable.class );
		if ( secondaryTable != null && nameToMatch.equals( secondaryTable.name() ) ) {
			return secondaryTable;
		}
		final var secondaryTables = annotatedClass.getDirectAnnotationUsage( SecondaryTables.class );
		if ( secondaryTables != null ) {
			for ( var nestedSecondaryTable : secondaryTables.value() ) {
				if ( nestedSecondaryTable != null && nameToMatch.equals( nestedSecondaryTable.name() ) ) {
					return nestedSecondaryTable;
				}
			}
		}
		return null;
	}

	private SecondaryRow findMatchingSecondaryRowAnnotation(String tableName) {
		final var row = annotatedClass.getDirectAnnotationUsage( SecondaryRow.class );
		if ( row != null && ( row.table().isBlank() || equalsTableName( tableName, row ) ) ) {
			return row;
		}
		else {
			final var tables = annotatedClass.getDirectAnnotationUsage( SecondaryRows.class );
			if ( tables != null ) {
				for ( var current : tables.value() ) {
					if ( equalsTableName( tableName, current ) ) {
						return current;
					}
				}
			}
			return null;
		}
	}

	private boolean equalsTableName(String physicalTableName, SecondaryRow secondaryRow) {
		final var database = getDatabase();
		final Identifier logicalName = database.toIdentifier( secondaryRow.table() );
		final Identifier secondaryRowPhysicalTableName =
				getPhysicalNamingStrategy()
						.toPhysicalTableName( logicalName, database.getJdbcEnvironment() );
		return physicalTableName.equals( secondaryRowPhysicalTableName.render() );
	}

	//Used for @*ToMany @JoinTable
	public Join addJoinTable(JoinTable joinTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		final var join = addJoin(
				holder,
				noDelayInPkColumnCreation,
				false,
				joinTable.name(),
				joinTable.schema(),
				joinTable.catalog(),
				joinTable.joinColumns(),
				joinTable.uniqueConstraints()
		);
		final var table = join.getTable();
		TableBinder.addTableCheck( table, joinTable.check() );
		TableBinder.addTableComment( table, joinTable.comment() );
		TableBinder.addTableOptions( table, joinTable.options() );
		return join;
	}

	public Join addSecondaryTable(SecondaryTable secondaryTable, PropertyHolder holder, boolean noDelayInPkColumnCreation) {
		final var join = addJoin(
				holder,
				noDelayInPkColumnCreation,
				true,
				secondaryTable.name(),
				secondaryTable.schema(),
				secondaryTable.catalog(),
				secondaryTable.pkJoinColumns(),
				secondaryTable.uniqueConstraints()
		);
		final var table = join.getTable();
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
		final var logicalName = logicalTableName( name, schema, catalog );
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
				getDatabase().getJdbcEnvironment().getIdentifierHelper().toIdentifier( name )
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
		final var tableXref = getMetadataCollector().getEntityTableXref( entityName );
		assert tableXref != null : "Could not locate EntityTableXref for entity [" + entityName + "]";
		tableXref.addSecondaryTable( logicalName, join );

		// No check constraints available on joins
		join.setTable( table );

		// Somehow keep joins() for later.
		// Has to do the work later because it needs PersistentClass id!

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
		final var matchingRow = findMatchingSecondaryRowAnnotation( tableName );
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

		final var sqlInsert = resolveCustomSqlAnnotation( annotatedClass, SQLInsert.class, tableName );
		if ( sqlInsert != null ) {
			join.setCustomSQLInsert(
					sqlInsert.sql().trim(),
					sqlInsert.callable(),
					fromResultCheckStyle( sqlInsert.check() )
			);
			final var expectationClass = sqlInsert.verify();
			if ( expectationClass != Expectation.class ) {
				join.setInsertExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		final var sqlUpdate = resolveCustomSqlAnnotation( annotatedClass, SQLUpdate.class, tableName );
		if ( sqlUpdate != null ) {
			join.setCustomSQLUpdate(
					sqlUpdate.sql().trim(),
					sqlUpdate.callable(),
					fromResultCheckStyle( sqlUpdate.check() )
			);
			final var expectationClass = sqlUpdate.verify();
			if ( expectationClass != Expectation.class ) {
				join.setUpdateExpectation( getDefaultSupplier( expectationClass ) );
			}
		}

		final var sqlDelete = resolveCustomSqlAnnotation( annotatedClass, SQLDelete.class, tableName );
		if ( sqlDelete != null ) {
			join.setCustomSQLDelete(
					sqlDelete.sql().trim(),
					sqlDelete.callable(),
					fromResultCheckStyle( sqlDelete.check() )
			);
			final var expectationClass = sqlDelete.verify();
			if ( expectationClass != Expectation.class ) {
				join.setDeleteExpectation( getDefaultSupplier( expectationClass ) );
			}
		}
	}

	public java.util.Map<String, Join> getSecondaryTables() {
		return secondaryTables;
	}

	public static String getCacheConcurrencyStrategy(CacheConcurrencyStrategy strategy) {
		final var accessType = strategy.toAccessType();
		return accessType == null ? null : accessType.getExternalName();
	}

	public boolean isIgnoreIdAnnotations() {
		return ignoreIdAnnotations;
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
		final var accessType = getExplicitAccessType( element );
		return accessType == null ? propertyAccessType : accessType;
	}

	private AccessType getExplicitAccessType(AnnotationTarget element) {
		if ( element != null ) {
			final var access = element.getAnnotationUsage( Access.class, modelsContext() );
			if ( access != null ) {
				return AccessType.getAccessStrategy( access.value() );
			}
		}
		return null;
	}

	/**
	 * Process the filters defined on the given class, as well as all filters
	 * defined on the MappedSuperclass(es) in the inheritance hierarchy
	 */
	private void bindFiltersInHierarchy() {

		bindFilters( annotatedClass );

		ClassDetails classToProcess = annotatedClass.getSuperClass();
		while ( classToProcess != null ) {
			final AnnotatedClassType classType = getMetadataCollector().getClassType( classToProcess );
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
		final var filters = getOverridableAnnotation( element, Filters.class, context );
		if ( filters != null ) {
			addAll( this.filters, filters.value() );
		}
		final var filter = element.getDirectAnnotationUsage( Filter.class );
		if ( filter != null ) {
			this.filters.add( filter );
		}
	}

	private static class JoinedSubclassFkSecondPass implements FkSecondPass {
		private final JoinedSubclass entity;
		private final MetadataBuildingContext buildingContext;
		private final SimpleValue key;
		private final AnnotatedJoinColumns columns;

		private JoinedSubclassFkSecondPass(
				JoinedSubclass entity,
				AnnotatedJoinColumns inheritanceJoinedColumns,
				SimpleValue key,
				MetadataBuildingContext buildingContext) {
			this.entity = entity;
			this.buildingContext = buildingContext;
			this.key = key;
			this.columns = inheritanceJoinedColumns;
		}

		@Override
		public Value getValue() {
			return key;
		}

		@Override
		public String getReferencedEntityName() {
			return entity.getSuperclass().getEntityName();
		}

		@Override
		public boolean isInPrimaryKey() {
			return true;
		}

		@Override
		public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
			bindForeignKey( entity.getSuperclass(), entity, columns, key, false, buildingContext );
		}
	}
}
