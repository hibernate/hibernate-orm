/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.Synchronize;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyColumnJpaAnnotation;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.InFlightMetadataCollector.CollectionTypeRegistrationDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;

import org.jboss.logging.Logger;

import jakarta.persistence.Access;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.UniqueConstraint;

import static jakarta.persistence.AccessType.PROPERTY;
import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static jakarta.persistence.ConstraintMode.PROVIDER_DEFAULT;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.boot.model.internal.AnnotatedClassType.EMBEDDABLE;
import static org.hibernate.boot.model.internal.AnnotatedClassType.NONE;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnFromAnnotation;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnFromNoAnnotation;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildColumnsFromAnnotations;
import static org.hibernate.boot.model.internal.AnnotatedColumn.buildFormulaFromAnnotation;
import static org.hibernate.boot.model.internal.AnnotatedJoinColumns.buildJoinColumnsWithDefaultColumnSuffix;
import static org.hibernate.boot.model.internal.AnnotatedJoinColumns.buildJoinTableJoinColumns;
import static org.hibernate.boot.model.internal.BinderHelper.buildAnyValue;
import static org.hibernate.boot.model.internal.BinderHelper.checkMappedByType;
import static org.hibernate.boot.model.internal.BinderHelper.createSyntheticPropertyReference;
import static org.hibernate.boot.model.internal.BinderHelper.extractFromPackage;
import static org.hibernate.boot.model.internal.BinderHelper.getCascadeStrategy;
import static org.hibernate.boot.model.internal.BinderHelper.getFetchMode;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.isDefault;
import static org.hibernate.boot.model.internal.BinderHelper.isPrimitive;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.EmbeddableBinder.fillEmbeddable;
import static org.hibernate.boot.model.internal.GeneratorBinder.visitIdGeneratorDefinitions;
import static org.hibernate.boot.model.internal.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.fromResultCheckStyle;
import static org.hibernate.internal.util.ReflectHelper.getDefaultSupplier;
import static org.hibernate.internal.util.StringHelper.getNonEmptyOrConjunctionIfBothNonEmpty;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.mapping.MappingHelper.createLocalUserCollectionTypeBean;

/**
 * Base class for stateful binders responsible for producing mapping model objects of type {@link Collection}.
 *
 * @author inger
 * @author Emmanuel Bernard
 */
