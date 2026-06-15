/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import jakarta.persistence.FetchType;
import org.hibernate.FetchMethod;
import org.hibernate.Filter;
import org.hibernate.Internal;
import org.hibernate.SessionCreationOption;
import org.hibernate.UnknownProfileException;
import org.hibernate.audit.AuditLog;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.FilterImpl;
import org.hibernate.engine.creation.internal.SessionCreationOptions;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import jakarta.annotation.Nullable;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.engine.FetchStyle.SUBSELECT;
import static org.hibernate.graph.spi.GraphHelper.appliesTo;

/**
 * Centralize all options which can influence the SQL query needed to load an
 * entity.  Currently, such influencers are defined as:<ul>
 * <li>filters</li>
 * <li>fetch profiles</li>
 * <li>internal fetch profile (merge profile, etc)</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class LoadQueryInfluencers implements Serializable {

	private final SessionFactoryImplementor sessionFactory;

	private @Nullable CascadingFetchProfile enabledCascadingFetchProfile;

	//Lazily initialized!
	private @Nullable HashSet<String> enabledFetchProfileNames;

	//Lazily initialized!
	//Note that ordering is important for cache keys
	private @Nullable TreeMap<String,Filter> enabledFilters;

	private boolean subselectFetchEnabled;

	private int batchSize;
	private FetchOptions fetchOptions = FetchOptions.NONE;

	private final EffectiveEntityGraph effectiveEntityGraph;

	private @Nullable Boolean readOnly;
	private @Nullable Object temporalIdentifier;

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		batchSize = sessionFactory.getSessionFactoryOptions().getDefaultBatchFetchSize();
		subselectFetchEnabled = sessionFactory.getSessionFactoryOptions().isSubselectFetchEnabled();
		effectiveEntityGraph = new EffectiveEntityGraph();
	}

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory, SessionCreationOptions options) {
		this.sessionFactory = sessionFactory;
		batchSize = options.getDefaultBatchFetchSize();
		subselectFetchEnabled = options.isSubselectFetchEnabled();
		effectiveEntityGraph = new EffectiveEntityGraph();
		temporalIdentifier = options.getTemporalIdentifier();
		for ( var filterDefinition : sessionFactory.getAutoEnabledFilters() ) {
			final var filter = new FilterImpl( filterDefinition );
			if ( enabledFilters == null ) {
				enabledFilters = new TreeMap<>();
			}
			enabledFilters.put( filterDefinition.getFilterName(), filter );
		}
		for ( var enabledFilterOption : options.getEnabledFilterOptions() ) {
			enableFilter( enabledFilterOption );
		}
	}

	public EffectiveEntityGraph applyEntityGraph(RootGraphImplementor<?> rootGraph, GraphSemantic graphSemantic) {
		final var effectiveEntityGraph = getEffectiveEntityGraph();
		if ( graphSemantic != null ) {
			if ( rootGraph == null ) {
				throw new IllegalArgumentException( "Graph semantic specified, but no RootGraph was supplied" );
			}
			effectiveEntityGraph.applyGraph( rootGraph, graphSemantic );
		}
		return effectiveEntityGraph;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public @Nullable Object getTemporalIdentifier() {
		return temporalIdentifier;
	}

	public void setTemporalIdentifier(Object temporalIdentifier) {
		this.temporalIdentifier = temporalIdentifier;
	}

	public boolean isAllRevisions() {
		return temporalIdentifier == AuditLog.ALL_CHANGESETS;
	}


	// internal fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public <T> T fromInternalFetchProfile(CascadingFetchProfile profile, Supplier<T> supplier) {
		final var previous = enabledCascadingFetchProfile;
		enabledCascadingFetchProfile = profile;
		try {
			return supplier.get();
		}
		finally {
			enabledCascadingFetchProfile = previous;
		}
	}

	/**
	 * Fetch-profile to apply, if one, when building the result-graph
	 * for cascade fetching - for example, the result-graph used when
	 * handling a {@linkplain org.hibernate.Session#merge merge} to
	 * immediately load additional based on {@linkplain jakarta.persistence.CascadeType#MERGE}
	 */
	public  @Nullable CascadingFetchProfile getEnabledCascadingFetchProfile() {
		return enabledCascadingFetchProfile;
	}

	public boolean hasEnabledCascadingFetchProfile() {
		return enabledCascadingFetchProfile != null;
	}

	/**
	 * Set the effective {@linkplain #getEnabledCascadingFetchProfile() cascading fetch-profile}
	 */
	public void setEnabledCascadingFetchProfile(CascadingFetchProfile enabledCascadingFetchProfile) {
		this.enabledCascadingFetchProfile = enabledCascadingFetchProfile;
	}


	// filter support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFilters() {
		return enabledFilters != null && !enabledFilters.isEmpty();
	}

	public Map<String,Filter> getEnabledFilters() {
		final var enabledFilters = this.enabledFilters;
		if ( enabledFilters == null ) {
			return emptyMap();
		}
		else {
			// First, validate all the enabled filters...
			for ( var filter : enabledFilters.values() ) {
				//TODO: this implementation has bad performance
				filter.validate();
			}
			return enabledFilters;
		}
	}

	/**
	 * Returns an unmodifiable Set of enabled filter names.
	 * @return an unmodifiable Set of enabled filter names.
	 */
	public Set<String> getEnabledFilterNames() {
		return enabledFilters == null ? emptySet() : unmodifiableSet( enabledFilters.keySet() );
	}

	public @Nullable Filter getEnabledFilter(@Nonnull String filterName) {
		return enabledFilters == null ? null : enabledFilters.get( filterName );
	}

	public Filter enableFilter(SessionCreationOption.EnabledFilter enabledFilterOption) {
		final var filter = enableFilter( enabledFilterOption.name() );
		for ( var entry : enabledFilterOption.arguments().entrySet() ) {
			setFilterParameter( filter, entry.getKey(), entry.getValue() );
		}
		return filter;
	}

	private static void setFilterParameter(Filter filter, String parameterName, Object argument) {
		if ( argument instanceof Collection<?> argumentList ) {
			filter.setParameterList( parameterName, argumentList );
		}
		else if ( argument instanceof Object[] argumentArray ) {
			filter.setParameterList( parameterName, argumentArray );
		}
		else {
			filter.setParameter( parameterName, argument );
		}
	}

	public Filter enableFilter(@Nonnull String filterName) {
		final var filter = new FilterImpl( sessionFactory.getFilterDefinition( filterName ) );
		if ( enabledFilters == null ) {
			enabledFilters = new TreeMap<>();
		}
		enabledFilters.put( filterName, filter );
		return filter;
	}

	public void disableFilter(@Nonnull String filterName) {
		if ( enabledFilters != null ) {
			enabledFilters.remove( filterName );
		}
	}

