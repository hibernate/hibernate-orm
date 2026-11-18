package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.query.predicate.FilterPredicate;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Applies the restrictions of a to-one association when the owner SQL could not. Checks that
 * an identifier is visible through the association, or loads the visible target by unique key.
 */
public final class ToOneVisibilityLoader {
	private final ToOneAttributeMapping association;
	private volatile @Nullable Plan identifierPlan;
	private volatile @Nullable Plan uniqueKeyPlan;

	public ToOneVisibilityLoader(ToOneAttributeMapping association) {
		this.association = association;
	}

	/** Whether the entity with the given identifier satisfies the association restrictions. */
	public boolean isVisible(Object identifier, JdbcValuesSourceProcessingState processingState) {
		return getPlan( false, processingState ).load( identifier, processingState.getSession() ) != null;
	}

	/**
	 * Loads the entity with the given key which satisfies the association restrictions, if any.
	 * A key repeated among the rows processed together is loaded once.
	 */
	public @Nullable Object loadByUniqueKey(Object key, JdbcValuesSourceProcessingState processingState) {
		final var session = processingState.getSession();
		final var plan = getPlan( true, processingState );
		final var cache = processingState.getToOneVisibilityPlanCache();
		if ( cache == null ) {
			return plan.load( key, session );
		}
		// The persistence context resolves a unique key regardless of the
		// association restrictions and of the current filter arguments.
		final var targets = cache.getTargets( this, plan );
		final var persister = association.getEntityMappingType().getEntityPersister();
		final String propertyName = association.getReferencedPropertyName();
		final var uniqueKey = new EntityUniqueKey( persister.getEntityName(), propertyName, key,
				persister.getPropertyType( propertyName ), session.getFactory() );
		if ( targets.containsKey( uniqueKey ) ) {
			return targets.get( uniqueKey );
		}
		final Object target = plan.load( key, session );
		targets.put( uniqueKey, target );
		return target;
	}

	private Plan getPlan(boolean byUniqueKey, JdbcValuesSourceProcessingState processingState) {
		final var influencers = processingState.getSession().getLoadQueryInfluencers();
		// Loading the entity, unlike selecting its identifier, also depends on whatever affects its fetches.
		if ( association.isAffectedByEnabledFilters( influencers ) || byUniqueKey
				&& association.getEntityMappingType().getEntityPersister().isAffectedByInfluencers( influencers, true ) ) {
			// Captured filter bindings may be reused within this execution, but not across sessions.
			final var cache = processingState.getToOneVisibilityPlanCache();
			return cache == null ? createPlan( byUniqueKey, influencers, false )
					: cache.getPlan( this, byUniqueKey, influencers );
		}
		var plan = byUniqueKey ? uniqueKeyPlan : identifierPlan;
		if ( plan == null ) {
			if ( byUniqueKey ) {
				uniqueKeyPlan = plan = createPlan( true, new LoadQueryInfluencers( influencers.getSessionFactory() ), false );
			}
			else {
				identifierPlan = plan = createPlan( false, influencers, false );
			}
		}
		return plan;
	}

	private Plan createPlan(boolean byUniqueKey, LoadQueryInfluencers influencers, boolean captureFilters) {
		final var target = association.getEntityMappingType();
		final var parameters = JdbcParametersList.newBuilder();
		final SelectStatement select;
		final BiFunction<Object, SharedSessionContractImplementor, Object> loader;
		if ( byUniqueKey ) {
			final var uniqueKey = (SingularAttributeMapping) target.findByPath( association.getReferencedPropertyName() );
			select = LoaderSelectBuilder.createAssociationTargetSelect( association, uniqueKey, influencers, parameters::add );
			final var uniqueKeyLoader = new SingleUniqueKeyEntityLoaderStandard<>( target, uniqueKey, select, parameters.build() );
			loader = (key, session) -> uniqueKeyLoader.load( key, LockOptions.NONE, null, session );
		}
		else {
			// Select only the identifier: an excluded association must not mark an entity as absent.
			final var identifier = target.getIdentifierMapping();
			select = LoaderSelectBuilder.createAssociationKeySelect( association, identifier, influencers, parameters::add );
			loader = new SingleIdLoadPlan<>( target, identifier, select, parameters.build(), LockOptions.NONE,
					influencers.getSessionFactory() )::load;
		}
		return new Plan( loader, captureFilters ? FilterState.capture( select, influencers ) : null,
				captureFilters && byUniqueKey ? FetchState.capture( influencers ) : null );
	}

