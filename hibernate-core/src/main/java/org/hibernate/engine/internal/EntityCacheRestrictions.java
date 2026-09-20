/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;

import static org.hibernate.binder.internal.TenantIdBinder.FILTER_NAME;

/** Immutable cache eligibility information shared by all sessions of a factory. */
public final class EntityCacheRestrictions {
	private final boolean hasSqlRestrictions;
	private final String[] filterNames;

	private EntityCacheRestrictions(boolean hasSqlRestrictions, String[] filterNames) {
		this.hasSqlRestrictions = hasSqlRestrictions;
		this.filterNames = filterNames;
	}

	public boolean hasSqlRestrictions() {
		return hasSqlRestrictions;
	}

	public static final EntityCacheRestrictions NONE = new EntityCacheRestrictions( false, new String[0] );
	private static final EntityCacheRestrictions SQL_RESTRICTED = new EntityCacheRestrictions( true, new String[0] );

	public static EntityCacheRestrictions create(EntityMappingType entity, MetadataImplementor bootModel) {
		if ( hasSqlRestrictions( entity, new HashSet<>() ) ) {
			return SQL_RESTRICTED;
		}
		else {
			final Set<String> names = new HashSet<>();
			collectEntityFilters( entity, bootModel, names, true );
			// Do not mark the root visited yet: a self-association must still check its tenant.
			collectAttributeFilters( entity, bootModel, names, new HashSet<>() );
			return names.isEmpty()
					? NONE
					: new EntityCacheRestrictions( false,
							names.toArray( String[]::new ) );
		}
	}

	public boolean isAffectedByFilters(LoadQueryInfluencers influencers) {
		for ( String name : filterNames ) {
			if ( influencers.getEnabledFilter( name ) != null ) {
				return true;
			}
		}
		return false;
	}

	private static void collectEntityFilters(
			EntityMappingType entity, MetadataImplementor bootModel, Set<String> names, boolean root) {
		final var factory = entity.getEntityPersister().getFactory();
		for ( var filter : bootModel.getEntityBinding( entity.getEntityName() ).getFilters() ) {
			final String name = filter.getName();
			if ( !(root && FILTER_NAME.equals( name ))
					&& factory.getFilterDefinition( name ).isAppliedToLoadByKey() ) {
				names.add( name );
			}
		}
	}

	private static void collectFilters(
			ManagedMappingType mapping, MetadataImplementor bootModel, Set<String> names, Set<ManagedMappingType> visited) {
		if ( visited.add( mapping ) ) {
			if ( mapping instanceof EntityMappingType entity ) {
				collectEntityFilters( entity, bootModel, names, false );
			}
			collectAttributeFilters( mapping, bootModel, names, visited );
		}
	}

	private static void collectAttributeFilters(
			ManagedMappingType mapping, MetadataImplementor bootModel, Set<String> names, Set<ManagedMappingType> visited) {
		for ( int i = 0; i < mapping.getNumberOfAttributeMappings(); i++ ) {
			final var attribute = mapping.getAttributeMapping( i );
			if ( attribute instanceof ToOneAttributeMapping toOne ) {
				collectFilters( toOne.getEntityMappingType(), bootModel, names, visited );
			}
			else if ( attribute instanceof EmbeddableValuedModelPart embedded ) {
				collectFilters( embedded.getEmbeddableTypeDescriptor(), bootModel, names, visited );
			}
			else if ( attribute instanceof PluralAttributeMapping plural ) {
				final var fetchOptions = attribute.getMappedFetchOptions();
				if ( fetchOptions.getTiming() == FetchTiming.IMMEDIATE
					&& fetchOptions.getStyle() == FetchStyle.JOIN ) {
					final var collection =
							bootModel.getCollectionBinding( plural.getCollectionDescriptor().getRole() );
					collection.getFilters()
							.forEach( filter -> names.add( filter.getName() ) );
					collection.getManyToManyFilters()
							.forEach( filter -> names.add( filter.getName() ) );
					collectCollectionPartFilters( plural.getElementDescriptor(), bootModel, names, visited );
					collectCollectionPartFilters( plural.getIndexDescriptor(), bootModel, names, visited );
				}
			}
		}
	}

	private static void collectCollectionPartFilters(
			Object part, MetadataImplementor bootModel, Set<String> names, Set<ManagedMappingType> visited) {
		if ( part instanceof EntityCollectionPart entity ) {
			collectFilters( entity.getEntityMappingType(), bootModel, names, visited );
		}
		else if ( part instanceof EmbeddedCollectionPart embedded ) {
			collectFilters( embedded.getEmbeddableTypeDescriptor(), bootModel, names, visited );
		}
	}

	public static boolean hasSqlRestrictions(ManagedMappingType mapping, Set<ManagedMappingType> visited) {
		if ( !visited.add( mapping ) ) {
			return false;
		}
		else if ( mapping instanceof EntityMappingType entity
					&& entity.hasWhereRestrictions() ) {
			return true;
		}
		else {
			for ( int i = 0; i < mapping.getNumberOfAttributeMappings(); i++ ) {
				final var attribute = mapping.getAttributeMapping( i );
				if ( attribute instanceof ToOneAttributeMapping toOne
						&& hasSqlRestrictions( toOne.getEntityMappingType(), visited )
					|| attribute instanceof EmbeddableValuedModelPart embedded
						&& hasSqlRestrictions( embedded.getEmbeddableTypeDescriptor(), visited ) ) {
					return true;
				}
			}
			return false;
		}
	}
}
