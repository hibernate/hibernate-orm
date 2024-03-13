/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
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
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.Loader;
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
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.BootLogging;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.TypeDefinition;
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
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
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
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.UniqueConstraint;

import static jakarta.persistence.AccessType.PROPERTY;
import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static jakarta.persistence.ConstraintMode.PROVIDER_DEFAULT;
import static jakarta.persistence.FetchType.EAGER;
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
import static org.hibernate.boot.model.internal.BinderHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.isDefault;
import static org.hibernate.boot.model.internal.BinderHelper.isPrimitive;
import static org.hibernate.boot.model.internal.EmbeddableBinder.fillEmbeddable;
import static org.hibernate.boot.model.internal.GeneratorBinder.buildGenerators;
import static org.hibernate.boot.model.internal.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.boot.model.source.internal.hbm.ModelBinder.useEntityWhereClauseForCollections;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.fromResultCheckStyle;
import static org.hibernate.internal.util.ReflectHelper.getDefaultSupplier;
import static org.hibernate.internal.util.StringHelper.getNonEmptyOrConjunctionIfBothNonEmpty;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.mapping.MappingHelper.createLocalUserCollectionTypeBean;

/**
 * Base class for stateful binders responsible for producing mapping model objects of type {@link Collection}.
 *
 * @author inger
 * @author Emmanuel Bernard
 */
