/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.query.predicate.FilterPredicate;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/** Checks association visibility when the owner SQL could not supply a restricted target key. */
public final class ToOneVisibilityLoader {
	private final ToOneAttributeMapping association;
	private volatile @Nullable Plan identifierPlan;
	private volatile @Nullable Plan uniqueKeyPlan;

	public ToOneVisibilityLoader(ToOneAttributeMapping association) {
		this.association = association;
	}

	public boolean isVisible(Object key, boolean byUniqueKey, JdbcValuesSourceProcessingState processingState) {
		final var session = processingState.getSession();
		final var influencers = session.getLoadQueryInfluencers();
		final Plan plan;
		if ( association.isAffectedByEnabledFilters( influencers ) ) {
			// Captured filter bindings may be reused within this execution, but not across sessions.
			final var cache = processingState.getToOneVisibilityPlanCache();
			plan = cache == null ? createPlan( byUniqueKey, influencers, false )
					: cache.getPlan( this, byUniqueKey, influencers );
		}
		else {
			var cached = byUniqueKey ? uniqueKeyPlan : identifierPlan;
			if ( cached == null ) {
				cached = createPlan( byUniqueKey, influencers, false );
				if ( byUniqueKey ) {
					uniqueKeyPlan = cached;
				}
				else {
					identifierPlan = cached;
				}
			}
			plan = cached;
		}
		// Select only the identifier: an excluded association must not mark an entity as absent.
		return plan.loadPlan.load( key, session ) != null;
	}

	private Plan createPlan(boolean byUniqueKey, LoadQueryInfluencers influencers, boolean captureFilters) {
		final var target = association.getEntityMappingType();
		final ModelPart attribute = byUniqueKey
				? target.findByPath( association.getReferencedPropertyName() ) : target.getIdentifierMapping();
		final ModelPart key = attribute instanceof ToOneAttributeMapping toOne
				? toOne.getForeignKeyDescriptor() : attribute;
		final var parameters = JdbcParametersList.newBuilder();
		final var select = LoaderSelectBuilder.createAssociationKeySelect( association, key, influencers, parameters::add );
		final var filterState = captureFilters ? FilterState.capture( select, influencers ) : null;
		return new Plan( new SingleIdLoadPlan<>( target, key, select, parameters.build(), LockOptions.NONE,
				influencers.getSessionFactory() ), filterState );
	}

	/**
	 * Lazily allocated by the result-processing state. Keeps only the latest plan for each
	 * association and key mode, with no growth as filter arguments change during scrolling.
	 */
	public static final class Cache {
		private @Nullable Map<ToOneVisibilityLoader, Plan> identifierPlans;
		private @Nullable Map<ToOneVisibilityLoader, Plan> uniqueKeyPlans;

		private Plan getPlan(ToOneVisibilityLoader loader, boolean byUniqueKey, LoadQueryInfluencers influencers) {
			var plans = byUniqueKey ? uniqueKeyPlans : identifierPlans;
			if ( plans == null ) {
				plans = new IdentityHashMap<>( 2 );
				if ( byUniqueKey ) {
					uniqueKeyPlans = plans;
				}
				else {
					identifierPlans = plans;
				}
			}
			var plan = plans.get( loader );
			if ( plan == null || !plan.matches( influencers ) ) {
				plan = loader.createPlan( byUniqueKey, influencers, true );
				plans.put( loader, plan );
			}
			return plan;
		}
	}

	private record Plan(SingleIdLoadPlan<Object> loadPlan, @Nullable FilterState filters) {
		boolean matches(LoadQueryInfluencers influencers) {
			return filters != null && filters.matches( influencers );
		}
	}

	private record FilterState(Set<String> enabledNames, List<Parameter> parameters) {
		static FilterState capture(SelectStatement select, LoadQueryInfluencers influencers) {
			final List<Parameter> parameters = new ArrayList<>();
			select.accept( new AbstractSqlAstWalker() {
				@Override
				public void visitFilterPredicate(FilterPredicate predicate) {
					for ( var fragment : predicate.getFragments() ) {
						if ( fragment.getParameters() != null ) {
							for ( var parameter : fragment.getParameters() ) {
								parameters.add( Parameter.capture( parameter ) );
							}
						}
					}
				}
			} );
			return new FilterState( Set.copyOf( influencers.getEnabledFilterNames() ), parameters );
		}

		boolean matches(LoadQueryInfluencers influencers) {
			// Enablement changes may introduce predicates absent from the previous AST,
			// including parameterless filters. Arguments of unused filters do not matter.
			if ( !enabledNames.equals( influencers.getEnabledFilterNames() ) ) {
				return false;
			}
			for ( var parameter : parameters ) {
				final var filter = influencers.getEnabledFilter( parameter.filterName );
				if ( filter == null || !parameter.matches( filter.getParameterValue( parameter.name ) ) ) {
					return false;
				}
			}
			return true;
		}
	}

	private record Parameter(String filterName, String name, JavaType<Object> javaType, Object value, boolean plural) {
		@SuppressWarnings("unchecked")
		static Parameter capture(FilterPredicate.FilterFragmentParameter parameter) {
			final var javaType = (JavaType<Object>) parameter.getValueMapping().getJavaTypeDescriptor();
			final Object value = parameter.getValue();
			final boolean plural = isPlural( value, javaType );
			final Object snapshot;
			if ( plural ) {
				final List<Object> elements = new ArrayList<>();
				for ( Object element : (Iterable<?>) value ) {
					elements.add( javaType.getMutabilityPlan().deepCopy( element ) );
				}
				snapshot = elements;
			}
			else {
				snapshot = javaType.getMutabilityPlan().deepCopy( value );
			}
			return new Parameter( parameter.getFilterName(), parameter.getParameterName(), javaType, snapshot, plural );
		}

		boolean matches(Object current) {
			if ( plural != isPlural( current, javaType ) ) {
				return false;
			}
			if ( !plural ) {
				return javaType.areEqual( value, current );
			}
			final var previous = ((Iterable<?>) value).iterator();
			final var next = ((Iterable<?>) current).iterator();
			while ( previous.hasNext() && next.hasNext() ) {
				if ( !javaType.areEqual( previous.next(), next.next() ) ) {
					return false;
				}
			}
			return !previous.hasNext() && !next.hasNext();
		}

		private static boolean isPlural(Object value, JavaType<?> javaType) {
			return value instanceof Iterable<?> && !javaType.isInstance( value );
		}
	}
}
