/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.io.Serializable;

import org.hibernate.Filter;
import org.hibernate.UnknownProfileException;
import org.hibernate.type.Type;
import org.hibernate.impl.FilterImpl;

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
	public static LoadQueryInfluencers NONE = new LoadQueryInfluencers();

	private final SessionFactoryImplementor sessionFactory;
	private String internalFetchProfile;
	private Map enabledFilters;
	private Set enabledFetchProfileNames;

	public LoadQueryInfluencers() {
		this( null, java.util.Collections.EMPTY_MAP, java.util.Collections.EMPTY_SET );
	}

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, new HashMap(), new HashSet() );
	}

	private LoadQueryInfluencers(SessionFactoryImplementor sessionFactory, Map enabledFilters, Set enabledFetchProfileNames) {
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
		return enabledFilters != null && !enabledFilters.isEmpty();
	}

	public Map getEnabledFilters() {
		// First, validate all the enabled filters...
		//TODO: this implementation has bad performance
		Iterator itr = enabledFilters.values().iterator();
		while ( itr.hasNext() ) {
			final Filter filter = ( Filter ) itr.next();
			filter.validate();
		}
		return enabledFilters;
	}

	public Filter getEnabledFilter(String filterName) {
		return ( Filter ) enabledFilters.get( filterName );
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
		String[] parsed = parseFilterParameterName( filterParameterName );
		FilterImpl filter = ( FilterImpl ) enabledFilters.get( parsed[0] );
		if ( filter == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
		}
		return filter.getParameter( parsed[1] );
	}

	public Type getFilterParameterType(String filterParameterName) {
		String[] parsed = parseFilterParameterName( filterParameterName );
		FilterDefinition filterDef = sessionFactory.getFilterDefinition( parsed[0] );
		if ( filterDef == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] not defined" );
		}
		Type type = filterDef.getParameterType( parsed[1] );
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
		String filterName = filterParameterName.substring( 0, dot );
		String parameterName = filterParameterName.substring( dot + 1 );
		return new String[] { filterName, parameterName };
	}


	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFetchProfiles() {
		return enabledFetchProfileNames != null && !enabledFetchProfileNames.isEmpty();
	}

	public Set getEnabledFetchProfileNames() {
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

}