public abstract class CollectionBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, CollectionBinder.class.getName());

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
	private ClassDetails declaringClass;
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

	private AnnotationUsage<jakarta.persistence.OrderBy> jpaOrderBy;
	private AnnotationUsage<org.hibernate.annotations.OrderBy> sqlOrderBy;
	private AnnotationUsage<SQLOrder> sqlOrder;
	private AnnotationUsage<SortNatural> naturalSort;
	private AnnotationUsage<SortComparator> comparatorSort;

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
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			AnnotatedJoinColumns joinColumns) {
		final AnnotationUsage<OneToMany> oneToManyAnn = property.getAnnotationUsage( OneToMany.class );
		final AnnotationUsage<ManyToMany> manyToManyAnn = property.getAnnotationUsage( ManyToMany.class );
		final AnnotationUsage<ElementCollection> elementCollectionAnn = property.getAnnotationUsage( ElementCollection.class );
		checkAnnotations( propertyHolder, inferredData, property, oneToManyAnn, manyToManyAnn, elementCollectionAnn );

		final CollectionBinder collectionBinder = getCollectionBinder( property, hasMapKeyAnnotation( property ), context );
		collectionBinder.setIndexColumn( getIndexColumn( propertyHolder, inferredData, entityBinder, context, property ) );
		collectionBinder.setMapKey( property.getAnnotationUsage( MapKey.class ) );
		collectionBinder.setPropertyName( inferredData.getPropertyName() );
		collectionBinder.setJpaOrderBy( property.getAnnotationUsage( OrderBy.class ) );
		collectionBinder.setSqlOrderBy( getOverridableAnnotation( property, org.hibernate.annotations.OrderBy.class, context ) );
		collectionBinder.setSqlOrder( getOverridableAnnotation( property, SQLOrder.class, context ) );
		collectionBinder.setNaturalSort( property.getAnnotationUsage( SortNatural.class ) );
		collectionBinder.setComparatorSort( property.getAnnotationUsage( SortComparator.class ) );
		collectionBinder.setCache( property.getAnnotationUsage( Cache.class ) );
		collectionBinder.setQueryCacheLayout( property.getAnnotationUsage( QueryCacheLayout.class ) );
		collectionBinder.setPropertyHolder(propertyHolder);

		collectionBinder.setNotFoundAction( notFoundAction( propertyHolder, inferredData, property, manyToManyAnn ) );
		collectionBinder.setElementType( inferredData.getClassOrElementType() );
		collectionBinder.setAccessType( inferredData.getDefaultAccess() );
		collectionBinder.setEmbedded( property.hasAnnotationUsage( Embedded.class ) );
		collectionBinder.setProperty( property );
		collectionBinder.setOnDeleteActionAction( onDeleteAction( property ) );
		collectionBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
		collectionBinder.setDeclaringClass( inferredData.getDeclaringClass() );

//		final Comment comment = property.getAnnotation( Comment.class );
		final AnnotationUsage<Cascade> hibernateCascade = property.getAnnotationUsage( Cascade.class );

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

		if ( property.hasAnnotationUsage( CollectionId.class ) ) {
			//do not compute the generators unless necessary
			final HashMap<String, IdentifierGeneratorDefinition> localGenerators = new HashMap<>(classGenerators);
			localGenerators.putAll( buildGenerators( property, context ) );
			collectionBinder.setLocalGenerators( localGenerators );

		}
		collectionBinder.bind();
	}

	private static TypeDetails determineElementType(PropertyData inferredData) {
		return inferredData.getClassOrElementType();
	}

	private static NotFoundAction notFoundAction(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MemberDetails property,
			AnnotationUsage<ManyToMany> manyToManyAnn) {
		final AnnotationUsage<NotFound> notFound = property.getAnnotationUsage( NotFound.class );
		if ( notFound != null ) {
			if ( manyToManyAnn == null ) {
				throw new AnnotationException( "Collection '" + getPath(propertyHolder, inferredData)
						+ "' annotated '@NotFound' is not a '@ManyToMany' association" );
			}
			return notFound.getEnum( "action" );
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
				mapKeyJoinColumnAnnotations( propertyHolder, inferredData, property, context ),
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
		final AnnotationUsage<OnDelete> onDelete = property.getAnnotationUsage( OnDelete.class );
		return onDelete == null ? null : onDelete.getEnum( "action" );
	}

	private static PropertyData virtualPropertyData(PropertyData inferredData, MemberDetails property) {
		//do not use "element" if you are a JPA 2 @ElementCollection, only for legacy Hibernate mappings
		return property.hasAnnotationUsage( ElementCollection.class )
				? inferredData
				: new WrappedInferredData(inferredData, "element" );
	}

	private static void checkAnnotations(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MemberDetails property,
			AnnotationUsage<OneToMany> oneToMany,
			AnnotationUsage<ManyToMany> manyToMany,
			AnnotationUsage<ElementCollection> elementCollection) {
		if ( ( oneToMany != null || manyToMany != null || elementCollection != null )
				&& isToManyAssociationWithinEmbeddableCollection( propertyHolder ) ) {
			throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData ) +
					"' belongs to an '@Embeddable' class that is contained in an '@ElementCollection' and may not be a "
					+ annotationName( oneToMany, manyToMany, elementCollection ));
		}

		if ( oneToMany != null && property.hasAnnotationUsage( SoftDelete.class ) ) {
			throw new UnsupportedMappingException(
					"@SoftDelete cannot be applied to @OneToMany - " +
							property.getDeclaringType().getName() + "." + property.getName()
			);
		}

		if ( property.hasAnnotationUsage( OrderColumn.class )
				&& manyToMany != null
				&& StringHelper.isNotEmpty( manyToMany.getString( "mappedBy" ) ) ) {
			throw new AnnotationException("Collection '" + getPath( propertyHolder, inferredData ) +
					"' is the unowned side of a bidirectional '@ManyToMany' and may not have an '@OrderColumn'");
		}

		if ( manyToMany != null || elementCollection != null ) {
			if ( property.hasAnnotationUsage( JoinColumn.class ) || property.hasAnnotationUsage( JoinColumns.class ) ) {
				throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData )
						+ "' is a " + annotationName( oneToMany, manyToMany, elementCollection )
						+ " and is directly annotated '@JoinColumn'"
						+ " (specify '@JoinColumn' inside '@JoinTable' or '@CollectionTable')" );
			}
		}
	}

	private static String annotationName(
			AnnotationUsage<OneToMany> oneToMany,
			AnnotationUsage<ManyToMany> manyToMany,
			AnnotationUsage<ElementCollection> elementCollection) {
		return oneToMany != null ? "'@OneToMany'" : manyToMany != null ? "'@ManyToMany'" : "'@ElementCollection'";
	}

	private static IndexColumn getIndexColumn(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			MemberDetails property) {
		return IndexColumn.fromAnnotations(
				property.getAnnotationUsage( OrderColumn.class ),
				property.getAnnotationUsage( org.hibernate.annotations.IndexColumn.class ),
				property.getAnnotationUsage( ListIndexBase.class ),
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
			AnnotationUsage<OneToMany> oneToManyAnn,
			AnnotationUsage<ManyToMany> manyToManyAnn,
			AnnotationUsage<ElementCollection> elementCollectionAnn,
			CollectionBinder collectionBinder,
			AnnotationUsage<Cascade> hibernateCascade) {

		//TODO enhance exception with @ManyToAny and @CollectionOfElements
		if ( oneToManyAnn != null && manyToManyAnn != null ) {
			throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData )
					+ "' is annotated both '@OneToMany' and '@ManyToMany'" );
		}
		final String mappedBy;
		final ReflectionManager reflectionManager = context.getBootstrapContext().getReflectionManager();
		if ( oneToManyAnn != null ) {
			if ( joinColumns.isSecondary() ) {
				throw new AnnotationException( "Collection '" + getPath( propertyHolder, inferredData )
						+ "' has foreign key in secondary table" );
			}
			collectionBinder.setFkJoinColumns( joinColumns );
			mappedBy = nullIfEmpty( oneToManyAnn.getString( "mappedBy" ) );
			collectionBinder.setTargetEntity( oneToManyAnn.getClassDetails( "targetEntity" ) );
			collectionBinder.setCascadeStrategy( getCascadeStrategy(
					oneToManyAnn.getList( "cascade" ),
					hibernateCascade,
					oneToManyAnn.getBoolean( "orphanRemoval" ),
					false
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
			final ClassDetails targetClassDetails = elementCollectionAnn.getClassDetails( "targetClass" );
			collectionBinder.setTargetEntity( targetClassDetails );
			collectionBinder.setOneToMany( false );
		}
		else if ( manyToManyAnn != null ) {
			mappedBy = nullIfEmpty( manyToManyAnn.getString( "mappedBy" ) );
			collectionBinder.setTargetEntity( manyToManyAnn.getClassDetails( "targetEntity" ) );
			collectionBinder.setCascadeStrategy( getCascadeStrategy(
					manyToManyAnn.getList( "cascade" ),
					hibernateCascade,
					false,
					false
			) );
			collectionBinder.setOneToMany( false );
		}
		else if ( property.hasAnnotationUsage( ManyToAny.class ) ) {
			mappedBy = null;
			collectionBinder.setTargetEntity( ClassDetails.VOID_CLASS_DETAILS );
			collectionBinder.setCascadeStrategy( getCascadeStrategy(
					null,
					hibernateCascade,
					false,
					false
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
		return property.hasAnnotationUsage(MapKeyJavaType.class)
			|| property.hasAnnotationUsage(MapKeyJdbcType.class)
			|| property.hasAnnotationUsage(MapKeyJdbcTypeCode.class)
			|| property.hasAnnotationUsage(MapKeyMutability.class)
			|| property.hasAnnotationUsage(MapKey.class)
			|| property.hasAnnotationUsage(MapKeyType.class);
	}

	private static boolean isToManyAssociationWithinEmbeddableCollection(PropertyHolder propertyHolder) {
		if ( propertyHolder instanceof ComponentPropertyHolder ) {
			ComponentPropertyHolder componentPropertyHolder = (ComponentPropertyHolder) propertyHolder;
			return componentPropertyHolder.isWithinElementCollection();
		}
		else {
			return false;
		}
	}

	private static AnnotatedColumns elementColumns(
			PropertyHolder propertyHolder,
			Nullability nullability,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			MemberDetails property,
			PropertyData virtualProperty) {
//			Comment comment) {
		if ( property.hasAnnotationUsage( jakarta.persistence.Column.class ) ) {
			return buildColumnFromAnnotation(
					property.getAnnotationUsage( jakarta.persistence.Column.class ),
					null,
//					comment,
					nullability,
					propertyHolder,
					virtualProperty,
					entityBinder.getSecondaryTables(),
					context
			);
		}
		else if ( property.hasAnnotationUsage( Formula.class ) ) {
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
		else if ( property.hasAnnotationUsage( Columns.class ) ) {
			return buildColumnsFromAnnotations(
					property.getAnnotationUsage( Columns.class ).getList( "columns" ),
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

	private static List<AnnotationUsage<JoinColumn>> mapKeyJoinColumnAnnotations(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MemberDetails property,
			MetadataBuildingContext context) {
		if ( property.hasAnnotationUsage( MapKeyJoinColumns.class ) ) {
			if ( property.hasAnnotationUsage( MapKeyJoinColumn.class ) ) {
				throw new AnnotationException(
						"Property '" + getPath( propertyHolder, inferredData )
								+ "' is annotated with both '@MapKeyJoinColumn' and '@MapKeyJoinColumns'"
				);
			}
			final List<AnnotationUsage<MapKeyJoinColumn>> mapKeyJoinColumns = property.getAnnotationUsage( MapKeyJoinColumns.class ).getList( "value" );
			final List<AnnotationUsage<JoinColumn>> joinKeyColumns = CollectionHelper.arrayList( mapKeyJoinColumns.size() );
			int index = 0;
			for ( AnnotationUsage<MapKeyJoinColumn> mapKeyJoinColumn : mapKeyJoinColumns ) {
				final MutableAnnotationUsage<JoinColumn> joinColumn
						= MapKeyJoinColumnDelegator.fromMapKeyJoinColumn( mapKeyJoinColumn, property, context );
				joinKeyColumns.add( joinColumn );
				index++;
			}
			return joinKeyColumns;
		}
		else if ( property.hasAnnotationUsage( MapKeyJoinColumn.class ) ) {
			final MutableAnnotationUsage<JoinColumn> delegator = MapKeyJoinColumnDelegator.fromMapKeyJoinColumn(
					property.getAnnotationUsage( MapKeyJoinColumn.class ),
					property,
					context
			);
			return List.of( delegator );
		}
		else {
			return null;
		}
	}

	private static AnnotatedColumns mapKeyColumns(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			MemberDetails property) {
//			Comment comment) {
		//noinspection unchecked,rawtypes,RedundantCast
		return buildColumnsFromAnnotations(
				property.hasAnnotationUsage( MapKeyColumn.class )
						? (List) List.of( property.getAnnotationUsage( MapKeyColumn.class ) )
						: null,
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
		final AnnotationUsage<JoinTable> assocTable = propertyHolder.getJoinTable( property );
		final AnnotationUsage<CollectionTable> collectionTable = property.getAnnotationUsage( CollectionTable.class );

		final List<AnnotationUsage<JoinColumn>> annJoins;
		final List<AnnotationUsage<JoinColumn>> annInverseJoins;
		if ( assocTable != null || collectionTable != null ) {
			final String catalog;
			final String schema;
			final String tableName;
			final List<AnnotationUsage<UniqueConstraint>> uniqueConstraints;
			final List<AnnotationUsage<JoinColumn>> joins;
			final List<AnnotationUsage<JoinColumn>> inverseJoins;
			final List<AnnotationUsage<Index>> jpaIndexes;

			//JPA 2 has priority
			if ( collectionTable != null ) {
				catalog = collectionTable.getString( "catalog" );
				schema = collectionTable.getString( "schema" );
				tableName = collectionTable.getString( "name" );
				uniqueConstraints = collectionTable.getList( "uniqueConstraints" );
				joins = collectionTable.getList( "joinColumns" );
				inverseJoins = null;
				jpaIndexes = collectionTable.getList( "indexes" );
			}
			else {
				catalog = assocTable.getString( "catalog" );
				schema = assocTable.getString( "schema" );
				tableName = assocTable.getString( "name" );
				uniqueConstraints = assocTable.getList( "uniqueConstraints" );
				joins = assocTable.getList( "joinColumns" );
				inverseJoins = assocTable.getList( "inverseJoinColumns" );
				jpaIndexes = assocTable.getList( "indexes" );
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
			//set check constraint in the second pass
			annJoins = joins.isEmpty() ? null : joins;
			annInverseJoins = inverseJoins == null || inverseJoins.isEmpty() ? null : inverseJoins;
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

	public void setJpaOrderBy(AnnotationUsage<jakarta.persistence.OrderBy> jpaOrderBy) {
		this.jpaOrderBy = jpaOrderBy;
	}

	@SuppressWarnings("removal")
	public void setSqlOrderBy(AnnotationUsage<org.hibernate.annotations.OrderBy> sqlOrderBy) {
		this.sqlOrderBy = sqlOrderBy;
	}

	public void setSqlOrder(AnnotationUsage<SQLOrder> sqlOrder) {
		this.sqlOrder = sqlOrder;
	}

	public void setNaturalSort(AnnotationUsage<SortNatural> naturalSort) {
		this.naturalSort = naturalSort;
	}

	public void setComparatorSort(AnnotationUsage<SortComparator> comparatorSort) {
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
		final AnnotationUsage<CollectionType> typeAnnotation = property.getAnnotationUsage( CollectionType.class );
		if ( typeAnnotation != null ) {
			binder = createBinderFromCustomTypeAnnotation( property, typeAnnotation, buildingContext );
			// todo (6.0) - technically, these should no longer be needed
			binder.explicitType = typeAnnotation.getClassDetails( "type" ).getClassName();
			for ( AnnotationUsage<Parameter> param : typeAnnotation.<AnnotationUsage<Parameter>>getList( "parameters" ) ) {
				binder.explicitTypeParameters.put( param.getString( "name" ), param.getString( "value" ) );
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
			AnnotationUsage<CollectionType> typeAnnotation,
			MetadataBuildingContext buildingContext) {
		determineSemanticJavaType( property, buildingContext );
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
			AnnotationUsage<CollectionType> typeAnnotation,
			MetadataBuildingContext context) {
		final Properties parameters = extractParameters( typeAnnotation );

		//noinspection unchecked,rawtypes
		return createCustomType(
				property.getDeclaringType().getName() + "." + property.getName(),
				typeAnnotation.getClassDetails( "type" ).toJavaClass(),
				(Map) parameters,
				context
		);
	}

	private static Properties extractParameters(AnnotationUsage<CollectionType> typeAnnotation) {
		final List<AnnotationUsage<Parameter>> parameterAnnotations = typeAnnotation.getList( "parameters" );
		final Properties configParams = new Properties( parameterAnnotations.size() );
		for ( AnnotationUsage<Parameter> parameterAnnotation : parameterAnnotations ) {
			configParams.put( parameterAnnotation.getString( "name" ), parameterAnnotation.getString( "value" ) );
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

		if ( !property.hasAnnotationUsage( Bag.class ) ) {
			return determineCollectionClassification( determineSemanticJavaType( property, buildingContext ), property, buildingContext );
		}

		if ( property.hasAnnotationUsage( OrderColumn.class ) ) {
			throw new AnnotationException( "Attribute '"
					+ qualify( property.getDeclaringType().getName(), property.getName() )
					+ "' is annotated '@Bag' and may not also be annotated '@OrderColumn'" );
		}

		if ( property.hasAnnotationUsage( ListIndexBase.class ) ) {
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

		if ( property.hasAnnotationUsage( CollectionId.class )
				|| property.hasAnnotationUsage( CollectionIdJdbcType.class )
				|| property.hasAnnotationUsage( CollectionIdJdbcTypeCode.class )
				|| property.hasAnnotationUsage( CollectionIdJavaType.class ) ) {
			// explicitly an ID_BAG
			return CollectionClassification.ID_BAG;
		}

		if ( java.util.List.class.isAssignableFrom( semanticJavaType ) ) {
			if ( property.hasAnnotationUsage( OrderColumn.class )
					|| property.hasAnnotationUsage( org.hibernate.annotations.IndexColumn.class )
					|| property.hasAnnotationUsage( ListIndexBase.class )
					|| property.hasAnnotationUsage( ListIndexJdbcType.class )
					|| property.hasAnnotationUsage( ListIndexJdbcTypeCode.class )
					|| property.hasAnnotationUsage( ListIndexJavaType.class ) ) {
				// it is implicitly a LIST because of presence of explicit List index config
				return CollectionClassification.LIST;
			}

			if ( property.hasAnnotationUsage( jakarta.persistence.OrderBy.class )
					|| property.hasAnnotationUsage( org.hibernate.annotations.OrderBy.class ) ) {
				return CollectionClassification.BAG;
			}

			final AnnotationUsage<ManyToMany> manyToMany = property.getAnnotationUsage( ManyToMany.class );
			if ( manyToMany != null && !manyToMany.getString( "mappedBy" ).isEmpty() ) {
				// We don't support @OrderColumn on the non-owning side of a many-to-many association.
				return CollectionClassification.BAG;
			}

			final AnnotationUsage<OneToMany> oneToMany = property.getAnnotationUsage( OneToMany.class );
			if ( oneToMany != null && !oneToMany.getString( "mappedBy" ).isEmpty() ) {
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
			if ( property.hasAnnotationUsage( CollectionId.class ) ) {
				return CollectionClassification.ID_BAG;
			}
			else {
				return CollectionClassification.BAG;
			}
		}

		return null;
	}

	private static Class<?> determineSemanticJavaType(MemberDetails property, MetadataBuildingContext buildingContext) {
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
							property.getName()
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
		return !property.hasAnnotationUsage( Immutable.class );
	}

	private void checkMapKeyColumn() {
		if ( property.hasAnnotationUsage( MapKeyColumn.class ) && hasMapKeyProperty ) {
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
		final AnnotationUsage<OptimisticLock> lockAnn = property.getAnnotationUsage( OptimisticLock.class );
		final boolean includeInOptimisticLockChecks = lockAnn != null ? !lockAnn.getBoolean( "excluded" ) : !isMappedBy;
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
				&& ( property.hasAnnotationUsage( JoinColumn.class )
					|| property.hasAnnotationUsage( JoinColumns.class ) ) ) {
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
				&& property.hasAnnotationUsage( OnDelete.class )
				&& !property.hasAnnotationUsage( JoinColumn.class )
				&& !property.hasAnnotationUsage( JoinColumns.class )) {
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
		final AnnotationUsage<LazyGroup> lazyGroupAnnotation = property.getAnnotationUsage( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			binder.setLazyGroup( lazyGroupAnnotation.getString( "value" ) );
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
	}

	@SuppressWarnings("deprecation")
	private void bindLoader() {
		//SQL overriding

		final AnnotationUsage<SQLInsert> sqlInsert = property.getAnnotationUsage( SQLInsert.class );
		if ( sqlInsert != null ) {
			collection.setCustomSQLInsert(
					sqlInsert.getString( "sql" ).trim(),
					sqlInsert.getBoolean( "callable" ),
					fromResultCheckStyle( sqlInsert.getEnum( "check" ) )
			);
			if ( sqlInsert.verify() != Expectation.class ) {
				collection.setInsertExpectation( getDefaultSupplier( sqlInsert.verify() ) );
			}
		}

		final AnnotationUsage<SQLUpdate> sqlUpdate = property.getAnnotationUsage( SQLUpdate.class );
		if ( sqlUpdate != null ) {
			collection.setCustomSQLUpdate(
					sqlUpdate.getString( "sql" ).trim(),
					sqlUpdate.getBoolean( "callable" ),
					fromResultCheckStyle( sqlUpdate.getEnum( "check" ) )
			);
			if ( sqlUpdate.verify() != Expectation.class ) {
				collection.setUpdateExpectation( getDefaultSupplier( sqlUpdate.verify() ) );
			}
		}

		final AnnotationUsage<SQLDelete> sqlDelete = property.getAnnotationUsage( SQLDelete.class );
		if ( sqlDelete != null ) {
			collection.setCustomSQLDelete(
					sqlDelete.getString( "sql" ).trim(),
					sqlDelete.getBoolean( "callable" ),
					fromResultCheckStyle( sqlDelete.getEnum( "check" ) )
			);
			if ( sqlDelete.verify() != Expectation.class ) {
				collection.setDeleteExpectation( getDefaultSupplier( sqlDelete.verify() ) );
			}
		}

		final AnnotationUsage<SQLDeleteAll> sqlDeleteAll = property.getAnnotationUsage( SQLDeleteAll.class );
		if ( sqlDeleteAll != null ) {
			collection.setCustomSQLDeleteAll(
					sqlDeleteAll.getString( "sql" ).trim(),
					sqlDeleteAll.getBoolean( "callable" ),
					fromResultCheckStyle( sqlDeleteAll.getEnum( "check" ) )
			);
			if ( sqlDeleteAll.verify() != Expectation.class ) {
				collection.setDeleteAllExpectation( getDefaultSupplier( sqlDeleteAll.verify() ) );
			}
		}

		final AnnotationUsage<SQLSelect> sqlSelect = property.getAnnotationUsage( SQLSelect.class );
		if ( sqlSelect != null ) {
			final String loaderName = collection.getRole() + "$SQLSelect";
			collection.setLoaderName( loaderName );
			// TODO: pass in the collection element type here
			QueryBinder.bindNativeQuery( loaderName, sqlSelect, null, buildingContext );
		}

		final AnnotationUsage<HQLSelect> hqlSelect = property.getAnnotationUsage( HQLSelect.class );
		if ( hqlSelect != null ) {
			final String loaderName = collection.getRole() + "$HQLSelect";
			collection.setLoaderName( loaderName );
			QueryBinder.bindQuery( loaderName, hqlSelect, buildingContext );
		}

		final AnnotationUsage<Loader> loader = property.getAnnotationUsage( Loader.class );
		if ( loader != null ) {
			collection.setLoaderName( loader.getString( "namedQuery" ) );
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
			comparatorClass = comparatorSort.getClassDetails( "value" ).toJavaClass();
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
				collection.setOrderBy( sqlOrderBy.getString( "clause" ) );
			}
			if ( sqlOrder != null ) {
				collection.setOrderBy( sqlOrder.getString( "value" ) );
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

	private void handleFetchProfileOverrides() {
		property.forEachAnnotationUsage( FetchProfileOverride.class, (usage) -> {
			buildingContext.getMetadataCollector().addSecondPass( new FetchSecondPass(
					usage,
					propertyHolder,
					propertyName,
					buildingContext
			) );
		} );
	}

	private void handleFetch() {
		final AnnotationUsage<Fetch> fetchAnnotation = property.getAnnotationUsage( Fetch.class );
		if ( fetchAnnotation != null ) {
			// Hibernate @Fetch annotation takes precedence
			setHibernateFetchMode( fetchAnnotation.getEnum( "value" ) );
		}
		else {
			collection.setFetchMode( getFetchMode( getJpaFetchType() ) );
		}
	}

	private void setHibernateFetchMode(org.hibernate.annotations.FetchMode fetchMode) {
		switch ( fetchMode ) {
			case JOIN -> {
				collection.setFetchMode( FetchMode.JOIN );
				collection.setLazy( false );
			}
			case SELECT -> {
				collection.setFetchMode( FetchMode.SELECT );
			}
			case SUBSELECT -> {
				collection.setFetchMode( FetchMode.SELECT );
				collection.setSubselectLoadable( true );
				collection.getOwner().setSubselectLoadableCollections( true );
			}
			default -> {
				throw new AssertionFailure( "unknown fetch type" );
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void handleLazy() {
		final FetchType jpaFetchType = getJpaFetchType();
		final AnnotationUsage<LazyCollection> lazyCollectionAnnotation = property.getAnnotationUsage( LazyCollection.class );
		if ( lazyCollectionAnnotation != null ) {
			final LazyCollectionOption option = lazyCollectionAnnotation.getEnum( "value" );
			boolean eager = option == LazyCollectionOption.FALSE;
			if ( !eager && jpaFetchType == EAGER ) {
				throw new AnnotationException("Collection '" + safeCollectionRole()
						+ "' is marked 'fetch=EAGER' and '@LazyCollection(" + option + ")'");
			}
			collection.setLazy( !eager );
			collection.setExtraLazy( option == LazyCollectionOption.EXTRA );
		}
		else {
			collection.setLazy( jpaFetchType == LAZY );
			collection.setExtraLazy( false );
		}
	}

	private FetchType getJpaFetchType() {
		final AnnotationUsage<OneToMany> oneToMany = property.getAnnotationUsage( OneToMany.class );
		final AnnotationUsage<ManyToMany> manyToMany = property.getAnnotationUsage( ManyToMany.class );
		final AnnotationUsage<ElementCollection> elementCollection = property.getAnnotationUsage( ElementCollection.class );
		final AnnotationUsage<ManyToAny> manyToAny = property.getAnnotationUsage( ManyToAny.class );
		if ( oneToMany != null ) {
			return oneToMany.getEnum( "fetch" );
		}

		if ( manyToMany != null ) {
			return manyToMany.getEnum( "fetch" );
		}

		if ( elementCollection != null ) {
			return elementCollection.getEnum( "fetch" );
		}

		if ( manyToAny != null ) {
			return LAZY;
		}

		throw new AssertionFailure(
				"Define fetch strategy on a property not annotated with @ManyToOne nor @OneToMany nor @CollectionOfElements"
		);
	}

	TypeDetails getElementType() {
		if ( isDefault( targetEntity, buildingContext ) ) {
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
			final String orderByFragment = buildOrderByClauseFromHql( jpaOrderBy.getString( "value" ), associatedClass );
			if ( isNotEmpty( orderByFragment ) ) {
				collection.setOrderBy( orderByFragment );
			}
		}
	}

	private void bindSynchronize() {
		final AnnotationUsage<Synchronize> synchronizeAnnotation = property.getAnnotationUsage( Synchronize.class );
		if ( synchronizeAnnotation != null ) {
			final JdbcEnvironment jdbcEnvironment = buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment();
			for ( String table : synchronizeAnnotation.<String>getList( "value" ) ) {
				String physicalName = synchronizeAnnotation.getBoolean( "logical" )
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
		property.forEachAnnotationUsage( Filter.class, (usage) -> {
			addFilter( hasAssociationTable, usage );
		} );

		property.forEachAnnotationUsage( FilterJoinTable.class, (usage) -> {
			addFilterJoinTable( hasAssociationTable, usage );
		} );
	}

	private void addFilter(boolean hasAssociationTable, AnnotationUsage<Filter> filterAnnotation) {
		final Map<String,String> aliasTableMap = new HashMap<>();
		final Map<String,String> aliasEntityMap = new HashMap<>();
		final List<AnnotationUsage<SqlFragmentAlias>> aliasAnnotations = filterAnnotation.getList( "aliases" );
		for ( AnnotationUsage<SqlFragmentAlias> aliasAnnotation : aliasAnnotations ) {
			final String alias = aliasAnnotation.getString( "alias" );

			final String table = aliasAnnotation.getString( "table" );
			if ( isNotEmpty( table ) ) {
				aliasTableMap.put( alias, table );
			}

			final ClassDetails entityClassDetails = aliasAnnotation.getClassDetails( "entity" );
			if ( entityClassDetails != ClassDetails.VOID_CLASS_DETAILS ) {
				aliasEntityMap.put( alias, entityClassDetails.getName() );
			}
		}

		if ( hasAssociationTable ) {
			collection.addManyToManyFilter(
					filterAnnotation.getString( "name" ),
					getFilterCondition( filterAnnotation ),
					filterAnnotation.getBoolean( "deduceAliasInjectionPoints" ),
					aliasTableMap,
					aliasEntityMap
			);
		}
		else {
			collection.addFilter(
					filterAnnotation.getString( "name" ),
					getFilterCondition( filterAnnotation ),
					filterAnnotation.getBoolean( "deduceAliasInjectionPoints" ),
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
		final AnnotationUsage<SQLJoinTableRestriction> joinTableRestriction = property.getAnnotationUsage( SQLJoinTableRestriction.class );
		if ( joinTableRestriction != null ) {
			return joinTableRestriction.getString( "value" );
		}
		final AnnotationUsage<WhereJoinTable> whereJoinTable = property.getAnnotationUsage( WhereJoinTable.class );
		return whereJoinTable == null ? null : whereJoinTable.getString( "clause" );
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
		final AnnotationUsage<SQLRestriction> restrictionOnCollection = getOverridableAnnotation( property, SQLRestriction.class, getBuildingContext() );
		if ( restrictionOnCollection != null ) {
			return restrictionOnCollection.getString( "value" );
		}

		final AnnotationUsage<Where> whereOnCollection = getOverridableAnnotation( property, Where.class, buildingContext );
		if ( whereOnCollection != null ) {
			return whereOnCollection.getString( "clause" );
		}

		return null;
	}

	private String getWhereOnClassClause() {
		final TypeDetails elementType = property.getElementType();
		if ( elementType != null && useEntityWhereClauseForCollections( buildingContext ) ) {
			final AnnotationUsage<SQLRestriction> restrictionOnClass = getOverridableAnnotation(
					property.getAssociatedType().determineRawClass(),
					SQLRestriction.class,
					buildingContext
			);
			if ( restrictionOnClass != null ) {
				return restrictionOnClass.getString( "value" );
			}
			final AnnotationUsage<Where> whereOnClass = getOverridableAnnotation( property, Where.class, buildingContext );
			if ( whereOnClass != null ) {
				return whereOnClass.getString( "clause" );
			}
			return null;
		}
		else {
			return null;
		}
	}

	private void addFilterJoinTable(boolean hasAssociationTable, AnnotationUsage<FilterJoinTable> filter) {
		if ( hasAssociationTable ) {
			final Map<String,String> aliasTableMap = new HashMap<>();
			final Map<String,String> aliasEntityMap = new HashMap<>();
			final List<AnnotationUsage<SqlFragmentAlias>> aliasAnnotations = filter.getList( "aliases" );
			for ( AnnotationUsage<SqlFragmentAlias> aliasAnnotation : aliasAnnotations ) {
				final String alias = aliasAnnotation.getString( "alias" );

				final String table = aliasAnnotation.getString( "table" );
				if ( isNotEmpty( table ) ) {
					aliasTableMap.put( alias, table );
				}

				final ClassDetails entityClassDetails = aliasAnnotation.getClassDetails( "entity" );
				if ( entityClassDetails != ClassDetails.VOID_CLASS_DETAILS ) {
					aliasEntityMap.put( alias, entityClassDetails.getName() );
				}
			}

			collection.addFilter(
					filter.getString( "name" ),
					getFilterConditionForJoinTable( filter ),
					filter.getBoolean( "deduceAliasInjectionPoints" ),
					aliasTableMap,
					aliasEntityMap
			);
		}
		else {
			throw new AnnotationException( "Collection '" + qualify( propertyHolder.getPath(), propertyName )
					+ "' is an association with no join table and may not have a '@FilterJoinTable'" );
		}
	}

	private String getFilterConditionForJoinTable(AnnotationUsage<FilterJoinTable> filterJoinTableAnnotation) {
		final String condition = filterJoinTableAnnotation.getString( "condition" );
		return condition.isEmpty()
				? getDefaultFilterCondition( filterJoinTableAnnotation.getString( "name" ), filterJoinTableAnnotation )
				: condition;
	}

	private String getFilterCondition(AnnotationUsage<Filter> filter) {
		final String condition = filter.getString( "condition" );
		return condition.isEmpty()
				? getDefaultFilterCondition( filter.getString( "name" ), filter )
				: condition;
	}

	private String getDefaultFilterCondition(String name, AnnotationUsage<? extends Annotation> annotation) {
		final FilterDefinition definition = buildingContext.getMetadataCollector().getFilterDefinition( name );
		if ( definition == null ) {
			throw new AnnotationException( "Collection '" + qualify( propertyHolder.getPath(), propertyName )
					+ "' has a '@" + annotation.getAnnotationType().getSimpleName()
					+ "' for an undefined filter named '" + name + "'" );
		}
		final String defaultCondition = definition.getDefaultFilterCondition();
		if ( isEmpty( defaultCondition ) ) {
			throw new AnnotationException( "Collection '" + qualify( propertyHolder.getPath(), propertyName ) +
					"' has a '@"  + annotation.getAnnotationType().getSimpleName()
					+ "' with no 'condition' and no default condition was given by the '@FilterDef' named '"
					+ name + "'" );
		}
		return defaultCondition;
	}

	public void setCache(AnnotationUsage<Cache> cache) {
		if ( cache != null ) {
			cacheRegionName = nullIfEmpty( cache.getString( "region" ));
			cacheConcurrencyStrategy = EntityBinder.getCacheConcurrencyStrategy( cache.getEnum( "usage" ) );
		}
		else {
			cacheConcurrencyStrategy = null;
			cacheRegionName = null;
		}
	}

	public void setQueryCacheLayout(AnnotationUsage<QueryCacheLayout> queryCacheLayout) {
		this.queryCacheLayout = queryCacheLayout == null ? null : queryCacheLayout.getEnum( "layout" );
	}

	public void setOneToMany(boolean oneToMany) {
		this.oneToMany = oneToMany;
	}

	public void setIndexColumn(IndexColumn indexColumn) {
		this.indexColumn = indexColumn;
	}

	public void setMapKey(AnnotationUsage<MapKey> key) {
		hasMapKeyProperty = key != null;
		if ( hasMapKeyProperty ) {
			mapKeyPropertyName = nullIfEmpty( key.getString( "name" ) );
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
			if ( orderByFragment.length() == 0 || orderByFragment.equalsIgnoreCase( "asc" ) ) {
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
			final AnnotationUsage<org.hibernate.annotations.ForeignKey> fk = property.getAnnotationUsage( org.hibernate.annotations.ForeignKey.class );
			if ( fk != null && !fk.getString( "name" ).isEmpty() ) {
				key.setForeignKeyName( fk.getString( "name" ) );
			}
			else {
				final AnnotationUsage<CollectionTable> collectionTableAnn = property.getAnnotationUsage( CollectionTable.class );
				if ( collectionTableAnn != null ) {
					final AnnotationUsage<Annotation> foreignKey = collectionTableAnn.getNestedUsage( "foreignKey" );
					final ConstraintMode constraintMode = foreignKey.getEnum( "value" );
					if ( constraintMode == NO_CONSTRAINT
							|| constraintMode == PROVIDER_DEFAULT && noConstraintByDefault ) {
						key.disableForeignKey();
					}
					else {
						key.setForeignKeyName( nullIfEmpty( foreignKey.getString( "name" ) ) );
						key.setForeignKeyDefinition( nullIfEmpty( foreignKey.getString( "foreignKeyDefinition" ) ) );
						if ( key.getForeignKeyName() == null
								&& key.getForeignKeyDefinition() == null
								&& collectionTableAnn.getList( "joinColumns" ).size() == 1 ) {
							//noinspection unchecked
							final AnnotationUsage<JoinColumn> joinColumn = (AnnotationUsage<JoinColumn>) collectionTableAnn.getList( "joinColumns" ).get( 0 );
							final AnnotationUsage<Annotation> nestedForeignKey = joinColumn.getNestedUsage( "foreignKey" );
							key.setForeignKeyName( nullIfEmpty( nestedForeignKey.getString( "name" ) ) );
							key.setForeignKeyDefinition( nullIfEmpty( nestedForeignKey.getString( "foreignKeyDefinition" ) ) );
						}
					}
				}
				else {
					final AnnotationUsage<JoinTable> joinTableAnn = property.getAnnotationUsage( JoinTable.class );
					if ( joinTableAnn != null ) {
						final AnnotationUsage<Annotation> foreignKey = joinTableAnn.getNestedUsage( "foreignKey" );
						String foreignKeyName = foreignKey.getString( "name" );
						String foreignKeyDefinition = foreignKey.getString( "foreignKeyDefinition" );
						ConstraintMode foreignKeyValue = foreignKey.getEnum( "value" );
						List<AnnotationUsage<JoinColumn>> joinColumnAnnotations = joinTableAnn.getList( "joinColumns" );
						if ( !joinColumnAnnotations.isEmpty() ) {
							final AnnotationUsage<JoinColumn> joinColumnAnn = joinColumnAnnotations.get( 0 );
							final AnnotationUsage<Annotation> joinColumnForeignKey = joinColumnAnn.getNestedUsage( "foreignKey" );
							if ( foreignKeyName.isEmpty() ) {
								foreignKeyName = joinColumnForeignKey.getString( "name" );
								foreignKeyDefinition = joinColumnForeignKey.getString( "foreignKeyDefinition" );
							}
							if ( foreignKeyValue != NO_CONSTRAINT ) {
								foreignKeyValue = joinColumnForeignKey.getEnum( "value" );
							}
						}
						if ( foreignKeyValue == NO_CONSTRAINT
								|| foreignKeyValue == PROVIDER_DEFAULT && noConstraintByDefault ) {
							key.disableForeignKey();
						}
						else {
							key.setForeignKeyName( nullIfEmpty( foreignKeyName ) );
							key.setForeignKeyDefinition( nullIfEmpty( foreignKeyDefinition ) );
						}
					}
					else {
						final String propertyPath = qualify( propertyHolder.getPath(), property.getName() );
						final AnnotationUsage<ForeignKey> foreignKey = propertyHolder.getOverriddenForeignKey( propertyPath );
						if ( foreignKey != null ) {
							handleForeignKeyConstraint( noConstraintByDefault, key, foreignKey );
						}
						else {
							final AnnotationUsage<OneToMany> oneToManyAnn = property.getAnnotationUsage( OneToMany.class );
							final AnnotationUsage<OnDelete> onDeleteAnn = property.getAnnotationUsage( OnDelete.class );
							if ( oneToManyAnn != null
									&& !oneToManyAnn.getString( "mappedBy" ).isEmpty()
									&& ( onDeleteAnn == null || onDeleteAnn.getEnum( "action" ) != OnDeleteAction.CASCADE ) ) {
								// foreign key should be up to @ManyToOne side
								// @OnDelete generate "on delete cascade" foreign key
								key.disableForeignKey();
							}
							else {
								final AnnotationUsage<JoinColumn> joinColumnAnn = property.getSingleAnnotationUsage( JoinColumn.class );
								if ( joinColumnAnn != null ) {
									handleForeignKeyConstraint( noConstraintByDefault, key, joinColumnAnn.getNestedUsage( "foreignKey" ) );
								}
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
			AnnotationUsage<ForeignKey> foreignKey) {
		final ConstraintMode constraintMode = foreignKey.getEnum( "value" );
		if ( constraintMode == NO_CONSTRAINT
				|| constraintMode == PROVIDER_DEFAULT && noConstraintByDefault) {
			key.disableForeignKey();
		}
		else {
			key.setForeignKeyName( nullIfEmpty( foreignKey.getString( "name" ) ) );
			key.setForeignKeyDefinition( nullIfEmpty( foreignKey.getString( "foreignKeyDefinition" ) ) );
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
		final boolean isManyToAny = property.hasAnnotationUsage( ManyToAny.class );

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
			final ManyToOne element = handleCollectionOfEntities( elementType, targetEntity, hqlOrderBy );
			bindManyToManyInverseForeignKey( targetEntity, inverseJoinColumns, element, oneToMany );
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

		final Class<? extends CompositeUserType<?>> compositeUserType = resolveCompositeUserType( property, elementClass, buildingContext );
		boolean isComposite = classType == EMBEDDABLE || compositeUserType != null;
		holder.prepare( property, isComposite );

		if ( isComposite ) {
			handleCompositeCollectionElement( hqlOrderBy, elementType, elementClass, holder, compositeUserType );
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
			ClassDetails elementClass,
			CollectionPropertyHolder holder,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		//TODO be smart with isNullable
		final AccessType accessType = accessType( property, collection.getOwner() );
		// We create a new entity binder here because it is needed for processing the embeddable
		// Since this is an element collection, there is no real entity binder though,
		// so we just create an "empty shell" for the purpose of avoiding null checks in the fillEmbeddable() method etc.
		final EntityBinder entityBinder = new EntityBinder();
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
		final AnnotationUsage<Access> accessAnn = property.getAnnotationUsage( Access.class );
		if ( accessAnn != null ) {
			// the attribute is locally annotated with `@Access`, use that
			return accessAnn.getEnum( "value" ) == PROPERTY
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
			final boolean attributeOverride = property.hasAnnotationUsage( AttributeOverride.class )
					|| property.hasAnnotationUsage( AttributeOverrides.class );
			// todo : force in the case of Convert annotation(s) with embedded paths (beyond key/value prefixes)?
			return isEmbedded || attributeOverride
					? EMBEDDABLE
					: buildingContext.getMetadataCollector().getClassType( elementClass );
		}
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

		final AnnotationUsage<org.hibernate.annotations.ForeignKey> fk = property.getAnnotationUsage( org.hibernate.annotations.ForeignKey.class );
		if ( fk != null && !fk.getString( "name" ).isEmpty() ) {
			element.setForeignKeyName( fk.getString( "name" ) );
		}
		else {
			final AnnotationUsage<JoinTable> joinTableAnn = property.getAnnotationUsage( JoinTable.class );
			if ( joinTableAnn != null ) {
				final AnnotationUsage<ForeignKey> inverseForeignKey = joinTableAnn.getNestedUsage( "inverseForeignKey" );
				String foreignKeyName = inverseForeignKey.getString( "name" );
				String foreignKeyDefinition = inverseForeignKey.getString( "foreignKeyDefinition" );

				final List<AnnotationUsage<JoinColumn>> inverseJoinColumns = joinTableAnn.getList( "inverseJoinColumns" );
				if ( !inverseJoinColumns.isEmpty() ) {
					final AnnotationUsage<JoinColumn> joinColumnAnn = inverseJoinColumns.get( 0);
					if ( foreignKeyName.isEmpty() ) {
						final AnnotationUsage<ForeignKey> inverseJoinColumnForeignKey = joinColumnAnn.getNestedUsage( "foreignKey" );
						foreignKeyName = inverseJoinColumnForeignKey.getString( "name" );
						foreignKeyDefinition = inverseJoinColumnForeignKey.getString( "foreignKeyDefinition" );
					}
				}

				final ConstraintMode constraintMode = inverseForeignKey.getEnum( "value" );
				if ( constraintMode == NO_CONSTRAINT
						|| constraintMode == PROVIDER_DEFAULT
								&& buildingContext.getBuildingOptions().isNoConstraintByDefault() ) {
					element.disableForeignKey();
				}
				else {
					element.setForeignKeyName( nullIfEmpty( foreignKeyName ) );
					element.setForeignKeyDefinition( nullIfEmpty( foreignKeyDefinition ) );
				}
			}
		}
		return element;
	}

	private void handleManyToAny() {
		//@ManyToAny
		//Make sure that collTyp is never used during the @ManyToAny branch: it will be set to void.class
		final PropertyData inferredData = new PropertyInferredData(
				null,
				property,
				"unsupported",
				buildingContext
		);

		final MemberDetails prop = inferredData.getAttributeMember();
		final AnnotationUsage<jakarta.persistence.Column> discriminatorColumnAnn = prop.getAnnotationUsage( jakarta.persistence.Column.class );
		final AnnotationUsage<Formula> discriminatorFormulaAnn = getOverridableAnnotation( prop, Formula.class, buildingContext );

		//override the table
		inverseJoinColumns.setTable( collection.getCollectionTable() );

		final AnnotationUsage<ManyToAny> anyAnn = property.getAnnotationUsage( ManyToAny.class );
		final Any any = buildAnyValue(
				discriminatorColumnAnn,
				discriminatorFormulaAnn,
				inverseJoinColumns,
				inferredData,
				onDeleteAction,
				anyAnn.getEnum( "fetch" ) == LAZY,
				Nullability.NO_CONSTRAINT,
				propertyHolder,
				new EntityBinder(),
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
				//"collection&&element" is not a valid property name => placeholder
				return new PropertyPreloadedData( AccessType.PROPERTY, "collection&&element", elementClass );
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
				!isCollectionOfEntities && property.hasAnnotationUsage( ElementCollection.class )
		);
		final Table collectionTable = tableBinder.bind();
		collection.setCollectionTable( collectionTable );
		handleCheckConstraints( collectionTable );
		processSoftDeletes();
	}

	private void handleCheckConstraints(Table collectionTable) {
		property.forEachAnnotationUsage( Check.class, (usage) -> {
			addCheckToCollection( collectionTable, usage );
		} );
	}

	private static void addCheckToCollection(Table collectionTable, AnnotationUsage<Check> check) {
		final String name = check.getString( "name" );
		final String constraint = check.getString( "constraints" );
		collectionTable.addCheck( name.isEmpty()
				? new CheckConstraint( constraint )
				: new CheckConstraint( name, constraint ) );
	}

	private void processSoftDeletes() {
		assert collection.getCollectionTable() != null;

		final AnnotationUsage<SoftDelete> softDelete = extractSoftDelete( property, propertyHolder, buildingContext );
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

	private static AnnotationUsage<SoftDelete> extractSoftDelete(
			MemberDetails property,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context) {
		final AnnotationUsage<SoftDelete> fromProperty = property.getAnnotationUsage( SoftDelete.class );
		if ( fromProperty != null ) {
			return fromProperty;
		}

		return extractFromPackage(
				SoftDelete.class,
				property.getDeclaringType(),
				context
		);
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

		if ( property.hasAnnotationUsage( Checks.class )
				|| property.hasAnnotationUsage( Check.class ) ) {
			throw new AnnotationException( "Association '" + safeCollectionRole()
					+ " is an unowned collection and may not be annotated '@Check'" );
		}
	}

	private void detectManyToManyProblems(
			TypeDetails elementType,
			boolean isCollectionOfEntities,
			boolean isManyToAny) {

		if ( !isCollectionOfEntities) {
			if ( property.hasAnnotationUsage( ManyToMany.class ) || property.hasAnnotationUsage( OneToMany.class ) ) {
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
				final AnnotationUsage<JoinTable> joinTableAnn = propertyHolder.getJoinTable( property );
				if ( joinTableAnn != null && !joinTableAnn.getList( "inverseJoinColumns" ).isEmpty() ) {
					throw new AnnotationException( "Association '" + safeCollectionRole()
							+ " has a '@JoinTable' with 'inverseJoinColumns' and"
							+ targetEntityMessage( elementType ) );
				}
			}
		}
	}

	static String targetEntityMessage(TypeDetails elementType) {
		final String problem = elementType.determineRawClass().hasAnnotationUsage( Entity.class )
				? " which does not belong to the same persistence unit"
				: " which is not an '@Entity' type";
		return " targets the type '" + elementType.getName() + "'" + problem;
	}

	private Class<? extends EmbeddableInstantiator> resolveCustomInstantiator(
			MemberDetails property,
			TypeDetails propertyClass,
			MetadataBuildingContext context) {
		final AnnotationUsage<org.hibernate.annotations.EmbeddableInstantiator> propertyAnnotation
				= property.getAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.getClassDetails( "value" ).toJavaClass();
		}

		final ClassDetails rawPropertyClassDetails = propertyClass.determineRawClass();
		final AnnotationUsage<org.hibernate.annotations.EmbeddableInstantiator> classAnnotation
				= rawPropertyClassDetails.getAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.getClassDetails( "value" ).toJavaClass();
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
		final AnnotationUsage<CompositeType> compositeType = property.getAnnotationUsage( CompositeType.class );
		if ( compositeType != null ) {
			return compositeType.getClassDetails( "value" ).toJavaClass();
		}

		if ( returnedClass != null ) {
			final Class<?> embeddableClass = returnedClass.toJavaClass();
			return embeddableClass == null
					? null
					: context.getMetadataCollector().findRegisteredCompositeUserType( embeddableClass );
		}

		return null;
	}

	private String extractHqlOrderBy(AnnotationUsage<OrderBy> jpaOrderBy) {
		if ( jpaOrderBy != null ) {
			// Null not possible. In case of empty expression, apply default ordering.
			return jpaOrderBy.getString( "value" );
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

		if ( property.hasAnnotationUsage( ElementCollection.class ) ) {
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
