/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.model.internal.QueryBinder;
import org.hibernate.boot.model.internal.SoftDeleteHelper;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.engine.FetchStyle;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.CustomSqlMapping;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SortableValue;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import static org.hibernate.boot.models.internal.DialectOverrideAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/// Applies collection-shape metadata after the concrete collection mapping exists.
///
/// `CollectionSource` decides the semantic classification of a plural member.
/// This helper applies the metadata that is orthogonal to table/element/key
/// binding, such as ordered and sorted collection settings.
///
/// @since 9.0
/// @author Steve Ebersole
class CollectionShapeBinder {
	static void apply(CollectionSource source, Collection collection, BindingState bindingState) {
		validateOrderingAndSorting( source, collection, bindingState );
		applyFetching( source, collection );
		applyFetchProfileOverrides( source, collection, bindingState );
		applyFilters( source, collection, bindingState );
		applyRestrictions( source, collection, bindingState );
		applyCustomSql( source, collection, bindingState );
		applyCaching( source, collection );
		applyQueryCacheLayout( source, collection );
		applyCustomLoader( source, collection, bindingState );
		applyCollectionType( source, collection );
		applySoftDelete( source, collection, bindingState );
		StateManagementBindingPhase.registerCollection( source, collection, bindingState );
		switch ( source.classification() ) {
			case ORDERED_SET, ORDERED_MAP -> applyOrdering( source, collection, bindingState );
			case SORTED_SET, SORTED_MAP -> applySorting( source, collection );
			default -> {
				if ( hasOrdering( source, bindingState ) ) {
					applyOrdering( source, collection, bindingState );
				}
			}
		}
		if ( collection instanceof List list ) {
			applyListIndexBase( source, list );
		}
	}

	private static void validateOrderingAndSorting(CollectionSource source, Collection collection, BindingState bindingState) {
		if ( hasOrdering( source, bindingState ) && hasSorting( source ) ) {
			throw new AnnotationException( "Collection '" + collection.getRole() + "' is both sorted and ordered" );
		}
	}

	private static boolean hasOrdering(CollectionSource source, BindingState bindingState) {
		return source.orderBy() != null || effectiveSqlOrder( source, bindingState ) != null;
	}

	private static boolean hasSorting(CollectionSource source) {
		return source.member().hasDirectAnnotationUsage( org.hibernate.annotations.SortNatural.class )
				|| source.sortComparator() != null;
	}

	private static void applySoftDelete(CollectionSource source, Collection collection, BindingState bindingState) {
		if ( source.nature() == CollectionSource.Nature.ONE_TO_MANY
				&& source.member().hasDirectAnnotationUsage( SoftDelete.class ) ) {
			throw new UnsupportedMappingException(
					"@SoftDelete cannot be applied to @OneToMany - "
							+ source.member().getDeclaringType().getName() + "." + source.member().getName()
			);
		}

		final SoftDelete softDelete = SoftDeleteBindingHelper.resolveCollectionSoftDelete( source, bindingState );
		if ( softDelete != null && collection.getCollectionTable() != null ) {
			SoftDeleteHelper.bindSoftDeleteIndicator(
					softDelete,
					collection,
					collection.getCollectionTable(),
					bindingState.getMetadataBuildingContext()
			);
		}
	}

	private static void applyFetching(CollectionSource source, Collection collection) {
		final FetchType fetchType = source.fetchType();
		collection.setLazy( fetchType == FetchType.LAZY );
		collection.setExtraLazy( false );
		final var hibernateFetchMode = source.hibernateFetchMode();
		if ( hibernateFetchMode != null ) {
			applyHibernateFetchStyle( hibernateFetchMode, collection );
		}
		else {
			collection.setFetchStyle( fetchType == FetchType.EAGER ? FetchStyle.JOIN : FetchStyle.SELECT );
		}
		if ( source.batchSize() >= 0 ) {
			collection.setBatchSize( source.batchSize() );
		}
	}

	private static void applyHibernateFetchStyle(
			org.hibernate.annotations.FetchMode fetchMode,
			Collection collection) {
		switch ( fetchMode ) {
			case JOIN -> {
				collection.setFetchStyle( FetchStyle.JOIN );
				collection.setLazy( false );
			}
			case SELECT -> collection.setFetchStyle( FetchStyle.SELECT );
			case SUBSELECT -> {
				collection.setFetchStyle( FetchStyle.SELECT );
				collection.setSubselectLoadable( true );
				collection.getOwner().setSubselectLoadableCollections( true );
				collection.getOwner().setSubselectLoadableAttributes( true );
			}
		}
	}