	/**
	 * Lazily allocated by the result-processing state. Keeps only the latest plan for each
	 * association and key mode, with no growth as filter arguments change during scrolling.
	 */
	public static final class Cache {
		private @Nullable Map<ToOneVisibilityLoader, Plan> identifierPlans;
		private @Nullable Map<ToOneVisibilityLoader, Plan> uniqueKeyPlans;
		private @Nullable Map<ToOneVisibilityLoader, Targets> targets;

		private Map<EntityUniqueKey, Object> getTargets(ToOneVisibilityLoader loader, Plan plan) {
			if ( targets == null ) {
				targets = new IdentityHashMap<>( 2 );
			}
			var loaded = targets.get( loader );
			if ( loaded == null || loaded.plan != plan ) {
				// Another plan reflects changed filters or fetching, which may select other targets.
				loaded = new Targets( plan, new HashMap<>() );
				targets.put( loader, loaded );
			}
			return loaded.byKey;
		}

		/** Forgets the loaded targets, which may be evicted once their rows have been processed. */
		public void finishUp() {
			targets = null;
		}

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

	private record Plan(
			BiFunction<Object, SharedSessionContractImplementor, Object> loader,
			@Nullable FilterState filters, @Nullable FetchState fetches) {
		@Nullable Object load(Object key, SharedSessionContractImplementor session) {
			return loader.apply( key, session );
		}

		boolean matches(LoadQueryInfluencers influencers) {
			return filters != null && filters.matches( influencers )
				&& ( fetches == null || fetches.matches( influencers ) );
		}
	}

	/**
	 * The session settings, other than filters, on which a select loading an entity may depend.
	 * A plan selecting only an identifier does not depend on them.
	 */
	private record FetchState(
			String[] profiles, @Nullable RootGraphImplementor<?> graph, @Nullable GraphSemantic graphSemantic,
			@Nullable CascadingFetchProfile cascadingProfile, int batchSize, boolean batchSizeOverride,
			boolean subselectFetch, @Nullable Object temporalIdentifier) {
		static FetchState capture(LoadQueryInfluencers influencers) {
			final var graph = influencers.getEffectiveEntityGraph();
			return new FetchState( influencers.getEnabledFetchProfileNames().toArray( String[]::new ),
					graph.getGraph(), graph.getSemantic(), influencers.getEnabledCascadingFetchProfile(),
					influencers.getBatchSize(), influencers.hasBatchSizeOverride(),
					influencers.getSubselectFetchEnabled(), influencers.getTemporalIdentifier() );
		}

		boolean matches(LoadQueryInfluencers influencers) {
			final var effectiveGraph = influencers.getEffectiveEntityGraph();
			return sameNames( profiles, influencers.getEnabledFetchProfileNames() )
				&& Objects.equals( graph, effectiveGraph.getGraph() )
				&& graphSemantic == effectiveGraph.getSemantic()
				&& cascadingProfile == influencers.getEnabledCascadingFetchProfile()
				&& batchSize == influencers.getBatchSize()
				&& batchSizeOverride == influencers.hasBatchSizeOverride()
				&& subselectFetch == influencers.getSubselectFetchEnabled()
				&& Objects.equals( temporalIdentifier, influencers.getTemporalIdentifier() );
		}
	}

	private record Targets(Plan plan, Map<EntityUniqueKey, Object> byKey) {
	}

	private static boolean sameNames(String[] snapshot, Set<String> names) {
		if ( snapshot.length != names.size() ) {
			return false;
		}
		for ( String name : snapshot ) {
			if ( !names.contains( name ) ) {
				return false;
			}
		}
		return true;
	}

	private record FilterState(String[] enabledNames, List<Parameter> parameters) {
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
			return new FilterState( influencers.getEnabledFilterNames().toArray( String[]::new ), parameters );
		}

		boolean matches(LoadQueryInfluencers influencers) {
			// Enablement changes may introduce predicates absent from the previous AST,
			// including parameterless filters. Arguments of unused filters do not matter.
			if ( !sameNames( enabledNames, influencers.getEnabledFilterNames() ) ) {
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
