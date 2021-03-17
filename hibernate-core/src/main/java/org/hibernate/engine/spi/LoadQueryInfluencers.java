/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityGraph;

import org.hibernate.Filter;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.FilterImpl;
import org.hibernate.type.Type;

/**
 * Centralize all options which can influence the SQL query needed to load an
 * entity.  Currently such influencers are defined as:<ul>
 * <li>filters</li>
 * <li>fetch profiles</li>
 * <li>internal fetch profile (merge profile, etc)</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class LoadQueryInfluencers implements Serializable {
	/**
	 * Static reference useful for cases where we are creating load SQL
	 * outside the context of any influencers.  One such example is
	 * anything created by the session factory.
	 */
	public static final LoadQueryInfluencers NONE = new LoadQueryInfluencers();

	private final SessionFactoryImplementor sessionFactory;

	private String internalFetchProfile;

	//Lazily initialized!
	private HashSet<String> enabledFetchProfileNames;

	//Lazily initialized!
	private HashMap<String,Filter> enabledFilters;

	private final EffectiveEntityGraph effectiveEntityGraph = new EffectiveEntityGraph();

	private Boolean readOnly;

	public LoadQueryInfluencers() {
		this( null, null );
	}

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory) {
		this(sessionFactory, null);
	}

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory, Boolean readOnly) {
		this.sessionFactory = sessionFactory;
		this.readOnly = readOnly;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}


	// internal fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public String getInternalFetchProfile() {
		return internalFetchProfile;
	}

	public void setInternalFetchProfile(String internalFetchProfile) {
		if ( sessionFactory == null ) {
			// thats the signal that this is the immutable, context-less
			// variety
			throw new IllegalStateException( "Cannot modify context-less LoadQueryInfluencers" );
		}
		this.internalFetchProfile = internalFetchProfile;
	}


	// filter support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFilters() {
		return enabledFilters != null && !enabledFilters.isEmpty();
	}

	public Map<String,Filter> getEnabledFilters() {
		if ( enabledFilters == null ) {
			return Collections.EMPTY_MAP;
		}
		else {
			// First, validate all the enabled filters...
			for ( Filter filter : enabledFilters.values() ) {
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
		if ( enabledFilters == null ) {
			return Collections.EMPTY_SET;
		}
		else {
			return java.util.Collections.unmodifiableSet( enabledFilters.keySet() );
		}
	}

	public Filter getEnabledFilter(String filterName) {
		if ( enabledFilters == null ) {
			return null;
		}
		else {
			return enabledFilters.get( filterName );
		}
	}

	public Filter enableFilter(String filterName) {
		FilterImpl filter = new FilterImpl( sessionFactory.getFilterDefinition( filterName ) );
		if ( enabledFilters == null ) {
			this.enabledFilters = new HashMap<>();
		}
		enabledFilters.put( filterName, filter );
		return filter;
	}

	public void disableFilter(String filterName) {
		if ( enabledFilters != null ) {
			enabledFilters.remove( filterName );
		}
	}

	public Object getFilterParameterValue(String filterParameterName) {
		final String[] parsed = parseFilterParameterName( filterParameterName );
		if ( enabledFilters == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
		}
		final FilterImpl filter = (FilterImpl) enabledFilters.get( parsed[0] );
		if ( filter == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
		}
		return filter.getParameter( parsed[1] );
	}

	public Type getFilterParameterType(String filterParameterName) {
		final String[] parsed = parseFilterParameterName( filterParameterName );
		final FilterDefinition filterDef = sessionFactory.getFilterDefinition( parsed[0] );
		if ( filterDef == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] not defined" );
		}
		final Type type = filterDef.getParameterType( parsed[1] );
		if ( type == null ) {
			// this is an internal error of some sort...
			throw new InternalError( "Unable to locate type for filter parameter" );
		}
		return type;
	}

	public static String[] parseFilterParameterName(String filterParameterName) {
		int dot = filterParameterName.lastIndexOf( '.' );
		if ( dot <= 0 ) {
			throw new IllegalArgumentException(
					"Invalid filter-parameter name format [" + filterParameterName + "]; expecting {filter-name}.{param-name}"
			);
		}
		final String filterName = filterParameterName.substring( 0, dot );
		final String parameterName = filterParameterName.substring( dot + 1 );
		return new String[] { filterName, parameterName };
	}


	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFetchProfiles() {
		return enabledFetchProfileNames != null && !enabledFetchProfileNames.isEmpty();
	}

	public Set<String> getEnabledFetchProfileNames() {
		if ( enabledFetchProfileNames == null ) {
			return Collections.EMPTY_SET;
		}
		else {
			return enabledFetchProfileNames;
		}
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
			this.enabledFetchProfileNames = new HashSet<>();
		}
		enabledFetchProfileNames.add( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		if ( enabledFetchProfileNames != null ) {
			enabledFetchProfileNames.remove( name );
		}
	}

	public EffectiveEntityGraph getEffectiveEntityGraph() {
		return effectiveEntityGraph;
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	public EntityGraph getFetchGraph() {
		if ( effectiveEntityGraph.getSemantic() != GraphSemantic.FETCH ) {
			return null;
		}

		return effectiveEntityGraph.getGraph();
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	public void setFetchGraph(EntityGraph fetchGraph) {
		effectiveEntityGraph.applyGraph( (RootGraphImplementor<?>) fetchGraph, GraphSemantic.FETCH );
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	public EntityGraph getLoadGraph() {
		if ( effectiveEntityGraph.getSemantic() != GraphSemantic.LOAD ) {
			return null;
		}

		return effectiveEntityGraph.getGraph();
	}

	/**
	 * @deprecated (since 5.4) {@link #getFetchGraph}, {@link #getLoadGraph}, {@link #setFetchGraph}
	 * and {@link #setLoadGraph} (as well as JPA itself honestly) all make it very unclear that
	 * there can be only one graph applied at any one time and that that graph is *either* a load or
	 * a fetch graph.  These have all been replaced with {@link #getEffectiveEntityGraph()}.
	 *
	 * @see EffectiveEntityGraph
	 */
	@Deprecated
	public void setLoadGraph(final EntityGraph loadGraph) {
		effectiveEntityGraph.applyGraph( (RootGraphImplementor<?>) loadGraph, GraphSemantic.LOAD );
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
	}
}