	private static void applyFetchProfileOverrides(
			CollectionSource source,
			Collection collection,
			BindingState bindingState) {
		for ( FetchProfileOverride override : source.fetchProfileOverrides() ) {
			final FetchProfile profile = bindingState.getFetchProfile( override.profile() );
			if ( profile == null ) {
				throw new AnnotationException( "Collection '" + collection.getRole()
						+ "' refers to an unknown fetch profile named '" + override.profile() + "'" );
			}
			profile.addFetch( new FetchProfile.Fetch(
					collection.getOwnerEntityName(),
					collection.getRole().substring( collection.getOwnerEntityName().length() + 1 ),
					override.mode(),
					override.fetch()
			) );
		}
	}

	private static void applyFilters(CollectionSource source, Collection collection, BindingState bindingState) {
		final boolean hasAssociationTable = hasAssociationTable( source );
		for ( Filter filter : effectiveFilters( source, bindingState ) ) {
			final String condition = resolveFilterCondition( collection, filter.name(), filter.condition(), bindingState );
			if ( hasAssociationTable ) {
				collection.addManyToManyFilter(
						filter.name(),
						condition,
						filter.deduceAliasInjectionPoints(),
						extractFilterAliasTableMap( filter.aliases() ),
						extractFilterAliasEntityMap( filter.aliases() )
				);
			}
			else {
				collection.addFilter(
						filter.name(),
						condition,
						filter.deduceAliasInjectionPoints(),
						extractFilterAliasTableMap( filter.aliases() ),
						extractFilterAliasEntityMap( filter.aliases() )
				);
			}
		}

		for ( FilterJoinTable filter : source.filterJoinTables() ) {
			if ( !hasAssociationTable ) {
				throw new AnnotationException( "Collection '" + collection.getRole()
						+ "' is an association with no join table and may not have a '@FilterJoinTable'" );
			}
			collection.addFilter(
					filter.name(),
					resolveFilterCondition( collection, filter.name(), filter.condition(), bindingState ),
					filter.deduceAliasInjectionPoints(),
					extractFilterAliasTableMap( filter.aliases() ),
					extractFilterAliasEntityMap( filter.aliases() )
			);
		}
	}

	private static boolean hasAssociationTable(CollectionSource source) {
		return switch ( source.nature() ) {
			case MANY_TO_MANY, MANY_TO_ANY -> true;
			case ONE_TO_MANY -> source.joinTable() != null
					|| isEmpty( source.oneToMany().mappedBy() )
							&& source.oneToManyJoinColumnsOrFormulas().isEmpty();
			case ELEMENT_COLLECTION -> false;
		};
	}

	private static String resolveFilterCondition(
			Collection collection,
			String filterName,
			String condition,
			BindingState bindingState) {
		if ( !condition.isBlank() ) {
			return condition;
		}

		final var filterDefinition = bindingState.getFilterDefinition( filterName );
		if ( filterDefinition == null ) {
			throw new AnnotationException( "Collection '" + collection.getRole()
					+ "' has a '@Filter' for an undefined filter named '" + filterName + "'" );
		}

		final String defaultCondition = filterDefinition.getDefaultFilterCondition();
		if ( defaultCondition == null || defaultCondition.isBlank() ) {
			throw new AnnotationException( "Collection '" + collection.getRole()
					+ "' has a '@Filter' with no 'condition' and no default condition was given by the '@FilterDef' named '"
					+ filterName + "'" );
		}
		return defaultCondition;
	}

	private static Map<String, String> extractFilterAliasTableMap(SqlFragmentAlias[] aliases) {
		if ( aliases.length == 0 ) {
			return null;
		}
		final Map<String,String> result = new HashMap<>();
		for ( SqlFragmentAlias alias : aliases ) {
			if ( !alias.table().isBlank() ) {
				result.put( alias.alias(), alias.table() );
			}
		}
		return result.isEmpty() ? null : result;
	}

	private static Map<String, String> extractFilterAliasEntityMap(SqlFragmentAlias[] aliases) {
		if ( aliases.length == 0 ) {
			return null;
		}
		final Map<String,String> result = new HashMap<>();
		for ( SqlFragmentAlias alias : aliases ) {
			if ( alias.entity() != void.class ) {
				result.put( alias.alias(), alias.entity().getName() );
			}
		}
		return result.isEmpty() ? null : result;
	}