//	public Object getFilterParameterValue(String filterParameterName) {
//		final String[] parsed = parseFilterParameterName( filterParameterName );
//		if ( enabledFilters == null ) {
//			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
//		}
//		final var filter = (FilterImpl) enabledFilters.get( parsed[0] );
//		if ( filter == null ) {
//			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
//		}
//		return filter.getParameter( parsed[1] );
//	}
//
//	public static String[] parseFilterParameterName(String filterParameterName) {
//		final int dot = filterParameterName.lastIndexOf( '.' );
//		if ( dot <= 0 ) {
//			throw new IllegalArgumentException(
//					"Invalid filter-parameter name format [" + filterParameterName + "]; expecting {filter-name}.{param-name}"
//			);
//		}
//		final String filterName = filterParameterName.substring( 0, dot );
//		final String parameterName = filterParameterName.substring( dot + 1 );
//		return new String[] { filterName, parameterName };
//	}


	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFetchProfiles() {
		return enabledFetchProfileNames != null && !enabledFetchProfileNames.isEmpty();
	}

	public Set<String> getEnabledFetchProfileNames() {
		return enabledFetchProfileNames == null ? emptySet() : enabledFetchProfileNames;
	}

	private void checkFetchProfileName(String name) {
		if ( !sessionFactory.containsFetchProfileDefinition( name ) ) {
			throw new UnknownProfileException( name );
		}
	}

	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		return enabledFetchProfileNames != null && enabledFetchProfileNames.contains( name );
	}

	public void enableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		if ( enabledFetchProfileNames == null ) {
			enabledFetchProfileNames = new HashSet<>();
		}
		enabledFetchProfileNames.add( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		if ( enabledFetchProfileNames != null ) {
			enabledFetchProfileNames.remove( name );
		}
	}

	@Internal
	public @Nullable HashSet<String> adjustFetchProfiles(
			@Nullable Set<String> disabledFetchProfiles, @Nullable Set<String> enabledFetchProfiles) {
		final var oldFetchProfiles =
				enabledFetchProfileNames != null && !enabledFetchProfileNames.isEmpty()
						? new HashSet<>( enabledFetchProfileNames )
						: null;
		if ( disabledFetchProfiles != null && enabledFetchProfileNames != null ) {
			enabledFetchProfileNames.removeAll( disabledFetchProfiles );
		}
		if ( enabledFetchProfiles != null ) {
			if ( enabledFetchProfileNames == null ) {
				enabledFetchProfileNames = new HashSet<>();
			}
			enabledFetchProfileNames.addAll( enabledFetchProfiles );
		}
		return oldFetchProfiles;
	}

	@Internal
	public void setEnabledFetchProfileNames(HashSet<String> enabledFetchProfileNames) {
		this.enabledFetchProfileNames = enabledFetchProfileNames;
	}

	public EffectiveEntityGraph getEffectiveEntityGraph() {
		return effectiveEntityGraph;
	}

	public @Nullable Boolean getReadOnly() {
		return readOnly;
	}

	public void setReadOnly(@Nullable Boolean readOnly) {
		this.readOnly = readOnly;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean hasBatchSizeOverride() {
		return fetchOptions.hasBatchSize();
	}

	public <T> T withFetchOptions(SharedSessionContractImplementor session, FetchOptions options, Supplier<T> supplier) {
		if ( !options.hasOptions() && !fetchOptions.hasOptions() ) {
			return supplier.get();
		}
		else {
			final var previousOptions = fetchOptions;
			fetchOptions = options;
			try {
				if ( fetchOptions.hasCacheModes() ) {
					final var previousCacheMode = session.getCacheMode();
					final var effectiveCacheMode = fetchOptions.overrideCacheMode( previousCacheMode );
					final boolean overrideCacheMode = effectiveCacheMode != previousCacheMode;
					if ( overrideCacheMode ) {
						session.setCacheMode( effectiveCacheMode );
					}
					try {
						return supplier.get();
					}
					finally {
						if ( overrideCacheMode ) {
							session.setCacheMode( previousCacheMode );
						}
					}
				}
				else {
					return supplier.get();
				}
			}
			finally {
				this.fetchOptions = previousOptions;
			}
		}
	}

	public int effectiveBatchSize(CollectionPersister persister) {
		final var batchSizeOverride = fetchOptions.batchSize();
		if ( batchSizeOverride != null ) {
			return batchSizeOverride;
		}
		else {
			final int persisterBatchSize = persister.getBatchSize();
			// persister-specific batch size overrides global setting
			// (note that due to legacy, -1 means no explicit setting)
			return persisterBatchSize >= 0 ? persisterBatchSize : batchSize;
		}
	}

	public boolean effectivelyBatchLoadable(CollectionPersister persister) {
		final var batchSizeOverride = fetchOptions.batchSize();
		return batchSizeOverride == null
				? persister.isBatchLoadable() || effectiveBatchSize( persister ) > 1
				: batchSizeOverride > 1;
	}

	public int effectiveBatchSize(EntityPersister persister) {
		final var batchSizeOverride = fetchOptions.batchSize();
		if ( batchSizeOverride != null ) {
			return batchSizeOverride;
		}
		else {
			final int persisterBatchSize = persister.getBatchSize();
			// persister-specific batch size overrides global setting
			// (note that due to legacy, -1 means no explicit setting)
			return persisterBatchSize >= 0 ? persisterBatchSize : batchSize;
		}
	}

	public boolean effectivelyBatchLoadable(EntityPersister persister) {
		final var batchSizeOverride = fetchOptions.batchSize();
		return batchSizeOverride == null
				? persister.isBatchLoadable() || effectiveBatchSize( persister ) > 1
				: batchSizeOverride > 1;
	}

	public boolean getSubselectFetchEnabled() {
		return subselectFetchEnabled;
	}

	public void setSubselectFetchEnabled(boolean subselectFetchEnabled) {
		this.subselectFetchEnabled = subselectFetchEnabled;
	}

	public boolean effectiveSubselectFetchEnabled(CollectionPersister persister) {
		return subselectFetchEnabled
			|| fetchOptions.fetchMethod() == FetchMethod.BY_SUBQUERY
			|| persister.isSubselectLoadable()
			|| isSubselectFetchEnabledInProfile( persister );
	}

	private boolean isSubselectFetchEnabledInProfile(CollectionPersister persister) {
		if ( hasEnabledFetchProfiles() ) {
			final var sqlTranslationEngine = persister.getFactory().getSqlTranslationEngine();
			for ( String profile : getEnabledFetchProfileNames() ) {
				final var fetchProfile = sqlTranslationEngine.getFetchProfile( profile )	;
				if ( fetchProfile != null ) {
					final var fetch = fetchProfile.getFetchByRole( persister.getRole() );
					if ( fetch != null && fetch.getMethod() == SUBSELECT) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean hasSubselectLoadableAttributes(EntityPersister persister) {
		return canUseAttributeFetchOptions( effectiveEntityGraph ) && persister.hasSubselectLoadableAttributes()
			|| subselectFetchEnabled && persister.hasCollections()
			|| hasSubselectLoadableCollectionsEnabledInProfile( persister )
			|| hasSubselectLoadableCollectionsEnabledInGraph( effectiveEntityGraph, persister );
	}

	public boolean hasSubselectLoadableAttributes(
			EntityPersister persister,
			@Nullable AppliedGraph appliedGraph) {
		return canUseAttributeFetchOptions( appliedGraph ) && hasSubselectLoadableAttributes( persister )
			|| hasSubselectLoadableCollectionsEnabledInGraph( appliedGraph, persister );
	}

	private static boolean canUseAttributeFetchOptions(@Nullable AppliedGraph appliedGraph) {
		return appliedGraph == null || appliedGraph.getSemantic() != GraphSemantic.FETCH;
	}

	private boolean hasSubselectLoadableCollectionsEnabledInProfile(EntityPersister persister) {
		if ( hasEnabledFetchProfiles() ) {
			final var sqlTranslationEngine = persister.getFactory().getSqlTranslationEngine();
			for ( String profile : getEnabledFetchProfileNames() ) {
				if ( sqlTranslationEngine.getFetchProfile( profile )
						.hasSubselectLoadableCollectionsEnabled( persister ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasSubselectLoadableCollectionsEnabledInGraph(
			@Nullable AppliedGraph appliedGraph,
			EntityPersister persister) {
		final var graph = appliedGraph == null ? null : appliedGraph.getGraph();
		if ( graph == null || appliedGraph.getSemantic() == null ) {
			return false;
		}

		var entityType = sessionFactory.getJpaMetamodel().findEntityType( persister.getEntityName() );
		if ( entityType == null ) {
			entityType = sessionFactory.getJpaMetamodel().findEntityType( persister.getMappedClass() );
		}
		return entityType != null && hasSubselectLoadableCollectionsEnabledInGraph( graph, entityType );
	}

	private static boolean hasSubselectLoadableCollectionsEnabledInGraph(
			GraphImplementor<?> graph,
			EntityDomainType<?> entityType) {
		if ( appliesTo( graph, entityType )
				&& hasDirectBulkSelectNode( graph ) ) {
			return true;
		}

		for ( var subgraph : graph.getTreatedSubgraphs().values() ) {
			if ( hasSubselectLoadableCollectionsEnabledInGraph( subgraph, entityType ) ) {
				return true;
			}
		}
		for ( var node : graph.getNodes().values() ) {
			for ( var subgraph : node.getSubGraphs().values() ) {
				if ( hasSubselectLoadableCollectionsEnabledInGraph( subgraph, entityType ) ) {
					return true;
				}
			}
			for ( var subgraph : node.getKeySubGraphs().values() ) {
				if ( hasSubselectLoadableCollectionsEnabledInGraph( subgraph, entityType ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasDirectBulkSelectNode(GraphImplementor<?> graph) {
		for ( var node : graph.getNodes().values() ) {
			if ( node.getFetchType() != FetchType.LAZY
					&& node.getOptions().contains( FetchMethod.BY_SUBQUERY ) ) {
				return true;
			}
		}
		return false;
	}

	public void withAppliedGraph(GraphSemantic semantic, RootGraphImplementor<?> graph, Runnable action) {
		effectiveEntityGraph.withAppliedGraph( semantic, graph, action );
	}

	public <T> T fromAppliedGraph(GraphSemantic semantic, RootGraphImplementor<?> graph, Supplier<T> action) {
		return effectiveEntityGraph.fromAppliedGraph( semantic, graph, action );
	}
}
