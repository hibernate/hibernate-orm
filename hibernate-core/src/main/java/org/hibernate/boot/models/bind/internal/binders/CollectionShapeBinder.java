/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.FetchMode;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.List;

import jakarta.persistence.FetchType;

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
		applyFetching( source, collection );
		applyFetchProfileOverrides( source, collection, bindingState );
		applyFilters( source, collection, bindingState );
		applyRestrictions( source, collection );
		applyCollectionType( source, collection );
		switch ( source.classification() ) {
			case ORDERED_SET, ORDERED_MAP -> applyOrdering( source, collection, bindingState );
			case SORTED_SET, SORTED_MAP -> applySorting( source, collection );
			default -> {
			}
		}
		if ( collection instanceof List list ) {
			applyListIndexBase( source, list );
		}
	}

	private static void applyFetching(CollectionSource source, Collection collection) {
		final FetchType fetchType = source.fetchType();
		collection.setLazy( fetchType == FetchType.LAZY );
		collection.setExtraLazy( false );
		final var hibernateFetch = source.hibernateFetch();
		if ( hibernateFetch != null ) {
			collection.setFetchMode( hibernateFetch.value().getHibernateFetchMode() );
		}
		else {
			collection.setFetchMode( fetchType == FetchType.EAGER ? FetchMode.JOIN : FetchMode.SELECT );
		}
		if ( source.batchSize() >= 0 ) {
			collection.setBatchSize( source.batchSize() );
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
		final boolean hasAssociationTable = source.joinTable() != null;
		for ( Filter filter : source.filters() ) {
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

	private static void applyRestrictions(CollectionSource source, Collection collection) {
		final boolean hasAssociationTable = source.joinTable() != null;
		final String whereClause = combinedRestriction( source.sqlRestriction(), source.associatedTypeRestriction() );
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

	private static String combinedRestriction(SQLRestriction memberRestriction, SQLRestriction associatedTypeRestriction) {
		if ( memberRestriction == null ) {
			return associatedTypeRestriction == null ? null : associatedTypeRestriction.value();
		}
		if ( associatedTypeRestriction == null ) {
			return memberRestriction.value();
		}
		return "(" + associatedTypeRestriction.value() + ") and (" + memberRestriction.value() + ")";
	}

	private static void applyOrdering(CollectionSource source, Collection collection, BindingState bindingState) {
		final var sqlOrder = source.sqlOrder();
		if ( sqlOrder != null ) {
			collection.setOrderBy( sqlOrder.value() );
			return;
		}

		final var orderBy = source.orderBy();
		if ( orderBy != null ) {
			collection.setOrderBy( orderBy.value() );
		}
	}

	private static void applySorting(CollectionSource source, Collection collection) {
		collection.setSorted( true );

		final var sortComparator = source.sortComparator();
		if ( sortComparator != null ) {
			collection.setComparatorClassName( sortComparator.value().getName() );
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