	private static Filter[] effectiveFilters(CollectionSource source, BindingState bindingState) {
		final Filters filters = getOverridableAnnotation(
				source.member(),
				Filters.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
		return filters == null ? source.filters() : filters.value();
	}

	private static void applyRestrictions(CollectionSource source, Collection collection, BindingState bindingState) {
		final boolean hasAssociationTable = hasAssociationTable( source );
		final String whereClause = combinedRestriction(
				getOverridableAnnotation(
						source.member(),
						SQLRestriction.class,
						bindingState.getDatabase().getDialect(),
						bindingState.getMetadataBuildingContext().getModelsContext()
				),
				effectiveAssociatedTypeRestriction( source, bindingState )
		);
		if ( whereClause != null ) {
			if ( hasAssociationTable ) {
				collection.setManyToManyWhere( whereClause );
			}
			else {
				collection.setWhere( whereClause );
			}
		}

		final var joinTableRestriction = source.sqlJoinTableRestriction();
		if ( joinTableRestriction != null ) {
			if ( !hasAssociationTable ) {
				throw new AnnotationException( "Collection '" + collection.getRole()
						+ "' is an association with no join table and may not have a '@SQLJoinTableRestriction'" );
			}
			collection.setWhere( joinTableRestriction.value() );
		}
	}

	private static void applyCustomSql(CollectionSource source, Collection collection, BindingState bindingState) {
		final var sqlInsert = getOverridableAnnotation(
				source.member(),
				SQLInsert.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
		if ( sqlInsert != null ) {
			collection.setCustomSqlInsert( customSqlMapping( sqlInsert.sql(), sqlInsert.callable(), sqlInsert.verify() ) );
		}

		final var sqlUpdate = getOverridableAnnotation(
				source.member(),
				SQLUpdate.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
		if ( sqlUpdate != null ) {
			collection.setCustomSqlUpdate( customSqlMapping( sqlUpdate.sql(), sqlUpdate.callable(), sqlUpdate.verify() ) );
		}

		final var sqlDelete = getOverridableAnnotation(
				source.member(),
				SQLDelete.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
		if ( sqlDelete != null ) {
			collection.setCustomSqlDelete( customSqlMapping( sqlDelete.sql(), sqlDelete.callable(), sqlDelete.verify() ) );
		}

		final var sqlDeleteAll = getOverridableAnnotation(
				source.member(),
				SQLDeleteAll.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
		if ( sqlDeleteAll != null ) {
			collection.setCustomSqlDeleteAll( customSqlMapping(
					sqlDeleteAll.sql(),
					sqlDeleteAll.callable(),
					sqlDeleteAll.verify()
			) );
		}
	}

	private static CustomSqlMapping customSqlMapping(
			String sql,
			boolean callable,
			Class<? extends org.hibernate.jdbc.Expectation> expectationClass) {
		return CustomSqlMapping.customSqlMapping( sql, callable, expectationClass, false );
	}

	private static void applyQueryCacheLayout(CollectionSource source, Collection collection) {
		final QueryCacheLayout queryCacheLayout = source.member().getDirectAnnotationUsage( QueryCacheLayout.class );
		if ( queryCacheLayout != null ) {
			collection.setQueryCacheLayout( queryCacheLayout.layout() );
		}
	}

	private static void applyCaching(CollectionSource source, Collection collection) {
		final Cache cache = source.member().getDirectAnnotationUsage( Cache.class );
		if ( cache != null ) {
			final var accessType = cache.usage().toAccessType();
			if ( accessType != null ) {
				collection.setCacheConcurrencyStrategy( accessType.getExternalName() );
				collection.setCacheRegionName( nullIfEmpty( cache.region() ) );
			}
		}
	}

	private static void applyCustomLoader(CollectionSource source, Collection collection, BindingState bindingState) {
		final SQLSelect sqlSelect = getOverridableAnnotation(
				source.member(),
				SQLSelect.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
		if ( sqlSelect != null ) {
			final String loaderName = collection.getRole() + "$SQLSelect";
			collection.setLoaderName( loaderName );
			QueryBinder.bindNativeQuery(
					loaderName,
					sqlSelect,
					null,
					bindingState.getMetadataBuildingContext()
			);
		}

		final HQLSelect hqlSelect = source.member().getDirectAnnotationUsage( HQLSelect.class );
		if ( hqlSelect != null ) {
			final String loaderName = collection.getRole() + "$HQLSelect";
			collection.setLoaderName( loaderName );
			QueryBinder.bindQuery( loaderName, hqlSelect, bindingState.getMetadataBuildingContext() );
		}
	}

	private static String combinedRestriction(SQLRestriction memberRestriction, SQLRestriction associatedTypeRestriction) {
		if ( memberRestriction == null ) {
			return associatedTypeRestriction == null ? null : associatedTypeRestriction.value();
		}
		if ( associatedTypeRestriction == null ) {
			return memberRestriction.value();
		}
		return "(" + associatedTypeRestriction.value() + ") and (" + memberRestriction.value() + ")";
	}

	private static SQLRestriction effectiveAssociatedTypeRestriction(CollectionSource source, BindingState bindingState) {
		if ( source.nature() == CollectionSource.Nature.ELEMENT_COLLECTION || source.elementType() == null ) {
			return null;
		}
		return getOverridableAnnotation(
				source.elementType().determineRawClass(),
				SQLRestriction.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
	}

	private static void applyOrdering(CollectionSource source, Collection collection, BindingState bindingState) {
		final SQLOrder sqlOrder = effectiveSqlOrder( source, bindingState );
		if ( sqlOrder != null ) {
			if ( isNotBlank( sqlOrder.value() ) ) {
				collection.setOrderBy( sqlOrder.value() );
			}
			return;
		}

		final var orderBy = source.orderBy();
		if ( orderBy != null ) {
			final String orderByFragment = orderByFragment( source, orderBy.value(), bindingState );
			if ( source.nature() == CollectionSource.Nature.MANY_TO_MANY ) {
				collection.setJpaOrderBy( orderByFragment );
				collection.setManyToManyJpaOrdering( orderByFragment );
			}
			else {
				collection.setJpaOrderBy( orderByFragment );
			}
		}
	}

	private static SQLOrder effectiveSqlOrder(CollectionSource source, BindingState bindingState) {
		return getOverridableAnnotation(
				source.member(),
				SQLOrder.class,
				bindingState.getDatabase().getDialect(),
				bindingState.getMetadataBuildingContext().getModelsContext()
		);
	}

	private static String orderByFragment(CollectionSource source, String orderByFragment, BindingState bindingState) {
		if ( source.nature() == CollectionSource.Nature.ELEMENT_COLLECTION ) {
			final String trimmed = orderByFragment.trim();
			if ( trimmed.isBlank() || trimmed.equalsIgnoreCase( "asc" ) ) {
				return "$element$ asc";
			}
			if ( trimmed.equalsIgnoreCase( "desc" ) ) {
				return "$element$ desc";
			}
		}
		else if ( source.elementType() != null && source.nature() != CollectionSource.Nature.MANY_TO_MANY ) {
			final String trimmed = orderByFragment.trim();
			if ( trimmed.isBlank() || trimmed.equalsIgnoreCase( "asc" ) ) {
				return entityIdentifierOrderByFragment( source, bindingState, " asc" );
			}
			if ( trimmed.equalsIgnoreCase( "desc" ) ) {
				return entityIdentifierOrderByFragment( source, bindingState, " desc" );
			}
		}
		return orderByFragment;
	}

	private static String entityIdentifierOrderByFragment(
			CollectionSource source,
			BindingState bindingState,
			String direction) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				targetClassDetails( source )
		);
		if ( targetTypeBinder == null ) {
			return "";
		}
		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			return "";
		}
		if ( identifierBinding.value() instanceof SortableValue sortableValue ) {
			sortableValue.sortProperties( bindingState::getEntityBinding );
		}
		final StringBuilder result = new StringBuilder();
		for ( Selectable selectable : identifierBinding.value().getSelectables() ) {
			if ( !result.isEmpty() ) {
				result.append( ", " );
			}
			result.append( selectable.getText() );
			result.append( direction );
		}
		return result.toString();
	}

	private static ClassDetails targetClassDetails(CollectionSource source) {
		final ManyToMany manyToMany = source.manyToMany();
		if ( manyToMany != null && manyToMany.targetEntity() != void.class ) {
			return source.modelsContext().getClassDetailsRegistry().resolveClassDetails( manyToMany.targetEntity().getName() );
		}
		final OneToMany oneToMany = source.oneToMany();
		if ( oneToMany != null && oneToMany.targetEntity() != void.class ) {
			return source.modelsContext().getClassDetailsRegistry().resolveClassDetails( oneToMany.targetEntity().getName() );
		}
		return source.elementType().determineRawClass();
	}

	private static void applySorting(CollectionSource source, Collection collection) {
		collection.setSorted( true );

		final var sortComparator = source.sortComparator();
		if ( sortComparator != null ) {
			collection.setComparatorClassDetails(
					source.modelsContext().getClassDetailsRegistry()
							.resolveClassDetails( sortComparator.value().getName() )
			);
		}
	}

	private static void applyListIndexBase(CollectionSource source, List collection) {
		final var listIndexBase = source.listIndexBase();
		if ( listIndexBase != null ) {
			collection.setBaseIndex( listIndexBase.value() );
		}
	}

	private static void applyCollectionType(CollectionSource source, Collection collection) {
		final var collectionType = source.collectionType();
		if ( collectionType == null ) {
			return;
		}

		collection.setTypeName( collectionType.type().getName() );
		collection.setTypeParameters( extractParameterMap( collectionType.parameters() ) );
	}

	private static Map<String, String> extractParameterMap(Parameter[] parameters) {
		final Map<String, String> result = new HashMap<>();
		for ( Parameter parameter : parameters ) {
			result.put( parameter.name(), parameter.value() );
		}
		return result;
	}
}
