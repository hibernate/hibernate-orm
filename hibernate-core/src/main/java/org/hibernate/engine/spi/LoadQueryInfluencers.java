/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityGraph;

import org.hibernate.Filter;
import org.hibernate.UnknownProfileException;
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
	private final Map<String,Filter> enabledFilters;
	private final Set<String> enabledFetchProfileNames;
	private EntityGraph fetchGraph;
	private EntityGraph loadGraph;

	public LoadQueryInfluencers() {
		this( null );
	}

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, new HashMap<String,Filter>(), new HashSet<String>() );
	}

	private LoadQueryInfluencers(SessionFactoryImplementor sessionFactory, Map<String,Filter> enabledFilters, Set<String> enabledFetchProfileNames) {
		this.sessionFactory = sessionFactory;
		this.enabledFilters = enabledFilters;
		this.enabledFetchProfileNames = enabledFetchProfileNames;
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
		return !enabledFilters.isEmpty();
	}

	public Map<String,Filter> getEnabledFilters() {
		// First, validate all the enabled filters...
		//TODO: this implementation has bad performance
		for ( Filter filter : enabledFilters.values() ) {
			filter.validate();
		}
		return enabledFilters;
	}

	/**
	 * Returns an unmodifiable Set of enabled filter names.
	 * @return an unmodifiable Set of enabled filter names.
	 */
	public Set<String> getEnabledFilterNames() {
		return java.util.Collections.unmodifiableSet( enabledFilters.keySet() );
	}

	public Filter getEnabledFilter(String filterName) {
		return enabledFilters.get( filterName );
	}

	public Filter enableFilter(String filterName) {
		FilterImpl filter = new FilterImpl( sessionFactory.getFilterDefinition( filterName ) );
		enabledFilters.put( filterName, filter );
		return filter;
	}

	public void disableFilter(String filterName) {
		enabledFilters.remove( filterName );
	}

	public Object getFilterParameterValue(String filterParameterName) {
		final String[] parsed = parseFilterParameterName( filterParameterName );
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
		int dot = filterParameterName.indexOf( '.' );
		if ( dot <= 0 ) {
			throw new IllegalArgumentException( "Invalid filter-parameter name format" );
		}
		final String filterName = filterParameterName.substring( 0, dot );
		final String parameterName = filterParameterName.substring( dot + 1 );
		return new String[] { filterName, parameterName };
	}


	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFetchProfiles() {
		return !enabledFetchProfileNames.isEmpty();
	}

	public Set<String> getEnabledFetchProfileNames() {
		return enabledFetchProfileNames;
	}

	private void checkFetchProfileName(String name) {
		if ( !sessionFactory.containsFetchProfileDefinition( name ) ) {
			throw new UnknownProfileException( name );
		}
	}

	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		return enabledFetchProfileNames.contains( name );
	}

	public void enableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		enabledFetchProfileNames.add( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		enabledFetchProfileNames.remove( name );
	}

	public EntityGraph getFetchGraph() {
		return fetchGraph;
	}

	public void setFetchGraph(final EntityGraph fetchGraph) {
		this.fetchGraph = fetchGraph;
	}

	public EntityGraph getLoadGraph() {
		return loadGraph;
	}

	public void setLoadGraph(final EntityGraph loadGraph) {
		this.loadGraph = loadGraph;
	}
}