public abstract class CollectionBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, CollectionBinder.class.getName() );

	private static final List<Class<?>> INFERRED_CLASS_PRIORITY = List.of(
			List.class,
			java.util.SortedSet.class,
			java.util.Set.class,
			java.util.SortedMap.class,
			Map.class,
			java.util.Collection.class
	);

	final MetadataBuildingContext buildingContext;
	private final Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver;
	private final boolean isSortedCollection;

	protected Collection collection;
	protected String propertyName;
	protected PropertyHolder propertyHolder;
	private String mappedBy;
	protected ClassDetails declaringClass;
	protected MemberDetails property;
	private TypeDetails collectionElementType;
	private TypeDetails targetEntity;
	private String cascadeStrategy;
	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private CacheLayout queryCacheLayout;
	private boolean oneToMany;
	protected IndexColumn indexColumn;
	protected OnDeleteAction onDeleteAction;
	protected boolean hasMapKeyProperty;
	protected String mapKeyPropertyName;
	private boolean insertable = true;
	private boolean updatable = true;
	protected AnnotatedJoinColumns inverseJoinColumns;
	protected AnnotatedJoinColumns foreignJoinColumns;
	private AnnotatedJoinColumns joinColumns;
	private boolean isExplicitAssociationTable;
	private AnnotatedColumns elementColumns;
	protected boolean isEmbedded;
	protected NotFoundAction notFoundAction;
	private TableBinder tableBinder;
	protected AnnotatedColumns mapKeyColumns;
	protected AnnotatedJoinColumns mapKeyManyToManyColumns;
	protected Map<String, IdentifierGeneratorDefinition> localGenerators;
	protected Map<ClassDetails, InheritanceState> inheritanceStatePerClass;
	private boolean declaringClassSet;
	private AccessType accessType;
	private boolean hibernateExtensionMapping;

	private jakarta.persistence.OrderBy jpaOrderBy;
	private org.hibernate.annotations.OrderBy sqlOrderBy;
	private SQLOrder sqlOrder;
	private SortNatural naturalSort;
	private SortComparator comparatorSort;

	private String explicitType;
	private final Map<String,String> explicitTypeParameters = new HashMap<>();

	protected CollectionBinder(
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver,
			boolean isSortedCollection,
			MetadataBuildingContext buildingContext) {
		this.customTypeBeanResolver = customTypeBeanResolver;
		this.isSortedCollection = isSortedCollection;
		this.buildingContext = buildingContext;
	}

	/**
	 * The first pass at binding a collection.
	 */
	public static void bindCollection(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			AnnotatedJoinColumns joinColumns) {
		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();

		final OneToMany oneToManyAnn = property.getAnnotationUsage( OneToMany.class, sourceModelContext );
		final ManyToMany manyToManyAnn = property.getAnnotationUsage( ManyToMany.class, sourceModelContext );
		final ElementCollection elementCollectionAnn = property.getAnnotationUsage( ElementCollection.class, sourceModelContext );
		checkAnnotations( propertyHolder, inferredData, property, oneToManyAnn, manyToManyAnn, elementCollectionAnn );

		final CollectionBinder collectionBinder = getCollectionBinder( property, hasMapKeyAnnotation( property ), context );
		collectionBinder.setIndexColumn( getIndexColumn( propertyHolder, inferredData, entityBinder, context, property ) );
		collectionBinder.setMapKey( property.getAnnotationUsage( MapKey.class, sourceModelContext ) );
		collectionBinder.setPropertyName( inferredData.getPropertyName() );
		collectionBinder.setJpaOrderBy( property.getAnnotationUsage( OrderBy.class, sourceModelContext ) );
		collectionBinder.setSqlOrderBy( getOverridableAnnotation( property, org.hibernate.annotations.OrderBy.class, context ) );
		collectionBinder.setSqlOrder( getOverridableAnnotation( property, SQLOrder.class, context ) );
		collectionBinder.setNaturalSort( property.getAnnotationUsage( SortNatural.class, sourceModelContext ) );
		collectionBinder.setComparatorSort( property.getAnnotationUsage( SortComparator.class, sourceModelContext ) );
		collectionBinder.setCache( property.getAnnotationUsage( Cache.class, sourceModelContext ) );
		collectionBinder.setQueryCacheLayout( property.getAnnotationUsage( QueryCacheLayout.class, sourceModelContext ) );
		collectionBinder.setPropertyHolder(propertyHolder);

		collectionBinder.setNotFoundAction( notFoundAction( propertyHolder, inferredData, property, manyToManyAnn, sourceModelContext ) );
		collectionBinder.setElementType( inferredData.getClassOrElementType() );
		collectionBinder.setAccessType( inferredData.getDefaultAccess() );
		collectionBinder.setEmbedded( property.hasAnnotationUsage( Embedded.class, sourceModelContext ) );
		collectionBinder.setProperty( property );
		collectionBinder.setOnDeleteActionAction( onDeleteAction( property ) );
		collectionBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
		collectionBinder.setDeclaringClass( inferredData.getDeclaringClass() );

//		final Comment comment = property.getAnnotation( Comment.class );
		final Cascade hibernateCascade = property.getAnnotationUsage( Cascade.class, sourceModelContext );

		collectionBinder.setElementColumns( elementColumns(
				propertyHolder,
				nullability,
				entityBinder,
				context,
				property,
				virtualPropertyData( inferredData, property )
//				comment
		) );

		collectionBinder.setMapKeyColumns( mapKeyColumns(
				propertyHolder,
				inferredData,
				entityBinder,
				context,
				property
//				comment
		) );

		collectionBinder.setMapKeyManyToManyColumns( mapKeyJoinColumns(
				propertyHolder,
				inferredData,
				entityBinder,
				context,
				property
//				comment
		) );

		bindJoinedTableAssociation(
				property,
				context,
				entityBinder,
				collectionBinder,
				propertyHolder,
				inferredData,
				handleTargetEntity(
						propertyHolder,
						inferredData,
						context,
						property,
						joinColumns,
						oneToManyAnn,
						manyToManyAnn,
						elementCollectionAnn,
						collectionBinder,
						hibernateCascade
				)
		);

		if ( isIdentifierMapper ) {
			collectionBinder.setInsertable( false );
			collectionBinder.setUpdatable( false );
		}

		if ( property.hasAnnotationUsage( CollectionId.class, sourceModelContext ) ) {
			//do not compute the generators unless necessary
			final HashMap<String, IdentifierGeneratorDefinition> availableGenerators = new HashMap<>();
			visitIdGeneratorDefinitions(
					property.getDeclaringType(),
					definition -> {
						if ( !definition.getName().isEmpty() ) {
							availableGenerators.put( definition.getName(), definition );
						}
					},
					context
			);
			visitIdGeneratorDefinitions(
					property,
					definition -> {
						if ( !definition.getName().isEmpty() ) {
							availableGenerators.put( definition.getName(), definition );
						}
					},
					context
			);
			collectionBinder.setLocalGenerators( availableGenerators );

		}
		collectionBinder.bind();
	}

	private static NotFoundAction notFoundAction(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MemberDetails property,
			ManyToMany manyToManyAnn,
			SourceModelBuildingContext sourceModelContext) {
		final NotFound notFound = property.getAnnotationUsage( NotFound.class, sourceModelContext );
		if ( notFound != null ) {
			if ( manyToManyAnn == null ) {
				throw new AnnotationException( "Collection '" + getPath(propertyHolder, inferredData)
						+ "' annotated '@NotFound' is not a '@ManyToMany' association" );
			}
			return notFound.action();
		}
		else {
			return null;
		}
	}

	private static AnnotatedJoinColumns mapKeyJoinColumns(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			MemberDetails property) {
//			Comment comment) {
		return buildJoinColumnsWithDefaultColumnSuffix(
				mapKeyJoinColumnAnnotations( property, context ),
//				comment,
				null,
				entityBinder.getSecondaryTables(),
				propertyHolder,
				inferredData,
				"_KEY",
				context
		);
	}

	private static OnDeleteAction onDeleteAction(MemberDetails property) {
		final OnDelete onDelete = property.getDirectAnnotationUsage( OnDelete.class );
		return onDelete == null ? null : onDelete.action();
	}

	private static PropertyData virtualPropertyData(PropertyData inferredData, MemberDetails property) {
		//do not use "element" if you are a JPA 2 @ElementCollection, only for legacy Hibernate mappings
		return property.hasDirectAnnotationUsage( ElementCollection.class )
				? inferredData
				: new WrappedInferredData(inferredData, "element" );
	}

	private static void checkAnnotations(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MemberDetails property,
			OneToMany oneToMany,
			ManyToMany manyToMany,
			ElementCollection elementCollection) {
		if ( ( oneToMany != null || manyToMany != null || elementCollection != null )
				&& isToManyAssociationWithinEmbeddableCollection( propertyHolder ) ) {
			throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData ) +
					"' belongs to an '@Embeddable' class that is contained in an '@ElementCollection' and may not be a "
					+ annotationName( oneToMany, manyToMany, elementCollection ));
		}

		if ( oneToMany != null && property.hasDirectAnnotationUsage( SoftDelete.class ) ) {
			throw new UnsupportedMappingException(
					"@SoftDelete cannot be applied to @OneToMany - " +
							property.getDeclaringType().getName() + "." + property.getName()
			);
		}

		if ( property.hasDirectAnnotationUsage( OrderColumn.class )
				&& manyToMany != null
				&& StringHelper.isNotEmpty( manyToMany.mappedBy() ) ) {
			throw new AnnotationException("Collection '" + getPath( propertyHolder, inferredData ) +
					"' is the unowned side of a bidirectional '@ManyToMany' and may not have an '@OrderColumn'");
		}

		if ( manyToMany != null || elementCollection != null ) {
			if ( property.hasDirectAnnotationUsage( JoinColumn.class )
					|| property.hasDirectAnnotationUsage( JoinColumns.class ) ) {
				throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData )
						+ "' is a " + annotationName( oneToMany, manyToMany, elementCollection )
						+ " and is directly annotated '@JoinColumn'"
						+ " (specify '@JoinColumn' inside '@JoinTable' or '@CollectionTable')" );
			}
		}
	}

	private static String annotationName(
			OneToMany oneToMany,
			ManyToMany manyToMany,
			ElementCollection elementCollection) {
		return oneToMany != null ? "'@OneToMany'" : manyToMany != null ? "'@ManyToMany'" : "'@ElementCollection'";
	}

	private static IndexColumn getIndexColumn(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			MemberDetails property) {
		return IndexColumn.fromAnnotations(
				property.getDirectAnnotationUsage( OrderColumn.class ),
				property.getDirectAnnotationUsage( ListIndexBase.class ),
				propertyHolder,
				inferredData,
				entityBinder.getSecondaryTables(),
				context
		);
	}

	private static String handleTargetEntity(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext context,
			MemberDetails property,
			AnnotatedJoinColumns joinColumns,
			OneToMany oneToManyAnn,
			ManyToMany manyToManyAnn,
			ElementCollection elementCollectionAnn,
			CollectionBinder collectionBinder,
			Cascade hibernateCascade) {

		//TODO enhance exception with @ManyToAny and @CollectionOfElements
		if ( oneToManyAnn != null && manyToManyAnn != null ) {
			throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData )
					+ "' is annotated both '@OneToMany' and '@ManyToMany'" );
		}
		final String mappedBy;
		if ( oneToManyAnn != null ) {
			if ( joinColumns.isSecondary() ) {
				throw new AnnotationException( "Collection '" + getPath( propertyHolder, inferredData )
						+ "' has foreign key in secondary table" );
			}
			collectionBinder.setFkJoinColumns( joinColumns );
			mappedBy = nullIfEmpty( oneToManyAnn.mappedBy() );
			collectionBinder.setTargetEntity( oneToManyAnn.targetEntity() );
			collectionBinder.setCascadeStrategy( getCascadeStrategy(
					oneToManyAnn.cascade(),
					hibernateCascade,
					oneToManyAnn.orphanRemoval(),
					context
			) );
			collectionBinder.setOneToMany( true );
		}
		else if ( elementCollectionAnn != null ) {
			if ( joinColumns.isSecondary() ) {
				throw new AnnotationException( "Collection '" + getPath( propertyHolder, inferredData )
						+ "' has foreign key in secondary table" );
			}
			collectionBinder.setFkJoinColumns( joinColumns );
			mappedBy = null;
			collectionBinder.setTargetEntity( elementCollectionAnn.targetClass() );
			collectionBinder.setOneToMany( false );
		}
		else if ( manyToManyAnn != null ) {
			mappedBy = nullIfEmpty( manyToManyAnn.mappedBy() );
			collectionBinder.setTargetEntity( manyToManyAnn.targetEntity() );
			collectionBinder.setCascadeStrategy( getCascadeStrategy(
					manyToManyAnn.cascade(),
					hibernateCascade,
					false,
					context
			) );
			collectionBinder.setOneToMany( false );
		}
		else if ( property.hasDirectAnnotationUsage( ManyToAny.class ) ) {
			mappedBy = null;
			collectionBinder.setTargetEntity( ClassDetails.VOID_CLASS_DETAILS );
			collectionBinder.setCascadeStrategy( getCascadeStrategy(
					null,
					hibernateCascade,
					false,
					context
			) );
			collectionBinder.setOneToMany( false );
		}
		else {
			mappedBy = null;
		}
		collectionBinder.setMappedBy( mappedBy );
		return mappedBy;
	}

	private static boolean hasMapKeyAnnotation(MemberDetails property) {
		return property.hasDirectAnnotationUsage(MapKeyJavaType.class)
			|| property.hasDirectAnnotationUsage(MapKeyJdbcType.class)
			|| property.hasDirectAnnotationUsage(MapKeyJdbcTypeCode.class)
			|| property.hasDirectAnnotationUsage(MapKeyMutability.class)
			|| property.hasDirectAnnotationUsage(MapKey.class)
			|| property.hasDirectAnnotationUsage(MapKeyType.class);
	}

	private static boolean isToManyAssociationWithinEmbeddableCollection(PropertyHolder propertyHolder) {
		return propertyHolder instanceof ComponentPropertyHolder componentPropertyHolder
			&& componentPropertyHolder.isWithinElementCollection();
	}

	private static AnnotatedColumns elementColumns(
			PropertyHolder propertyHolder,
			Nullability nullability,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			MemberDetails property,
			PropertyData virtualProperty) {
//			Comment comment) {
		if ( property.hasDirectAnnotationUsage( jakarta.persistence.Column.class ) ) {
			return buildColumnFromAnnotation(
					property.getDirectAnnotationUsage( jakarta.persistence.Column.class ),
					null,
//					comment,
					nullability,
					propertyHolder,
					virtualProperty,
					entityBinder.getSecondaryTables(),
					context
			);
		}
		else if ( property.hasDirectAnnotationUsage( Formula.class ) ) {
			return buildFormulaFromAnnotation(
					getOverridableAnnotation(property, Formula.class, context),
//					comment,
					nullability,
					propertyHolder,
					virtualProperty,
					entityBinder.getSecondaryTables(),
					context
			);
		}
		else if ( property.hasDirectAnnotationUsage( Columns.class ) ) {
			return buildColumnsFromAnnotations(
					property.getDirectAnnotationUsage( Columns.class ).columns(),
					null,
//					comment,
					nullability,
					propertyHolder,
					virtualProperty,
					entityBinder.getSecondaryTables(),
					context
			);
		}
		else {
			return buildColumnFromNoAnnotation(
					null,
//					comment,
					nullability,
					propertyHolder,
					virtualProperty,
					entityBinder.getSecondaryTables(),
					context
			);
		}
	}

	private static JoinColumn[] mapKeyJoinColumnAnnotations(
			MemberDetails property,
			MetadataBuildingContext context) {
		final MapKeyJoinColumn[] mapKeyJoinColumns = property.getRepeatedAnnotationUsages(
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				context.getMetadataCollector().getSourceModelBuildingContext()
		);

		if ( isEmpty( mapKeyJoinColumns ) ) {
			return null;
		}

		final JoinColumn[] joinColumns = new JoinColumn[mapKeyJoinColumns.length];
		for ( int i = 0; i < mapKeyJoinColumns.length; i++ ) {
			final JoinColumn joinColumn = JoinColumnJpaAnnotation.toJoinColumn(
					mapKeyJoinColumns[i],
					context.getMetadataCollector().getSourceModelBuildingContext()
			);
			joinColumns[i] = joinColumn;
		}
		return joinColumns;
	}

	private static AnnotatedColumns mapKeyColumns(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			MemberDetails property) {
//			Comment comment) {
		final jakarta.persistence.Column column;
		if ( property.hasDirectAnnotationUsage( MapKeyColumn.class ) ) {
			column = MapKeyColumnJpaAnnotation.toColumnAnnotation(
					property.getDirectAnnotationUsage( MapKeyColumn.class ),
					context.getMetadataCollector().getSourceModelBuildingContext()
			);
		}
		else {
			column = null;
		}
		return buildColumnsFromAnnotations(
				column == null ? null : new jakarta.persistence.Column[] { column },
//				comment,
				Nullability.FORCED_NOT_NULL,
				propertyHolder,
				inferredData,
				"_KEY",
				entityBinder.getSecondaryTables(),
				context
		);
	}

	private static void bindJoinedTableAssociation(
			MemberDetails property,
			MetadataBuildingContext buildingContext,
			EntityBinder entityBinder,
			CollectionBinder collectionBinder,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String mappedBy) {
		final TableBinder associationTableBinder = new TableBinder();
		final JoinTable assocTable = propertyHolder.getJoinTable( property );
		final CollectionTable collectionTable = property.getDirectAnnotationUsage( CollectionTable.class );

		final JoinColumn[] annJoins;
		final JoinColumn[] annInverseJoins;
		if ( assocTable != null || collectionTable != null ) {
			final String catalog;
			final String schema;
			final String tableName;
			final UniqueConstraint[] uniqueConstraints;
			final JoinColumn[] joins;
			final JoinColumn[] inverseJoins;
			final Index[] jpaIndexes;
			final String options;

			//JPA 2 has priority
			if ( collectionTable != null ) {
				catalog = collectionTable.catalog();
				schema = collectionTable.schema();
				tableName = collectionTable.name();
				uniqueConstraints = collectionTable.uniqueConstraints();
				joins = collectionTable.joinColumns();
				inverseJoins = null;
				jpaIndexes = collectionTable.indexes();
				options = collectionTable.options();
			}
			else {
				catalog = assocTable.catalog();
				schema = assocTable.schema();
				tableName = assocTable.name();
				uniqueConstraints = assocTable.uniqueConstraints();
				joins = assocTable.joinColumns();
				inverseJoins = assocTable.inverseJoinColumns();
				jpaIndexes = assocTable.indexes();
				options = assocTable.options();
			}

			collectionBinder.setExplicitAssociationTable( true );
			if ( CollectionHelper.isNotEmpty( jpaIndexes ) ) {
				associationTableBinder.setJpaIndex( jpaIndexes );
			}
			if ( !schema.isEmpty() ) {
				associationTableBinder.setSchema( schema );
			}
			if ( !catalog.isEmpty() ) {
				associationTableBinder.setCatalog( catalog );
			}
			if ( !tableName.isEmpty() ) {
				associationTableBinder.setName( tableName );
			}
			associationTableBinder.setUniqueConstraints( uniqueConstraints );
			associationTableBinder.setJpaIndex( jpaIndexes );
			associationTableBinder.setOptions( options );
			//set check constraint in the second pass
			annJoins = ArrayHelper.isEmpty( joins ) ? null : joins;
			annInverseJoins = inverseJoins == null || ArrayHelper.isEmpty( inverseJoins ) ? null : inverseJoins;
		}
		else {
			annJoins = null;
			annInverseJoins = null;
		}
		associationTableBinder.setBuildingContext( buildingContext );
		collectionBinder.setTableBinder( associationTableBinder );
		collectionBinder.setJoinColumns( buildJoinTableJoinColumns(
				annJoins,
				entityBinder.getSecondaryTables(),
				propertyHolder,
				inferredData,
				mappedBy,
				buildingContext
		) );
		collectionBinder.setInverseJoinColumns( buildJoinTableJoinColumns(
				annInverseJoins,
				entityBinder.getSecondaryTables(),
				propertyHolder,
				inferredData,
				mappedBy,
				buildingContext
		) );
	}

	protected MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	public Supplier<ManagedBean<? extends UserCollectionType>> getCustomTypeBeanResolver() {
		return customTypeBeanResolver;
	}

	public boolean isMap() {
		return false;
	}

	protected void setIsHibernateExtensionMapping(boolean hibernateExtensionMapping) {
		this.hibernateExtensionMapping = hibernateExtensionMapping;
	}

	protected boolean isHibernateExtensionMapping() {
		return hibernateExtensionMapping;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setInheritanceStatePerClass(Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
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

	public void setInverseJoinColumns(AnnotatedJoinColumns inverseJoinColumns) {
		this.inverseJoinColumns = inverseJoinColumns;
	}

	public void setJoinColumns(AnnotatedJoinColumns joinColumns) {
		this.joinColumns = joinColumns;
	}

	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	public void setJpaOrderBy(jakarta.persistence.OrderBy jpaOrderBy) {
		this.jpaOrderBy = jpaOrderBy;
	}

	@SuppressWarnings("removal")
	public void setSqlOrderBy(org.hibernate.annotations.OrderBy sqlOrderBy) {
		this.sqlOrderBy = sqlOrderBy;
	}

	public void setSqlOrder(SQLOrder sqlOrder) {
		this.sqlOrder = sqlOrder;
	}

	public void setNaturalSort(SortNatural naturalSort) {
		this.naturalSort = naturalSort;
	}

	public void setComparatorSort(SortComparator comparatorSort) {
		this.comparatorSort = comparatorSort;
	}

	/**
	 * collection binder factory
	 */
	public static CollectionBinder getCollectionBinder(
			MemberDetails property,
			boolean isHibernateExtensionMapping,
			MetadataBuildingContext buildingContext) {

		final CollectionBinder binder;
		final CollectionType typeAnnotation = property.getAnnotationUsage( CollectionType.class, buildingContext.getMetadataCollector().getSourceModelBuildingContext() );
		if ( typeAnnotation != null ) {
			binder = createBinderFromCustomTypeAnnotation( property, typeAnnotation, buildingContext );
			// todo (6.0) - technically, these should no longer be needed
			binder.explicitType = typeAnnotation.type().getName();
			for ( Parameter param : typeAnnotation.parameters() ) {
				binder.explicitTypeParameters.put( param.name(), param.value() );
			}
		}
		else {
			binder = createBinderAutomatically( property, buildingContext );
		}
		binder.setIsHibernateExtensionMapping( isHibernateExtensionMapping );
		return binder;
	}

	private static CollectionBinder createBinderAutomatically(MemberDetails property, MetadataBuildingContext context) {
		final CollectionClassification classification = determineCollectionClassification( property, context );
		final CollectionTypeRegistrationDescriptor typeRegistration =
				context.getMetadataCollector().findCollectionTypeRegistration( classification );
		return typeRegistration != null
				? createBinderFromTypeRegistration( property, classification, typeRegistration, context )
				: createBinderFromProperty( property, context );
	}

	private static CollectionBinder createBinderFromTypeRegistration(
			MemberDetails property,
			CollectionClassification classification,
			CollectionTypeRegistrationDescriptor typeRegistration,
			MetadataBuildingContext buildingContext) {
		return createBinder(
				property,
				() -> createCustomType(
						property.getDeclaringType().getName() + "#" + property.getName(),
						typeRegistration.getImplementation(),
						typeRegistration.getParameters(),
						buildingContext
				),
				classification,
				buildingContext
		);
	}

	private static ManagedBean<? extends UserCollectionType> createCustomType(
			String role,
			Class<? extends UserCollectionType> implementation,
			Map<String,String> parameters,
			MetadataBuildingContext buildingContext) {
		final boolean hasParameters = CollectionHelper.isNotEmpty( parameters );
		if ( !buildingContext.getBuildingOptions().isAllowExtensionsInCdi() ) {
			// if deferred container access is enabled, we locally create the user-type
			return createLocalUserCollectionTypeBean( role, implementation, hasParameters, parameters );
		}

		final ManagedBean<? extends UserCollectionType> managedBean =
				buildingContext.getBuildingOptions().getServiceRegistry()
						.requireService( ManagedBeanRegistry.class )
						.getBean( implementation );

		if ( hasParameters ) {
			if ( ParameterizedType.class.isAssignableFrom( managedBean.getBeanClass() ) ) {
				// create a copy of the parameters and create a bean wrapper to delay injecting
				// the parameters, thereby delaying the need to resolve the instance from the
				// wrapped bean
				final Properties copy = new Properties();
				copy.putAll( parameters );
				return new DelayedParameterizedTypeBean<>( managedBean, copy );
			}

			// there were parameters, but the custom-type does not implement the interface
			// used to inject them - log a "warning"
			BootLogging.BOOT_LOGGER.debugf(
					"`@CollectionType` (%s) specified parameters, but the" +
							" implementation does not implement `%s` which is used to inject them - `%s`",
					role,
					ParameterizedType.class.getName(),
					implementation.getName()
			);

			// fall through to returning `managedBean`
		}

		return managedBean;
	}

	private static CollectionBinder createBinderFromProperty(MemberDetails property, MetadataBuildingContext context) {
		final CollectionClassification classification = determineCollectionClassification( property, context );
		return createBinder( property, null, classification, context );
	}

	private static CollectionBinder createBinderFromCustomTypeAnnotation(
			MemberDetails property,
			CollectionType typeAnnotation,
			MetadataBuildingContext buildingContext) {
		determineSemanticJavaType( property );
		final ManagedBean<? extends UserCollectionType> customTypeBean = resolveCustomType(
				property,
				typeAnnotation,
				buildingContext
		);
		return createBinder(
				property,
				() -> customTypeBean,
				customTypeBean.getBeanInstance().getClassification(),
				buildingContext
		);
	}

	public static ManagedBean<? extends UserCollectionType> resolveCustomType(
			MemberDetails property,
			CollectionType typeAnnotation,
			MetadataBuildingContext context) {
		final Properties parameters = extractParameters( typeAnnotation );

		//noinspection unchecked,rawtypes
		return createCustomType(
				property.getDeclaringType().getName() + "." + property.getName(),
				typeAnnotation.type(),
				(Map) parameters,
				context
		);
	}

	private static Properties extractParameters(CollectionType typeAnnotation) {
		final Parameter[] parameterAnnotations = typeAnnotation.parameters();
		final Properties configParams = new Properties( parameterAnnotations.length );
		for ( Parameter parameterAnnotation : parameterAnnotations ) {
			configParams.put( parameterAnnotation.name(), parameterAnnotation.value() );
		}
		return configParams;
	}

	private static CollectionBinder createBinder(
			MemberDetails property,
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanAccess,
			CollectionClassification classification,
			MetadataBuildingContext buildingContext) {
		final TypeDetails elementType = property.getElementType();

		return switch ( classification ) {
			case ARRAY -> elementType.getTypeKind() == TypeDetails.Kind.PRIMITIVE
					? new PrimitiveArrayBinder( customTypeBeanAccess, buildingContext )
					: new ArrayBinder( customTypeBeanAccess, buildingContext );
			case BAG -> new BagBinder( customTypeBeanAccess, buildingContext );
			case ID_BAG -> new IdBagBinder( customTypeBeanAccess, buildingContext );
			case LIST -> new ListBinder( customTypeBeanAccess, buildingContext );
			case MAP, ORDERED_MAP -> new MapBinder( customTypeBeanAccess, false, buildingContext );
			case SORTED_MAP -> new MapBinder( customTypeBeanAccess, true, buildingContext );
			case SET, ORDERED_SET -> new SetBinder( customTypeBeanAccess, false, buildingContext );
			case SORTED_SET -> new SetBinder( customTypeBeanAccess, true, buildingContext );
		};
	}

	private static CollectionClassification determineCollectionClassification(
			MemberDetails property,
			MetadataBuildingContext buildingContext) {
		if ( property.isArray() ) {
			return CollectionClassification.ARRAY;
		}

		final SourceModelBuildingContext sourceModelContext = buildingContext.getMetadataCollector().getSourceModelBuildingContext();

		if ( !property.hasAnnotationUsage( Bag.class, sourceModelContext ) ) {
			return determineCollectionClassification( determineSemanticJavaType( property ), property, buildingContext );
		}

		if ( property.hasAnnotationUsage( OrderColumn.class, sourceModelContext ) ) {
			throw new AnnotationException( "Attribute '"
					+ qualify( property.getDeclaringType().getName(), property.getName() )
					+ "' is annotated '@Bag' and may not also be annotated '@OrderColumn'" );
		}

		if ( property.hasAnnotationUsage( ListIndexBase.class, sourceModelContext ) ) {
			throw new AnnotationException( "Attribute '"
					+ qualify( property.getDeclaringType().getName(), property.getName() )
					+ "' is annotated '@Bag' and may not also be annotated '@ListIndexBase'" );
		}

		final ClassDetails collectionClassDetails = property.getType().determineRawClass();
		final Class<?> collectionJavaType = collectionClassDetails.toJavaClass();
		if ( java.util.List.class.equals( collectionJavaType )
				|| java.util.Collection.class.equals( collectionJavaType ) ) {
			return CollectionClassification.BAG;
		}
		else {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Attribute '%s.%s' of type '%s' is annotated '@Bag' (bags are of type '%s' or '%s')",
							property.getDeclaringType().getName(),
							property.getName(),
							collectionJavaType.getName(),
							java.util.List.class.getName(),
							java.util.Collection.class.getName()
					)
			);
		}
	}

	private static CollectionClassification determineCollectionClassification(
			Class<?> semanticJavaType,
			MemberDetails property,
			MetadataBuildingContext buildingContext) {
		if ( semanticJavaType.isArray() ) {
			return CollectionClassification.ARRAY;
		}

		if ( property.hasDirectAnnotationUsage( CollectionId.class )
				|| property.hasDirectAnnotationUsage( CollectionIdJdbcType.class )
				|| property.hasDirectAnnotationUsage( CollectionIdJdbcTypeCode.class )
				|| property.hasDirectAnnotationUsage( CollectionIdJavaType.class ) ) {
			// explicitly an ID_BAG
			return CollectionClassification.ID_BAG;
		}

		if ( java.util.List.class.isAssignableFrom( semanticJavaType ) ) {
			if ( property.hasDirectAnnotationUsage( OrderColumn.class )
					|| property.hasDirectAnnotationUsage( ListIndexBase.class )
					|| property.hasDirectAnnotationUsage( ListIndexJdbcType.class )
					|| property.hasDirectAnnotationUsage( ListIndexJdbcTypeCode.class )
					|| property.hasDirectAnnotationUsage( ListIndexJavaType.class ) ) {
				// it is implicitly a LIST because of presence of explicit List index config
				return CollectionClassification.LIST;
			}

			if ( property.hasDirectAnnotationUsage( jakarta.persistence.OrderBy.class )
					|| property.hasDirectAnnotationUsage( org.hibernate.annotations.OrderBy.class ) ) {
				return CollectionClassification.BAG;
			}

			final SourceModelBuildingContext sourceModelContext = buildingContext.getMetadataCollector().getSourceModelBuildingContext();
			final ManyToMany manyToMany = property.getAnnotationUsage( ManyToMany.class, sourceModelContext );
			if ( manyToMany != null && !manyToMany.mappedBy().isEmpty() ) {
				// We don't support @OrderColumn on the non-owning side of a many-to-many association.
				return CollectionClassification.BAG;
			}

			final OneToMany oneToMany = property.getAnnotationUsage( OneToMany.class, sourceModelContext );
			if ( oneToMany != null && !oneToMany.mappedBy().isEmpty() ) {
				// Unowned to-many mappings are always considered BAG by default
				return CollectionClassification.BAG;
			}

			// otherwise, return the implicit classification for List attributes
			return buildingContext.getBuildingOptions().getMappingDefaults().getImplicitListClassification();
		}

		if ( java.util.SortedSet.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.SORTED_SET;
		}

		if ( java.util.Set.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.SET;
		}

		if ( java.util.SortedMap.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.SORTED_MAP;
		}

		if ( java.util.Map.class.isAssignableFrom( semanticJavaType ) ) {
			return CollectionClassification.MAP;
		}

		if ( java.util.Collection.class.isAssignableFrom( semanticJavaType ) ) {
			if ( property.hasDirectAnnotationUsage( CollectionId.class ) ) {
				return CollectionClassification.ID_BAG;
			}
			else {
				return CollectionClassification.BAG;
			}
		}

		return null;
	}

	private static Class<?> determineSemanticJavaType(MemberDetails property) {
		if ( property.isPlural() ) {
			final ClassDetails collectionClassDetails = property.getType().determineRawClass();
			final Class<?> collectionClass = collectionClassDetails.toJavaClass();
			return inferCollectionClassFromSubclass( collectionClass );
		}
		else {
			throw new AnnotationException(
					String.format(
							Locale.ROOT,
							"Property '%s.%s' is not a collection and may not be a '@OneToMany', '@ManyToMany', or '@ElementCollection'",
							property.getDeclaringType().getName(),
							property.resolveAttributeName()
					)
			);
		}
	}

	private static Class<?> inferCollectionClassFromSubclass(Class<?> clazz) {
		for ( Class<?> priorityClass : INFERRED_CLASS_PRIORITY ) {
			if ( priorityClass.isAssignableFrom( clazz ) ) {
				return priorityClass;
			}
		}
		return null;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = nullIfEmpty( mappedBy );
	}

	public void setTableBinder(TableBinder tableBinder) {
		this.tableBinder = tableBinder;
	}

	public void setElementType(TypeDetails collectionElementType) {
		this.collectionElementType = collectionElementType;
	}

	public void setTargetEntity(Class<?> targetEntity) {
		final SourceModelBuildingContext sourceModelContext = buildingContext.getMetadataCollector().getSourceModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = sourceModelContext.getClassDetailsRegistry();
		setTargetEntity( classDetailsRegistry.resolveClassDetails( targetEntity.getName() ) );
	}

	public void setTargetEntity(ClassDetails targetEntity) {
		setTargetEntity( new ClassTypeDetailsImpl( targetEntity, TypeDetails.Kind.CLASS ) );
	}

	public void setTargetEntity(TypeDetails targetEntity) {
		this.targetEntity = targetEntity;
	}

	protected abstract Collection createCollection(PersistentClass persistentClass);

	public Collection getCollection() {
		return collection;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setDeclaringClass(ClassDetails declaringClass) {
		this.declaringClass = declaringClass;
		this.declaringClassSet = true;
	}

	public void bind() {
		collection = createCollection( propertyHolder.getPersistentClass() );
		final String role = qualify( propertyHolder.getPath(), propertyName );
		LOG.debugf( "Collection role: %s", role );
		collection.setRole( role );
		collection.setMappedByProperty( mappedBy );

		checkMapKeyColumn();
		bindExplicitTypes();
		//set laziness
		defineFetchingStrategy();
		collection.setMutable( isMutable() );
		//work on association
		boolean isUnowned = isUnownedCollection();
		bindOptimisticLock( isUnowned );
		applySortingAndOrdering();
		bindCache();
		bindLoader();
		detectMappedByProblem( isUnowned );
		collection.setInverse( isUnowned );

		//TODO reduce tableBinder != null and oneToMany
		scheduleSecondPass( isUnowned );
		buildingContext.getMetadataCollector().addCollectionBinding( collection );
		bindProperty();
	}

	private boolean isUnownedCollection() {
		return mappedBy != null;
	}

	private boolean isMutable() {
		return !property.hasDirectAnnotationUsage( Immutable.class );
	}

	private void checkMapKeyColumn() {
		if ( property.hasDirectAnnotationUsage( MapKeyColumn.class ) && hasMapKeyProperty ) {
			throw new AnnotationException( "Collection '" + qualify( propertyHolder.getPath(), propertyName )
					+ "' is annotated both '@MapKey' and '@MapKeyColumn'" );
		}
	}

	private void scheduleSecondPass(boolean isMappedBy) {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		//many to many may need some second pass information
		if ( !oneToMany && isMappedBy ) {
			metadataCollector.addMappedBy( getElementType().getName(), mappedBy, propertyName );
		}

		if ( inheritanceStatePerClass == null) {
			throw new AssertionFailure( "inheritanceStatePerClass not set" );
		}
		metadataCollector.addSecondPass( getSecondPass(), !isMappedBy );
	}

	private void bindOptimisticLock(boolean isMappedBy) {
		final OptimisticLock lockAnn = property.getDirectAnnotationUsage( OptimisticLock.class );
		final boolean includeInOptimisticLockChecks = lockAnn != null ? !lockAnn.excluded() : !isMappedBy;
		collection.setOptimisticLocked( includeInOptimisticLockChecks );
	}

	private void bindCache() {
		//set cache
		if ( isNotEmpty( cacheConcurrencyStrategy ) ) {
			collection.setCacheConcurrencyStrategy( cacheConcurrencyStrategy );
			collection.setCacheRegionName( cacheRegionName );
		}
		collection.setQueryCacheLayout( queryCacheLayout );
	}

	private void bindExplicitTypes() {
		// set explicit type information
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		if ( explicitType != null ) {
			final TypeDefinition typeDef = metadataCollector.getTypeDefinition( explicitType );
			if ( typeDef == null ) {
				collection.setTypeName( explicitType );
				collection.setTypeParameters( explicitTypeParameters );
			}
			else {
				collection.setTypeName( typeDef.getTypeImplementorClass().getName() );
				collection.setTypeParameters( typeDef.getParameters() );
			}
		}
	}

	private void detectMappedByProblem(boolean isMappedBy) {
		if ( isMappedBy
				&& ( property.hasDirectAnnotationUsage( JoinColumn.class )
					|| property.hasDirectAnnotationUsage( JoinColumns.class ) ) ) {
			throw new AnnotationException( "Association '"
					+ qualify( propertyHolder.getPath(), propertyName )
					+ "' is 'mappedBy' another entity and may not specify the '@JoinColumn'" );
		}

		if ( isMappedBy
				&& propertyHolder.getJoinTable( property ) != null ) {
			throw new AnnotationException( "Association '"
					+ qualify( propertyHolder.getPath(), propertyName )
					+ "' is 'mappedBy' another entity and may not specify the '@JoinTable'" );
		}

		if ( !isMappedBy
				&& oneToMany
				&& property.hasDirectAnnotationUsage( OnDelete.class )
				&& !property.hasDirectAnnotationUsage( JoinColumn.class )
				&& !property.hasDirectAnnotationUsage( JoinColumns.class )) {
			throw new AnnotationException( "Unidirectional '@OneToMany' association '"
					+ qualify( propertyHolder.getPath(), propertyName )
					+ "' is annotated '@OnDelete' and must explicitly specify a '@JoinColumn'" );
		}
	}

	private void bindProperty() {
		//property building
		PropertyBinder binder = new PropertyBinder();
		binder.setName( propertyName );
		binder.setValue( collection );
		binder.setCascade( cascadeStrategy );
		if ( cascadeStrategy != null && cascadeStrategy.contains( "delete-orphan" ) ) {
			collection.setOrphanDelete( true );
		}
		binder.setLazy( collection.isLazy() );
		final LazyGroup lazyGroupAnnotation = property.getDirectAnnotationUsage( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			binder.setLazyGroup( lazyGroupAnnotation.value() );
		}
		binder.setAccessType( accessType );
		binder.setMemberDetails( property );
		binder.setInsertable( insertable );
		binder.setUpdatable( updatable );
		binder.setBuildingContext( buildingContext );
		binder.setHolder( propertyHolder );
		Property prop = binder.makeProperty();
		//we don't care about the join stuffs because the column is on the association table.
		if ( !declaringClassSet ) {
			throw new AssertionFailure( "DeclaringClass is not set in CollectionBinder while binding" );
		}
		propertyHolder.addProperty( prop, property, declaringClass );
		binder.callAttributeBindersInSecondPass( prop );
	}

	@SuppressWarnings("deprecation")
	private void bindLoader() {
		//SQL overriding

		final SQLInsert sqlInsert = property.getDirectAnnotationUsage( SQLInsert.class );
		if ( sqlInsert != null ) {
			collection.setCustomSQLInsert(
					sqlInsert.sql().trim(),
					sqlInsert.callable(),
					fromResultCheckStyle( sqlInsert.check() )
			);
			final Class<? extends Expectation> verifier = sqlInsert.verify();
			if ( !Expectation.class.equals( verifier ) ) {
				collection.setInsertExpectation( getDefaultSupplier( verifier ) );
			}
		}

		final SQLUpdate sqlUpdate = property.getDirectAnnotationUsage( SQLUpdate.class );
		if ( sqlUpdate != null ) {
			collection.setCustomSQLUpdate(
					sqlUpdate.sql().trim(),
					sqlUpdate.callable(),
					fromResultCheckStyle( sqlUpdate.check() )
			);
			final Class<? extends Expectation> verifier = sqlUpdate.verify();
			if ( !Expectation.class.equals( verifier ) ) {
				collection.setUpdateExpectation( getDefaultSupplier( verifier ) );
			}
		}

		final SQLDelete sqlDelete = property.getDirectAnnotationUsage( SQLDelete.class );
		if ( sqlDelete != null ) {
			collection.setCustomSQLDelete(
					sqlDelete.sql().trim(),
					sqlDelete.callable(),
					fromResultCheckStyle( sqlDelete.check() )
			);
			final Class<? extends Expectation> verifier = sqlDelete.verify();
			if ( !Expectation.class.equals( verifier ) ) {
				collection.setDeleteExpectation( getDefaultSupplier( verifier ) );
			}
		}

		final SQLDeleteAll sqlDeleteAll = property.getDirectAnnotationUsage( SQLDeleteAll.class );
		if ( sqlDeleteAll != null ) {
			collection.setCustomSQLDeleteAll(
					sqlDeleteAll.sql().trim(),
					sqlDeleteAll.callable(),
					fromResultCheckStyle( sqlDeleteAll.check() )
			);
			final Class<? extends Expectation> verifier = sqlDeleteAll.verify();
			if ( !Expectation.class.equals( verifier ) ) {
				collection.setDeleteAllExpectation( getDefaultSupplier( verifier ) );
			}
		}

		final SQLSelect sqlSelect = property.getDirectAnnotationUsage( SQLSelect.class );
		if ( sqlSelect != null ) {
			final String loaderName = collection.getRole() + "$SQLSelect";
			collection.setLoaderName( loaderName );
			// TODO: pass in the collection element type here
			QueryBinder.bindNativeQuery( loaderName, sqlSelect, null, buildingContext );
		}

		final HQLSelect hqlSelect = property.getDirectAnnotationUsage( HQLSelect.class );
		if ( hqlSelect != null ) {
			final String loaderName = collection.getRole() + "$HQLSelect";
			collection.setLoaderName( loaderName );
			QueryBinder.bindQuery( loaderName, hqlSelect, buildingContext );
		}
	}

	private void applySortingAndOrdering() {

		if ( naturalSort != null && comparatorSort != null ) {
			throw buildIllegalSortCombination();
		}
		final boolean sorted = naturalSort != null || comparatorSort != null;
		final Class<? extends Comparator<?>> comparatorClass;
		if ( naturalSort != null ) {
			comparatorClass = null;
		}
		else if ( comparatorSort != null ) {
			comparatorClass = comparatorSort.value();
		}
		else {
			comparatorClass = null;
		}

		if ( jpaOrderBy != null && ( sqlOrderBy != null || sqlOrder != null ) ) {
			throw buildIllegalOrderCombination();
		}
		boolean ordered = jpaOrderBy != null || sqlOrderBy != null || sqlOrder != null ;
		if ( ordered ) {
			// we can only apply the sql-based order by up front.  The jpa order by has to wait for second pass
			if ( sqlOrderBy != null ) {
				collection.setOrderBy( sqlOrderBy.clause() );
			}
			if ( sqlOrder != null ) {
				collection.setOrderBy( sqlOrder.value() );
			}
		}

		final boolean isSorted = isSortedCollection || sorted;
		if ( isSorted && ordered ) {
			throw buildIllegalOrderAndSortCombination();
		}
		collection.setSorted( isSorted );
		instantiateComparator( collection, comparatorClass );
	}

	private void instantiateComparator(Collection collection, Class<? extends Comparator<?>> comparatorClass) {
		if ( comparatorClass != null ) {
			try {
				collection.setComparator( comparatorClass.newInstance() );
			}
			catch (Exception e) {
				throw new AnnotationException(
						String.format(
								"Could not instantiate comparator class '%s' for collection '%s'",
								comparatorClass.getName(),
								safeCollectionRole()
						),
						e
				);
			}
		}
	}

	private AnnotationException buildIllegalOrderCombination() {
		return new AnnotationException(
				String.format(
						Locale.ROOT,
						"Collection '%s' is annotated both '@%s' and '@%s'",
						safeCollectionRole(),
						jakarta.persistence.OrderBy.class.getName(),
						org.hibernate.annotations.OrderBy.class.getName()
				)
		);
	}

	private AnnotationException buildIllegalOrderAndSortCombination() {
		throw new AnnotationException(
				String.format(
						Locale.ROOT,
						"Collection '%s' is both sorted and ordered (only one of '@%s', '@%s', '@%s', and '@%s' may be used)",
						safeCollectionRole(),
						jakarta.persistence.OrderBy.class.getName(),
						org.hibernate.annotations.OrderBy.class.getName(),
						SortComparator.class.getName(),
						SortNatural.class.getName()
				)
		);
	}

	private AnnotationException buildIllegalSortCombination() {
		return new AnnotationException(
				String.format(
						"Collection '%s' is annotated both '@%s' and '@%s'",
						safeCollectionRole(),
						SortNatural.class.getName(),
						SortComparator.class.getName()
				)
		);
	}

	private void defineFetchingStrategy() {
		handleLazy();
		handleFetch();
		handleFetchProfileOverrides();
	}

	private SourceModelBuildingContext sourceModelContext() {
		return buildingContext.getMetadataCollector().getSourceModelBuildingContext();
	}

	private void handleFetchProfileOverrides() {
		property.forEachAnnotationUsage( FetchProfileOverride.class, sourceModelContext(), (usage) -> {
			buildingContext.getMetadataCollector().addSecondPass( new FetchSecondPass(
					usage,
					propertyHolder,
					propertyName,
					buildingContext
			) );
		} );
	}

	private void handleFetch() {
		final Fetch fetchAnnotation = property.getDirectAnnotationUsage( Fetch.class );
		if ( fetchAnnotation != null ) {
			// Hibernate @Fetch annotation takes precedence
			setHibernateFetchMode( fetchAnnotation.value() );
		}
		else {
			collection.setFetchMode( getFetchMode( getJpaFetchType() ) );
		}
	}

	private void setHibernateFetchMode(org.hibernate.annotations.FetchMode fetchMode) {
		switch ( fetchMode ) {
			case JOIN :
				collection.setFetchMode( FetchMode.JOIN );
				collection.setLazy( false );
				break;
			case SELECT:
				collection.setFetchMode( FetchMode.SELECT );
				break;
			case SUBSELECT:
				collection.setFetchMode( FetchMode.SELECT );
				collection.setSubselectLoadable( true );
				collection.getOwner().setSubselectLoadableCollections( true );
				break;
			default:
				throw new AssertionFailure( "unknown fetch type" );
		}
	}

	private void handleLazy() {
		final FetchType jpaFetchType = getJpaFetchType();
		collection.setLazy( jpaFetchType == LAZY );
		collection.setExtraLazy( false );
	}

	private FetchType getJpaFetchType() {
		final OneToMany oneToMany = property.getDirectAnnotationUsage( OneToMany.class );
		final ManyToMany manyToMany = property.getDirectAnnotationUsage( ManyToMany.class );
		final ElementCollection elementCollection = property.getDirectAnnotationUsage( ElementCollection.class );
		final ManyToAny manyToAny = property.getDirectAnnotationUsage( ManyToAny.class );
		if ( oneToMany != null ) {
			return oneToMany.fetch();
		}

		if ( manyToMany != null ) {
			return manyToMany.fetch();
		}

		if ( elementCollection != null ) {
			return elementCollection.fetch();
		}

		if ( manyToAny != null ) {
			return LAZY;
		}

		throw new AssertionFailure(
				"Define fetch strategy on a property not annotated with @ManyToOne nor @OneToMany nor @CollectionOfElements"
		);
	}

	TypeDetails getElementType() {
		if ( isDefault( targetEntity ) ) {
			if ( collectionElementType != null ) {
				return collectionElementType;
			}
			else {
				throw new AnnotationException( "Collection '" + safeCollectionRole()
						+ "' is declared with a raw type and has an explicit 'targetEntity'" );
			}
		}
		else {
			return targetEntity;
		}
	}

	SecondPass getSecondPass() {
		return new CollectionSecondPass( collection ) {
			@Override
			public void secondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
				bindStarToManySecondPass( persistentClasses );
			}
		};
	}

	/**
	 * return true if it's a Fk, false if it's an association table
	 */
	protected boolean bindStarToManySecondPass(Map<String, PersistentClass> persistentClasses) {
		if ( noAssociationTable( persistentClasses ) ) {
			//this is a foreign key
			bindOneToManySecondPass( persistentClasses );
			return true;
		}
		else {
			//this is an association table
			bindManyToManySecondPass( persistentClasses );
			return false;
		}
	}

	private boolean isReversePropertyInJoin(
			TypeDetails elementType,
			PersistentClass persistentClass,
			Map<String, PersistentClass> persistentClasses) {
		if ( persistentClass != null && isUnownedCollection() ) {
			final Property mappedByProperty;
			try {
				mappedByProperty = persistentClass.getRecursiveProperty( mappedBy );
			}
			catch (MappingException e) {
				throw new AnnotationException(
						"Collection '" + safeCollectionRole()
								+ "' is 'mappedBy' a property named '" + mappedBy
								+ "' which does not exist in the target entity '" + elementType.getName() + "'"
				);
			}
			checkMappedByType( mappedBy, mappedByProperty.getValue(), propertyName, propertyHolder, persistentClasses );
			return persistentClass.getJoinNumber( mappedByProperty ) != 0;
		}
		else {
			return false;
		}
	}

	private boolean noAssociationTable(Map<String, PersistentClass> persistentClasses) {
		final PersistentClass persistentClass = persistentClasses.get( getElementType().getName() );
		return persistentClass != null
			&& !isReversePropertyInJoin( getElementType(), persistentClass, persistentClasses )
			&& oneToMany
			&& !isExplicitAssociationTable
			&& ( implicitJoinColumn() || explicitForeignJoinColumn() );
	}

	private boolean implicitJoinColumn() {
		return joinColumns.getJoinColumns().get(0).isImplicit()
			&& isUnownedCollection(); //implicit @JoinColumn
	}

	private boolean explicitForeignJoinColumn() {
		return !foreignJoinColumns.getJoinColumns().get(0).isImplicit(); //this is an explicit @JoinColumn
	}

	/**
	 * Bind a {@link OneToMany} association.
	 */
	protected void bindOneToManySecondPass(Map<String, PersistentClass> persistentClasses) {
		if ( property == null ) {
			throw new AssertionFailure( "null was passed for argument property" );
		}

		logOneToManySecondPass();

		final org.hibernate.mapping.OneToMany oneToMany =
				new org.hibernate.mapping.OneToMany( buildingContext, getCollection().getOwner() );
		collection.setElement( oneToMany );
		oneToMany.setReferencedEntityName( getElementType().getName() );
		oneToMany.setNotFoundAction( notFoundAction );

		final String referencedEntityName = oneToMany.getReferencedEntityName();
		final PersistentClass associatedClass = persistentClasses.get( referencedEntityName );
		handleJpaOrderBy( collection, associatedClass );
		if ( associatedClass == null ) {
			throw new MappingException(
					String.format( "Association [%s] for entity [%s] references unmapped class [%s]",
							propertyName, propertyHolder.getClassName(), referencedEntityName )
			);
		}
		oneToMany.setAssociatedClass( associatedClass );

		final Map<String, Join> joins = buildingContext.getMetadataCollector().getJoins( referencedEntityName );
		foreignJoinColumns.setPropertyHolder( buildPropertyHolder(
				associatedClass,
				joins,
				foreignJoinColumns.getBuildingContext(),
				inheritanceStatePerClass
		) );
		foreignJoinColumns.setJoins( joins );
		if ( foreignJoinColumns.hasMappedBy() ) {
			collection.setCollectionTable( associatedClass.getRecursiveProperty( foreignJoinColumns.getMappedBy() ).getValue().getTable() );
		}
		else {
			collection.setCollectionTable( foreignJoinColumns.getTable() );
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Mapping collection: %s -> %s", collection.getRole(), collection.getCollectionTable().getName() );
		}

		bindSynchronize();
		bindFilters( false );
		handleWhere( false );

		final PersistentClass targetEntity = persistentClasses.get( getElementType().getName() );
		bindCollectionSecondPass( targetEntity, foreignJoinColumns );

		if ( !collection.isInverse() && !collection.getKey().isNullable() ) {
			createOneToManyBackref( oneToMany );
		}
	}

	private void createOneToManyBackref(org.hibernate.mapping.OneToMany oneToMany) {
		final InFlightMetadataCollector collector = buildingContext.getMetadataCollector();
		// for non-inverse one-to-many, with a not-null fk, add a backref!
		final String entityName = oneToMany.getReferencedEntityName();
		final PersistentClass referenced = collector.getEntityBinding( entityName );
		final Backref backref = new Backref();
		final String backrefName = '_' + foreignJoinColumns.getPropertyName()
				+ '_' + foreignJoinColumns.getColumns().get(0).getLogicalColumnName()
				+ "Backref";
		backref.setName( backrefName );
		backref.setOptional( true );
		backref.setUpdateable( false);
		backref.setSelectable( false );
		backref.setCollectionRole( collection.getRole() );
		backref.setEntityName( collection.getOwner().getEntityName() );
		backref.setValue( collection.getKey() );
		referenced.addProperty( backref );
	}

	private void handleJpaOrderBy(Collection collection, PersistentClass associatedClass) {
		if ( jpaOrderBy != null ) {
			final String orderByFragment = buildOrderByClauseFromHql( jpaOrderBy.value(), associatedClass );
			if ( isNotEmpty( orderByFragment ) ) {
				collection.setOrderBy( orderByFragment );
			}
		}
	}

	private void bindSynchronize() {
		final Synchronize synchronizeAnnotation = property.getDirectAnnotationUsage( Synchronize.class );
		if ( synchronizeAnnotation != null ) {
			final JdbcEnvironment jdbcEnvironment = buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment();
			for ( String table : synchronizeAnnotation.value() ) {
				String physicalName = synchronizeAnnotation.logical()
						? toPhysicalName( jdbcEnvironment, table )
						: table;
				collection.addSynchronizedTable( physicalName );
			}
		}
	}

	private String toPhysicalName(JdbcEnvironment jdbcEnvironment, String logicalName) {
		return buildingContext.getBuildingOptions().getPhysicalNamingStrategy()
				.toPhysicalTableName(
						jdbcEnvironment.getIdentifierHelper().toIdentifier( logicalName ),
						jdbcEnvironment
				)
				.render( jdbcEnvironment.getDialect() );
	}

	private void bindFilters(boolean hasAssociationTable) {
		property.forEachAnnotationUsage( Filter.class, sourceModelContext(), (usage) -> {
			addFilter( hasAssociationTable, usage );
		} );

		property.forEachAnnotationUsage( FilterJoinTable.class, sourceModelContext(), (usage) -> {
			addFilterJoinTable( hasAssociationTable, usage );
		} );
	}

	private void addFilter(boolean hasAssociationTable, Filter filterAnnotation) {
		final Map<String,String> aliasTableMap = new HashMap<>();
		final Map<String,String> aliasEntityMap = new HashMap<>();
		final SqlFragmentAlias[] aliasAnnotations = filterAnnotation.aliases();
		for ( SqlFragmentAlias aliasAnnotation : aliasAnnotations ) {
			final String alias = aliasAnnotation.alias();

			final String table = aliasAnnotation.table();
			if ( isNotEmpty( table ) ) {
				aliasTableMap.put( alias, table );
			}

			final Class<?> entityClassDetails = aliasAnnotation.entity();
			if ( entityClassDetails != void.class ) {
				aliasEntityMap.put( alias, entityClassDetails.getName() );
			}
		}

		if ( hasAssociationTable ) {
			collection.addManyToManyFilter(
					filterAnnotation.name(),
					getFilterCondition( filterAnnotation ),
					filterAnnotation.deduceAliasInjectionPoints(),
					aliasTableMap,
					aliasEntityMap
			);
		}
		else {
			collection.addFilter(
					filterAnnotation.name(),
					getFilterCondition( filterAnnotation ),
					filterAnnotation.deduceAliasInjectionPoints(),
					aliasTableMap,
					aliasEntityMap
			);
		}
	}

	private void handleWhere(boolean hasAssociationTable) {
		final String whereClause = getWhereClause();
		if ( hasAssociationTable ) {
			// A many-to-many association has an association (join) table
			// Collection#setManytoManyWhere is used to set the "where" clause that applies
			// to the many-to-many associated entity table (not the join table).
			collection.setManyToManyWhere( whereClause );
		}
		else {
			// A one-to-many association does not have an association (join) table.
			// Collection#setWhere is used to set the "where" clause that applies to the collection table
			// (which is the associated entity table for a one-to-many association).
			collection.setWhere( whereClause );
		}

		final String whereJoinTableClause = getWhereJoinTableClause();
		if ( isNotEmpty( whereJoinTableClause ) ) {
			if ( hasAssociationTable ) {
				// This is a many-to-many association.
				// Collection#setWhere is used to set the "where" clause that applies to the collection table
				// (which is the join table for a many-to-many association).
				collection.setWhere( whereJoinTableClause );
			}
			else {
				throw new AnnotationException(
						"Collection '" + qualify( propertyHolder.getPath(), propertyName )
								+ "' is an association with no join table and may not have a 'WhereJoinTable'"
				);
			}
		}
	}

	private String getWhereJoinTableClause() {
		final SQLJoinTableRestriction joinTableRestriction = property.getDirectAnnotationUsage( SQLJoinTableRestriction.class );
		return joinTableRestriction != null ? joinTableRestriction.value() : null;
	}

	private String getWhereClause() {
		// There are 2 possible sources of "where" clauses that apply to the associated entity table:
		// 1) from the associated entity mapping; i.e., @Entity @Where(clause="...")
		//    (ignored if useEntityWhereClauseForCollections == false)
		// 2) from the collection mapping;
		//    for one-to-many, e.g., @OneToMany @JoinColumn @Where(clause="...") public Set<Rating> getRatings();
		//    for many-to-many e.g., @ManyToMany @Where(clause="...") public Set<Rating> getRatings();
		return getNonEmptyOrConjunctionIfBothNonEmpty( getWhereOnClassClause(), getWhereOnCollectionClause() );
	}

	private String getWhereOnCollectionClause() {
		final SQLRestriction restrictionOnCollection = getOverridableAnnotation( property, SQLRestriction.class, getBuildingContext() );
		return restrictionOnCollection != null ? restrictionOnCollection.value() : null;
	}

	private String getWhereOnClassClause() {
		final SQLRestriction restrictionOnClass = getOverridableAnnotation(
				property.getAssociatedType().determineRawClass(),
				SQLRestriction.class,
				buildingContext
		);
		return restrictionOnClass != null ? restrictionOnClass.value() : null;
	}

	private void addFilterJoinTable(boolean hasAssociationTable, FilterJoinTable filter) {
		if ( hasAssociationTable ) {
			final Map<String,String> aliasTableMap = new HashMap<>();
			final Map<String,String> aliasEntityMap = new HashMap<>();
			final SqlFragmentAlias[] aliasAnnotations = filter.aliases();
			for ( SqlFragmentAlias aliasAnnotation : aliasAnnotations ) {
				final String alias = aliasAnnotation.alias();

				final String table = aliasAnnotation.table();
				if ( isNotEmpty( table ) ) {
					aliasTableMap.put( alias, table );
				}

				final Class<?> entityClassDetails = aliasAnnotation.entity();
				if ( entityClassDetails != void.class ) {
					aliasEntityMap.put( alias, entityClassDetails.getName() );
				}
			}

			collection.addFilter(
					filter.name(),
					getFilterConditionForJoinTable( filter ),
					filter.deduceAliasInjectionPoints(),
					aliasTableMap,
					aliasEntityMap
			);
		}
		else {
			throw new AnnotationException( "Collection '" + qualify( propertyHolder.getPath(), propertyName )
					+ "' is an association with no join table and may not have a '@FilterJoinTable'" );
		}
	}

	private String getFilterConditionForJoinTable(FilterJoinTable filterJoinTableAnnotation) {
		final String condition = filterJoinTableAnnotation.condition();
		return condition.isEmpty()
				? getDefaultFilterCondition( filterJoinTableAnnotation.name(), filterJoinTableAnnotation )
				: condition;
	}

	private String getFilterCondition(Filter filter) {
		final String condition = filter.condition();
		return condition.isEmpty()
				? getDefaultFilterCondition( filter.name(), filter )
				: condition;
	}

	private String getDefaultFilterCondition(String name, Annotation annotation) {
		final FilterDefinition definition = buildingContext.getMetadataCollector().getFilterDefinition( name );
		if ( definition == null ) {
			throw new AnnotationException( "Collection '" + qualify( propertyHolder.getPath(), propertyName )
					+ "' has a '@" + annotation.annotationType().getSimpleName()
					+ "' for an undefined filter named '" + name + "'" );
		}
		final String defaultCondition = definition.getDefaultFilterCondition();
		if ( isEmpty( defaultCondition ) ) {
			throw new AnnotationException( "Collection '" + qualify( propertyHolder.getPath(), propertyName ) +
					"' has a '@"  + annotation.annotationType().getSimpleName()
					+ "' with no 'condition' and no default condition was given by the '@FilterDef' named '"
					+ name + "'" );
		}
		return defaultCondition;
	}

	public void setCache(Cache cache) {
		if ( cache != null ) {
			cacheRegionName = nullIfEmpty( cache.region() );
			cacheConcurrencyStrategy = EntityBinder.getCacheConcurrencyStrategy( cache.usage() );
		}
		else {
			cacheConcurrencyStrategy = null;
			cacheRegionName = null;
		}
	}

	public void setQueryCacheLayout(QueryCacheLayout queryCacheLayout) {
		this.queryCacheLayout = queryCacheLayout == null ? null : queryCacheLayout.layout();
	}

	public void setOneToMany(boolean oneToMany) {
		this.oneToMany = oneToMany;
	}

	public void setIndexColumn(IndexColumn indexColumn) {
		this.indexColumn = indexColumn;
	}

	public void setMapKey(MapKey key) {
		hasMapKeyProperty = key != null;
		if ( hasMapKeyProperty ) {
			mapKeyPropertyName = nullIfEmpty( key.name() );
		}
	}

	private static String buildOrderByClauseFromHql(String orderByFragment, PersistentClass associatedClass) {
		if ( orderByFragment == null ) {
			return null;
		}
		else if ( orderByFragment.isEmpty() ) {
			//order by id
			return buildOrderById( associatedClass, " asc" );
		}
		else if ( "desc".equalsIgnoreCase( orderByFragment ) ) {
			return buildOrderById( associatedClass, " desc" );
		}
		else {
			return orderByFragment;
		}
	}

	private static String buildOrderById(PersistentClass associatedClass, String order) {
		final StringBuilder sb = new StringBuilder();
		for ( Selectable selectable: associatedClass.getIdentifier().getSelectables() ) {
			sb.append( selectable.getText() );
			sb.append( order );
			sb.append( ", " );
		}
		sb.setLength( sb.length() - 2 );
		return sb.toString();
	}

	public static String adjustUserSuppliedValueCollectionOrderingFragment(String orderByFragment) {
		if ( orderByFragment != null ) {
			orderByFragment = orderByFragment.trim();
			if ( orderByFragment.isEmpty() || orderByFragment.equalsIgnoreCase( "asc" ) ) {
				// This indicates something like either:
				//		`@OrderBy()`
				//		`@OrderBy("asc")
				//
				// JPA says this should indicate an ascending natural ordering of the elements - id for
				//		entity associations or the value(s) for "element collections"
				return "$element$ asc";
			}
			else if ( orderByFragment.equalsIgnoreCase( "desc" ) ) {
				// This indicates:
				//		`@OrderBy("desc")`
				//
				// JPA says this should indicate a descending natural ordering of the elements - id for
				//		entity associations or the value(s) for "element collections"
				return "$element$ desc";
			}
		}

		return orderByFragment;
	}

	private DependantValue buildCollectionKey(AnnotatedJoinColumns joinColumns, OnDeleteAction onDeleteAction) {

		final boolean noConstraintByDefault = buildingContext.getBuildingOptions().isNoConstraintByDefault();

		// give a chance to override the referenced property name
		// has to do that here because the referencedProperty creation happens in a FKSecondPass for ManyToOne yuk!
		overrideReferencedPropertyName( collection, joinColumns );

		final String referencedPropertyName = collection.getReferencedPropertyName();
		//binding key reference using column
		final PersistentClass owner = collection.getOwner();
		final KeyValue keyValue = referencedPropertyName == null
				? owner.getIdentifier()
				: (KeyValue) owner.getReferencedProperty( referencedPropertyName ).getValue();

		final DependantValue key = new DependantValue( buildingContext, collection.getCollectionTable(), keyValue );
		key.setTypeName( null );
		joinColumns.checkPropertyConsistency();
		final List<AnnotatedColumn> columns = joinColumns.getColumns();
		key.setNullable( columns.isEmpty() || columns.get(0).isNullable() );
		key.setUpdateable( columns.isEmpty() || columns.get(0).isUpdatable() );
		key.setOnDeleteAction( onDeleteAction );
		collection.setKey( key );

		if ( property != null ) {
			final CollectionTable collectionTableAnn = property.getDirectAnnotationUsage( CollectionTable.class );
			if ( collectionTableAnn != null ) {
				final ForeignKey foreignKey = collectionTableAnn.foreignKey();
				final ConstraintMode constraintMode = foreignKey.value();
				if ( constraintMode == NO_CONSTRAINT
						|| constraintMode == PROVIDER_DEFAULT && noConstraintByDefault ) {
					key.disableForeignKey();
				}
				else {
					key.setForeignKeyName( nullIfEmpty( foreignKey.name() ) );
					key.setForeignKeyDefinition( nullIfEmpty( foreignKey.foreignKeyDefinition() ) );
					key.setForeignKeyOptions( foreignKey.options() );
					if ( key.getForeignKeyName() == null
							&& key.getForeignKeyDefinition() == null
							&& collectionTableAnn.joinColumns().length == 1 ) {
						final JoinColumn joinColumn = collectionTableAnn.joinColumns()[0];
						final ForeignKey nestedForeignKey = joinColumn.foreignKey();
						key.setForeignKeyName( nullIfEmpty( nestedForeignKey.name() ) );
						key.setForeignKeyDefinition( nullIfEmpty( nestedForeignKey.foreignKeyDefinition() ) );
						key.setForeignKeyOptions( nestedForeignKey.options() );
					}
				}
			}
			else {
				final JoinTable joinTableAnn = property.getDirectAnnotationUsage( JoinTable.class );
				if ( joinTableAnn != null ) {
					final ForeignKey foreignKey = joinTableAnn.foreignKey();
					String foreignKeyName = foreignKey.name();
					String foreignKeyDefinition = foreignKey.foreignKeyDefinition();
					String foreignKeyOptions = foreignKey.options();
					ConstraintMode foreignKeyValue = foreignKey.value();
					final JoinColumn[] joinColumnAnnotations = joinTableAnn.joinColumns();
					if ( !ArrayHelper.isEmpty( joinColumnAnnotations ) ) {
						final JoinColumn joinColumnAnn = joinColumnAnnotations[0];
						final ForeignKey joinColumnForeignKey = joinColumnAnn.foreignKey();
						if ( foreignKeyName.isEmpty() ) {
							foreignKeyName = joinColumnForeignKey.name();
							foreignKeyDefinition = joinColumnForeignKey.foreignKeyDefinition();
							foreignKeyOptions = joinColumnForeignKey.options();
						}
						if ( foreignKeyValue != NO_CONSTRAINT ) {
							foreignKeyValue = joinColumnForeignKey.value();
						}
					}
					if ( foreignKeyValue == NO_CONSTRAINT
							|| foreignKeyValue == PROVIDER_DEFAULT && noConstraintByDefault ) {
						key.disableForeignKey();
					}
					else {
						key.setForeignKeyName( nullIfEmpty( foreignKeyName ) );
						key.setForeignKeyDefinition( nullIfEmpty( foreignKeyDefinition ) );
						key.setForeignKeyOptions( foreignKeyOptions );
					}
				}
				else {
					final String propertyPath = qualify( propertyHolder.getPath(), property.getName() );
					final ForeignKey foreignKey = propertyHolder.getOverriddenForeignKey( propertyPath );
					if ( foreignKey != null ) {
						handleForeignKeyConstraint( noConstraintByDefault, key, foreignKey );
					}
					else {
						final OneToMany oneToManyAnn = property.getDirectAnnotationUsage( OneToMany.class );
						final OnDelete onDeleteAnn = property.getDirectAnnotationUsage( OnDelete.class );
						if ( oneToManyAnn != null
								&& !oneToManyAnn.mappedBy().isEmpty()
								&& ( onDeleteAnn == null || onDeleteAnn.action() != OnDeleteAction.CASCADE ) ) {
							// foreign key should be up to @ManyToOne side
							// @OnDelete generate "on delete cascade" foreign key
							key.disableForeignKey();
						}
						else {
							final JoinColumn joinColumnAnn = property.getDirectAnnotationUsage( JoinColumn.class );
							if ( joinColumnAnn != null ) {
								handleForeignKeyConstraint( noConstraintByDefault, key, joinColumnAnn.foreignKey() );
							}
						}
					}
				}
			}
		}

		return key;
	}

	private static void handleForeignKeyConstraint(
			boolean noConstraintByDefault,
			DependantValue key,
			ForeignKey foreignKey) {
		final ConstraintMode constraintMode = foreignKey.value();
		if ( constraintMode == NO_CONSTRAINT
				|| constraintMode == PROVIDER_DEFAULT && noConstraintByDefault) {
			key.disableForeignKey();
		}
		else {
			key.setForeignKeyName( nullIfEmpty( foreignKey.name() ) );
			key.setForeignKeyDefinition( nullIfEmpty( foreignKey.foreignKeyDefinition() ) );
			key.setForeignKeyOptions( foreignKey.options() );
		}
	}

	private void overrideReferencedPropertyName(Collection collection, AnnotatedJoinColumns joinColumns) {
		if ( isUnownedCollection() && !joinColumns.getColumns().isEmpty() ) {
			final String entityName = joinColumns.getManyToManyOwnerSideEntityName() != null
					? "inverse__" + joinColumns.getManyToManyOwnerSideEntityName()
					: joinColumns.getPropertyHolder().getEntityName();
			final InFlightMetadataCollector collector = buildingContext.getMetadataCollector();
			final String referencedProperty = collector.getPropertyReferencedAssociation( entityName, mappedBy );
			if ( referencedProperty != null ) {
				collection.setReferencedPropertyName( referencedProperty );
				collector.addPropertyReference( collection.getOwnerEntityName(), referencedProperty );
			}
		}
	}

	/**
	 * Bind a {@link ManyToMany} association or {@link ElementCollection}.
	 */
	private void bindManyToManySecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		if ( property == null ) {
			throw new AssertionFailure( "null was passed for argument property" );
		}

		final TypeDetails elementType = getElementType();
		final PersistentClass targetEntity = persistentClasses.get( elementType.getName() ); //null if this is an @ElementCollection
		final String hqlOrderBy = extractHqlOrderBy( jpaOrderBy );

		final boolean isCollectionOfEntities = targetEntity != null;
		final boolean isManyToAny = property.hasDirectAnnotationUsage( ManyToAny.class );

		logManyToManySecondPass( oneToMany, isCollectionOfEntities, isManyToAny );

		//check for user error
		detectManyToManyProblems( elementType, isCollectionOfEntities, isManyToAny );

		if ( isUnownedCollection() ) {
			handleUnownedManyToMany( elementType, targetEntity, isCollectionOfEntities );
		}
		else {
			handleOwnedManyToMany( targetEntity, isCollectionOfEntities );
		}

		bindSynchronize();
		bindFilters( isCollectionOfEntities );
		handleWhere( isCollectionOfEntities );

		bindCollectionSecondPass( targetEntity, joinColumns );


		if ( isCollectionOfEntities ) {
			final ClassDetails collectionClassDetails = property.getType().determineRawClass();
			final Class<?> collectionClass = collectionClassDetails.toJavaClass();
			final AnnotatedJoinColumn firstInverseJoinColumn = inverseJoinColumns.getJoinColumns().get(0);
			final ManyToOne element = handleCollectionOfEntities( elementType, targetEntity, hqlOrderBy );
			bindManyToManyInverseForeignKey(
				targetEntity,
				inverseJoinColumns,
				element,
				java.util.List.class.isAssignableFrom(collectionClass) ? firstInverseJoinColumn.isUnique() : oneToMany
			);
		}
		else if ( isManyToAny ) {
			handleManyToAny();
		}
		else {
			handleElementCollection( elementType, hqlOrderBy );
		}

		checkFilterConditions( collection );
		checkConsistentColumnMutability( collection );
	}

	private void handleElementCollection(TypeDetails elementType, String hqlOrderBy) {
		// 'propertyHolder' is the PropertyHolder for the owner of the collection
		// 'holder' is the CollectionPropertyHolder.
		// 'property' is the collection XProperty

		final boolean isPrimitive = isPrimitive( elementType.getName() );
		final ClassDetails elementClass = isPrimitive
				? null
				: elementType.determineRawClass();
		final AnnotatedClassType classType = annotatedElementType( isEmbedded, isPrimitive, property, elementClass );
		if ( !isPrimitive ) {
			propertyHolder.startingProperty( property );
		}

		final CollectionPropertyHolder holder = buildPropertyHolder(
				collection,
				collection.getRole(),
				elementClass,
				property,
				propertyHolder,
				buildingContext
		);

		final Class<? extends CompositeUserType<?>> compositeUserType =
				resolveCompositeUserType( property, elementClass, buildingContext );
		final boolean isComposite = classType == EMBEDDABLE || compositeUserType != null;
		holder.prepare( property, isComposite );

		if ( isComposite ) {
			handleCompositeCollectionElement( hqlOrderBy, elementType, holder, compositeUserType );
		}
		else {
			handleCollectionElement( elementType, hqlOrderBy, elementClass, holder );
		}
	}

	private void handleCollectionElement(
			TypeDetails elementType,
			String hqlOrderBy,
			ClassDetails elementClass,
			CollectionPropertyHolder holder) {
		final BasicValueBinder elementBinder =
				new BasicValueBinder( BasicValueBinder.Kind.COLLECTION_ELEMENT, buildingContext );
		elementBinder.setReturnedClassName( elementType.getName() );
		final AnnotatedColumns actualColumns = createElementColumnsIfNecessary(
				collection,
				elementColumns,
				Collection.DEFAULT_ELEMENT_COLUMN_NAME,
				null,
				buildingContext
		);
		elementBinder.setColumns( actualColumns );
		elementBinder.setType(
				property,
				elementType,
				collection.getOwnerEntityName(),
				holder.resolveElementAttributeConverterDescriptor( property, elementClass )
		);
		elementBinder.setPersistentClassName( propertyHolder.getEntityName() );
		elementBinder.setAccessType( accessType );
		collection.setElement( elementBinder.make() );
		final String orderBy = adjustUserSuppliedValueCollectionOrderingFragment( hqlOrderBy );
		if ( orderBy != null ) {
			collection.setOrderBy( orderBy );
		}
	}

	private void handleCompositeCollectionElement(
			String hqlOrderBy,
			TypeDetails elementType,
			CollectionPropertyHolder holder,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		//TODO be smart with isNullable
		final AccessType accessType = accessType( property, collection.getOwner() );
		// We create a new entity binder here because it is needed for processing the embeddable
		// Since this is an element collection, there is no real entity binder though,
		// so we just create an "empty shell" for the purpose of avoiding null checks in the fillEmbeddable() method etc.
		final EntityBinder entityBinder = new EntityBinder( buildingContext );
		// Copy over the access type that we resolve for the element collection,
		// so that nested components use the same access type. This fixes HHH-15966
		entityBinder.setPropertyAccessType( accessType );
		final Component component = fillEmbeddable(
				holder,
				getSpecialMembers( elementType ),
				accessType,
				true,
				entityBinder,
				false,
				false,
				true,
				resolveCustomInstantiator( property, elementType, buildingContext ),
				compositeUserType,
				null,
				buildingContext,
				inheritanceStatePerClass
		);
		collection.setElement( component );
		if ( isNotEmpty( hqlOrderBy ) ) {
			final String orderBy = adjustUserSuppliedValueCollectionOrderingFragment( hqlOrderBy );
			if ( orderBy != null ) {
				collection.setOrderBy( orderBy );
			}
		}
	}

	static AccessType accessType(MemberDetails property, PersistentClass owner) {
		final Access accessAnn = property.getDirectAnnotationUsage( Access.class );
		if ( accessAnn != null ) {
			// the attribute is locally annotated with `@Access`, use that
			return accessAnn.value() == PROPERTY
					? AccessType.PROPERTY
					: AccessType.FIELD;
		}

		if ( owner.getIdentifierProperty() != null ) {
			// use the access for the owning entity's id attribute, if one
			return owner.getIdentifierProperty().getPropertyAccessorName().equals( "property" )
					? AccessType.PROPERTY
					: AccessType.FIELD;
		}

		if ( owner.getIdentifierMapper() != null && owner.getIdentifierMapper().getPropertySpan() > 0 ) {
			// use the access for the owning entity's "id mapper", if one
			return owner.getIdentifierMapper().getProperties().get(0).getPropertyAccessorName().equals( "property" )
					? AccessType.PROPERTY
					: AccessType.FIELD;
		}

		// otherwise...
		throw new AssertionFailure( "Unable to guess collection property accessor name" );
	}

	private AnnotatedClassType annotatedElementType(
			boolean isEmbedded,
			boolean isPrimitive,
			MemberDetails property,
			ClassDetails elementClass) {
		if ( isPrimitive ) {
			return NONE;
		}
		else {
			//force in case of attribute override
			final boolean attributeOverride = mappingDefinedAttributeOverrideOnElement(property);
			// todo : force in the case of Convert annotation(s) with embedded paths (beyond key/value prefixes)?
			return isEmbedded || attributeOverride
					? EMBEDDABLE
					: buildingContext.getMetadataCollector().getClassType( elementClass );
		}
	}

	protected boolean mappingDefinedAttributeOverrideOnElement(MemberDetails property) {
		return property.hasDirectAnnotationUsage( AttributeOverride.class )
				|| property.hasDirectAnnotationUsage( AttributeOverrides.class );
	}

	static AnnotatedColumns createElementColumnsIfNecessary(
			Collection collection,
			AnnotatedColumns elementColumns,
			String defaultName,
			Long defaultLength,
			MetadataBuildingContext context) {
		if ( elementColumns == null || elementColumns.getColumns().isEmpty() ) {
			final AnnotatedColumns columns = new AnnotatedColumns();
			columns.setBuildingContext( context );
			final AnnotatedColumn column = new AnnotatedColumn();
			column.setLogicalColumnName( defaultName );
			if ( defaultLength != null ) {
				column.setLength( defaultLength );
			}
			column.setImplicit( false );
			//not following the spec but more clean
			column.setNullable( true );
//			column.setContext( context );
			column.setParent( columns );
			column.bind();
			elementColumns = columns;
		}
		//override the table
		elementColumns.setTable( collection.getCollectionTable() );
		return elementColumns;
	}

	private ManyToOne handleCollectionOfEntities(
			TypeDetails elementType,
			PersistentClass collectionEntity,
			String hqlOrderBy) {
		final ManyToOne element = new ManyToOne( buildingContext,  collection.getCollectionTable() );
		collection.setElement( element );
		element.setReferencedEntityName( elementType.getName() );
		//element.setFetchMode( fetchMode );
		//element.setLazy( fetchMode != FetchMode.JOIN );
		//make the second join non-lazy
		element.setFetchMode( FetchMode.JOIN );
		element.setLazy( false );
		element.setNotFoundAction( notFoundAction );
		// as per 11.1.38 of JPA 2.0 spec, default to primary key if no column is specified by @OrderBy.
		if ( hqlOrderBy != null ) {
			collection.setManyToManyOrdering( buildOrderByClauseFromHql( hqlOrderBy, collectionEntity ) );
		}

		final JoinTable joinTableAnn = property.getDirectAnnotationUsage( JoinTable.class );
		if ( joinTableAnn != null ) {
			final ForeignKey inverseForeignKey = joinTableAnn.inverseForeignKey();
			String foreignKeyName = inverseForeignKey.name();
			String foreignKeyDefinition = inverseForeignKey.foreignKeyDefinition();
			String foreignKeyOptions = inverseForeignKey.options();

			final JoinColumn[] inverseJoinColumns = joinTableAnn.inverseJoinColumns();
			if ( !ArrayHelper.isEmpty( inverseJoinColumns ) ) {
				final JoinColumn joinColumnAnn = inverseJoinColumns[0];
				if ( foreignKeyName.isEmpty() ) {
					final ForeignKey inverseJoinColumnForeignKey = joinColumnAnn.foreignKey();
					foreignKeyName = inverseJoinColumnForeignKey.name();
					foreignKeyDefinition = inverseJoinColumnForeignKey.foreignKeyDefinition();
					foreignKeyOptions = inverseJoinColumnForeignKey.options();
				}
			}

			final ConstraintMode constraintMode = inverseForeignKey.value();
			if ( constraintMode == NO_CONSTRAINT
					|| constraintMode == PROVIDER_DEFAULT
							&& buildingContext.getBuildingOptions().isNoConstraintByDefault() ) {
				element.disableForeignKey();
			}
			else {
				element.setForeignKeyName( nullIfEmpty( foreignKeyName ) );
				element.setForeignKeyDefinition( nullIfEmpty( foreignKeyDefinition ) );
				element.setForeignKeyOptions( foreignKeyOptions );
			}
		}
		return element;
	}

	private void handleManyToAny() {
		//@ManyToAny
		//Make sure that collTyp is never used during the @ManyToAny branch: it will be set to void.class
		final PropertyData inferredData = new PropertyInferredData(
				null,
				declaringClass,
				property,
				"unsupported",
				buildingContext
		);

		final MemberDetails prop = inferredData.getAttributeMember();
		final jakarta.persistence.Column discriminatorColumnAnn = prop.getDirectAnnotationUsage( jakarta.persistence.Column.class );
		final Formula discriminatorFormulaAnn = getOverridableAnnotation( prop, Formula.class, buildingContext );

		//override the table
		inverseJoinColumns.setTable( collection.getCollectionTable() );

		final ManyToAny anyAnn = property.getDirectAnnotationUsage( ManyToAny.class );
		final Any any = buildAnyValue(
				discriminatorColumnAnn,
				discriminatorFormulaAnn,
				inverseJoinColumns,
				inferredData,
				onDeleteAction,
				anyAnn.fetch() == LAZY,
				Nullability.NO_CONSTRAINT,
				propertyHolder,
				new EntityBinder( buildingContext ),
				true,
				buildingContext
		);
		collection.setElement( any );
	}

	private PropertyData getSpecialMembers(TypeDetails elementClass) {
		if ( isMap() ) {
			//"value" is the JPA 2 prefix for map values (used to be "element")
			if ( isHibernateExtensionMapping() ) {
				return new PropertyPreloadedData( AccessType.PROPERTY, "element", elementClass );
			}
			else {
				return new PropertyPreloadedData( AccessType.PROPERTY, "value", elementClass );
			}
		}
		else {
			if ( isHibernateExtensionMapping() ) {
				return new PropertyPreloadedData( AccessType.PROPERTY, "element", elementClass );
			}
			else {
				//"{element}" is not a valid property name => placeholder
				return new PropertyPreloadedData( AccessType.PROPERTY, "{element}", elementClass );
			}
		}
	}

	private void handleOwnedManyToMany(PersistentClass collectionEntity, boolean isCollectionOfEntities) {
		//TODO: only for implicit columns?
		//FIXME NamingStrategy
		final InFlightMetadataCollector collector = buildingContext.getMetadataCollector();
		final PersistentClass owner = collection.getOwner();
		joinColumns.setMappedBy(
				owner.getEntityName(),
				collector.getLogicalTableName( owner.getTable() ),
				collector.getFromMappedBy( owner.getEntityName(), joinColumns.getPropertyName() )
		);
		if ( isEmpty( tableBinder.getName() ) ) {
			//default value
			tableBinder.setDefaultName(
					owner.getClassName(),
					owner.getEntityName(),
					owner.getJpaEntityName(),
					collector.getLogicalTableName( owner.getTable() ),
					collectionEntity != null ? collectionEntity.getClassName() : null,
					collectionEntity != null ? collectionEntity.getEntityName() : null,
					collectionEntity != null ? collectionEntity.getJpaEntityName() : null,
					collectionEntity != null ? collector.getLogicalTableName( collectionEntity.getTable() ) : null,
					joinColumns.getPropertyName()
			);
		}
		tableBinder.setJPA2ElementCollection(
				!isCollectionOfEntities && property.hasDirectAnnotationUsage( ElementCollection.class )
		);
		final Table collectionTable = tableBinder.bind();
		collection.setCollectionTable( collectionTable );
		handleCheckConstraints( collectionTable );
		processSoftDeletes();
	}

	private void handleCheckConstraints(Table collectionTable) {
		property.forEachAnnotationUsage( Check.class, sourceModelContext(), (usage) -> {
			addCheckToCollection( collectionTable, usage );
		} );
		property.forEachAnnotationUsage( jakarta.persistence.JoinTable.class, sourceModelContext(), (usage) -> {
			TableBinder.addTableCheck( collectionTable, usage.check() );
			TableBinder.addTableComment( collectionTable, usage.comment() );
			TableBinder.addTableOptions( collectionTable, usage.options() );
		} );
	}

	private static void addCheckToCollection(Table collectionTable, Check check) {
		final String name = check.name();
		final String constraint = check.constraints();
		collectionTable.addCheck( name.isEmpty()
				? new CheckConstraint( constraint )
				: new CheckConstraint( name, constraint ) );
	}

	private void processSoftDeletes() {
		assert collection.getCollectionTable() != null;

		final SoftDelete softDelete = extractSoftDelete( property, buildingContext );
		if ( softDelete == null ) {
			return;
		}

		SoftDeleteHelper.bindSoftDeleteIndicator(
				softDelete,
				collection,
				collection.getCollectionTable(),
				buildingContext
		);
	}

	private static SoftDelete extractSoftDelete(MemberDetails property, MetadataBuildingContext context) {
		final SoftDelete fromProperty = property.getDirectAnnotationUsage( SoftDelete.class );
		if ( fromProperty != null ) {
			return fromProperty;
		}
		else {
			return extractFromPackage(
					SoftDelete.class,
					property.getDeclaringType(),
					context
			);
		}
	}

	private void handleUnownedManyToMany(
			TypeDetails elementType,
			PersistentClass collectionEntity,
			boolean isCollectionOfEntities) {
		if ( !isCollectionOfEntities) {
			throw new AnnotationException( "Association '" + safeCollectionRole() + "'"
					+ targetEntityMessage( elementType ) );
		}

		joinColumns.setManyToManyOwnerSideEntityName( collectionEntity.getEntityName() );

		final Property otherSideProperty;
		try {
			otherSideProperty = collectionEntity.getRecursiveProperty( mappedBy );
		}
		catch ( MappingException e ) {
			throw new AnnotationException( "Association '" + safeCollectionRole()
					+ "is 'mappedBy' a property named '" + mappedBy
					+ "' which does not exist in the target entity '" + elementType.getName() + "'" );
		}
		final Value otherSidePropertyValue = otherSideProperty.getValue();
		final Table table = otherSidePropertyValue instanceof Collection
				// this is a collection on the other side
				? ( (Collection) otherSidePropertyValue ).getCollectionTable()
				// this is a ToOne with a @JoinTable or a regular property
				: otherSidePropertyValue.getTable();
		collection.setCollectionTable( table );
		processSoftDeletes();

		if ( property.hasDirectAnnotationUsage( Checks.class )
				|| property.hasDirectAnnotationUsage( Check.class ) ) {
			throw new AnnotationException( "Association '" + safeCollectionRole()
					+ " is an unowned collection and may not be annotated '@Check'" );
		}
	}

	private void detectManyToManyProblems(
			TypeDetails elementType,
			boolean isCollectionOfEntities,
			boolean isManyToAny) {

		if ( !isCollectionOfEntities) {
			if ( property.hasDirectAnnotationUsage( ManyToMany.class )
					|| property.hasDirectAnnotationUsage( OneToMany.class ) ) {
				throw new AnnotationException( "Association '" + safeCollectionRole() + "'"
						+ targetEntityMessage( elementType ) );
			}
			else if (isManyToAny) {
				if ( propertyHolder.getJoinTable( property ) == null ) {
					throw new AnnotationException( "Association '" + safeCollectionRole()
							+ "' is a '@ManyToAny' and must specify a '@JoinTable'" );
				}
			}
			else {
				final JoinTable joinTableAnn = propertyHolder.getJoinTable( property );
				if ( joinTableAnn != null && !ArrayHelper.isEmpty( joinTableAnn.inverseJoinColumns() ) ) {
					throw new AnnotationException( "Association '" + safeCollectionRole()
							+ " has a '@JoinTable' with 'inverseJoinColumns' and"
							+ targetEntityMessage( elementType ) );
				}
			}
		}
	}

	static String targetEntityMessage(TypeDetails elementType) {
		final String problem = elementType.determineRawClass().hasDirectAnnotationUsage( Entity.class )
				? " which does not belong to the same persistence unit"
				: " which is not an '@Entity' type";
		return " targets the type '" + elementType.getName() + "'" + problem;
	}

	private Class<? extends EmbeddableInstantiator> resolveCustomInstantiator(
			MemberDetails property,
			TypeDetails propertyClass,
			MetadataBuildingContext context) {
		final org.hibernate.annotations.EmbeddableInstantiator propertyAnnotation
				= property.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.value();
		}

		final ClassDetails rawPropertyClassDetails = propertyClass.determineRawClass();
		final org.hibernate.annotations.EmbeddableInstantiator classAnnotation
				= rawPropertyClassDetails.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.value();
		}

		final Class<?> embeddableClass = rawPropertyClassDetails.toJavaClass();
		if ( embeddableClass != null ) {
			return context.getMetadataCollector().findRegisteredEmbeddableInstantiator( embeddableClass );
		}

		return null;
	}

	private static Class<? extends CompositeUserType<?>> resolveCompositeUserType(
			MemberDetails property,
			ClassDetails returnedClass,
			MetadataBuildingContext context) {
		final CompositeType compositeType = property.getDirectAnnotationUsage( CompositeType.class );
		if ( compositeType != null ) {
			return compositeType.value();
		}

		if ( returnedClass != null ) {
			final Class<?> embeddableClass = returnedClass.toJavaClass();
			return embeddableClass == null
					? null
					: context.getMetadataCollector().findRegisteredCompositeUserType( embeddableClass );
		}

		return null;
	}

	private String extractHqlOrderBy(OrderBy jpaOrderBy) {
		if ( jpaOrderBy != null ) {
			// Null not possible. In case of empty expression, apply default ordering.
			return jpaOrderBy.value();
		}
		else {
			// @OrderBy not found.
			return null;
		}
	}

	private static void checkFilterConditions(Collection collection) {
		//for now it can't happen, but sometime soon...
		if ( ( !collection.getFilters().isEmpty() || isNotEmpty( collection.getWhere() ) )
				&& collection.getFetchMode() == FetchMode.JOIN
				&& !( collection.getElement() instanceof SimpleValue ) //SimpleValue (CollectionOfElements) are always SELECT but it does not matter
				&& collection.getElement().getFetchMode() != FetchMode.JOIN ) {
			throw new MappingException(
					"@ManyToMany or @ElementCollection defining filter or where without join fetching "
							+ "not valid within collection using join fetching[" + collection.getRole() + "]"
			);
		}
	}

	private static void checkConsistentColumnMutability(Collection collection) {
		checkConsistentColumnMutability( collection.getRole(), collection.getKey() );
		checkConsistentColumnMutability( collection.getRole(), collection.getElement() );
	}

	private static void checkConsistentColumnMutability(String collectionRole, Value value) {
		Boolean readOnly = null;
		for ( int i = 0; i < value.getColumnSpan(); i++ ) {
			final boolean insertable = value.isColumnInsertable( i );
			if ( insertable != value.isColumnUpdateable( i ) ) {
				throw new AnnotationException(
						"Join column '" + value.getColumns().get( i ).getName() + "' on collection property '"
								+ collectionRole + "' must be defined with the same insertable and updatable attributes"
				);
			}
			if ( readOnly == null ) {
				readOnly = insertable;
			}
			else if ( readOnly != insertable && !value.getColumns().get( i ).isFormula() ) {
				// We also assert that all join columns have the same mutability (except formulas)
				throw new AnnotationException(
						"All join columns on collection '" + collectionRole + "' should have" +
								" the same insertable and updatable setting"
				);
			}
		}
	}

	private void bindCollectionSecondPass(PersistentClass targetEntity, AnnotatedJoinColumns joinColumns) {

		if ( !isUnownedCollection() ) {
			createSyntheticPropertyReference(
					joinColumns,
					collection.getOwner(),
					collection.getOwner(),
					collection,
					propertyName,
					false,
					buildingContext
			);
		}

		if ( property.hasDirectAnnotationUsage( ElementCollection.class ) ) {
			joinColumns.setElementCollection( true );
		}

		final DependantValue key = buildCollectionKey( joinColumns, onDeleteAction );
		TableBinder.bindForeignKey(
				collection.getOwner(),
				targetEntity,
				joinColumns,
				key,
				false,
				buildingContext
		);
		key.sortProperties();
	}

	public void setOnDeleteActionAction(OnDeleteAction onDeleteAction) {
		this.onDeleteAction = onDeleteAction;
	}

	String safeCollectionRole() {
		return propertyHolder != null ? propertyHolder.getEntityName() + "." + propertyName : "";
	}

	/**
	 * Bind the inverse foreign key of a {@link ManyToMany}, that is, the columns
	 * specified by {@code @JoinTable(inverseJoinColumns=...)}, which are the
	 * columns that reference the target entity of the many-to-many association.
	 * If we are in a {@code mappedBy} case, read the columns from the associated
	 * collection element in the target entity.
	 */
	public void bindManyToManyInverseForeignKey(
			PersistentClass targetEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value,
			boolean unique) {
		// This method is also called for entity valued map keys, so we must consider
		// the mappedBy of the join columns instead of the collection's one
		if ( joinColumns.hasMappedBy() ) {
			bindUnownedManyToManyInverseForeignKey( targetEntity, joinColumns, value );
		}
		else {
			bindOwnedManyToManyForeignKeyMappedBy( targetEntity, joinColumns, value, unique );
		}
	}

	private void bindOwnedManyToManyForeignKeyMappedBy(
			PersistentClass targetEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value,
			boolean unique) { // true when it's actually a logical @OneToMany
		createSyntheticPropertyReference(
				joinColumns,
				targetEntity,
				collection.getOwner(),
				value,
				propertyName,
				true,
				buildingContext
		);
		if ( notFoundAction == NotFoundAction.IGNORE ) {
			value.disableForeignKey();
		}
		TableBinder.bindForeignKey(
				targetEntity,
				collection.getOwner(),
				joinColumns,
				value,
				unique,
				buildingContext
		);
	}

	private void bindUnownedManyToManyInverseForeignKey(
			PersistentClass targetEntity,
			AnnotatedJoinColumns joinColumns,
			SimpleValue value) {
		final Property property = targetEntity.getRecursiveProperty( mappedBy );
		final List<Selectable> mappedByColumns = mappedByColumns( targetEntity, property );
		final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
		for ( Selectable selectable: mappedByColumns ) {
			firstColumn.linkValueUsingAColumnCopy( (Column) selectable, value);
		}
		final String referencedPropertyName = buildingContext.getMetadataCollector()
				.getPropertyReferencedAssociation( targetEntity.getEntityName(), mappedBy );
		if ( referencedPropertyName != null ) {
			//TODO always a many to one?
			( (ManyToOne) value).setReferencedPropertyName( referencedPropertyName );
			buildingContext.getMetadataCollector()
					.addUniquePropertyReference( targetEntity.getEntityName(), referencedPropertyName );
		}
		( (ManyToOne) value).setReferenceToPrimaryKey( referencedPropertyName == null );
		value.createForeignKey();
	}

	private static List<Selectable> mappedByColumns(PersistentClass referencedEntity, Property property) {
		if ( property.getValue() instanceof Collection ) {
			return ( (Collection) property.getValue() ).getKey().getSelectables();
		}
		else {
			//find the appropriate reference key, can be in a join
			KeyValue key = null;
			for ( Join join : referencedEntity.getJoins() ) {
				if ( join.containsProperty(property) ) {
					key = join.getKey();
					break;
				}
			}
			if ( key == null ) {
				key = property.getPersistentClass().getIdentifier();
			}
			return key.getSelectables();
		}
	}

	public void setFkJoinColumns(AnnotatedJoinColumns annotatedJoinColumns) {
		this.foreignJoinColumns = annotatedJoinColumns;
	}

	public void setExplicitAssociationTable(boolean isExplicitAssociationTable) {
		this.isExplicitAssociationTable = isExplicitAssociationTable;
	}

	public void setElementColumns(AnnotatedColumns elementColumns) {
		this.elementColumns = elementColumns;
	}

	public void setEmbedded(boolean annotationPresent) {
		this.isEmbedded = annotationPresent;
	}

	public void setProperty(MemberDetails property) {
		this.property = property;
	}

	public NotFoundAction getNotFoundAction() {
		return notFoundAction;
	}

	public void setNotFoundAction(NotFoundAction notFoundAction) {
		this.notFoundAction = notFoundAction;
	}

	public void setMapKeyColumns(AnnotatedColumns mapKeyColumns) {
		this.mapKeyColumns = mapKeyColumns;
	}

	public void setMapKeyManyToManyColumns(AnnotatedJoinColumns mapJoinColumns) {
		this.mapKeyManyToManyColumns = mapJoinColumns;
	}

	public void setLocalGenerators(Map<String, IdentifierGeneratorDefinition> localGenerators) {
		this.localGenerators = localGenerators;
	}

	private void logOneToManySecondPass() {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Binding a OneToMany: %s through a foreign key", safeCollectionRole() );
		}
	}

	private void logManyToManySecondPass(
			boolean isOneToMany,
			boolean isCollectionOfEntities,
			boolean isManyToAny) {
		if ( LOG.isDebugEnabled() ) {
			if ( isCollectionOfEntities && isOneToMany ) {
				LOG.debugf( "Binding a OneToMany: %s through an association table", safeCollectionRole() );
			}
			else if ( isCollectionOfEntities ) {
				LOG.debugf( "Binding a ManyToMany: %s", safeCollectionRole() );
			}
			else if ( isManyToAny ) {
				LOG.debugf( "Binding a ManyToAny: %s", safeCollectionRole() );
			}
			else {
				LOG.debugf( "Binding a collection of element: %s", safeCollectionRole() );
			}
		}
	}

}
