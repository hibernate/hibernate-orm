/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.profile;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.BagType;

/**
 * A 'fetch profile' allows a user to dynamically modify the fetching strategy used for particular associations at
 * runtime, whereas that information was historically only statically defined in the metadata.
 * <p/>
 * This class defines the runtime representation of this data.
 *
 * @author Steve Ebersole
 */
public class FetchProfile {
	private static final Logger log = LoggerFactory.getLogger( FetchProfile.class );

	private final String name;
	private Map<String,Fetch> fetches = new HashMap<String,Fetch>();

	private boolean containsJoinFetchedCollection = false;
	private boolean containsJoinFetchedBag = false;
	private Fetch bagJoinFetch;

	/**
	 * A 'fetch profile' is uniquely named within a
	 * {@link SessionFactoryImplementor SessionFactory}, thus it is also
	 * uniquely and easily identifiable within that
	 * {@link SessionFactoryImplementor SessionFactory}.
	 *
	 * @param name The name under which we are bound in the sessionFactory
	 */
	public FetchProfile(String name) {
		this.name = name;
	}

	/**
	 * Add a fetch to the profile.
	 *
	 * @param association The association to be fetched
	 * @param fetchStyleName The name of the fetch style to apply
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public void addFetch(Association association, String fetchStyleName) {
		addFetch( association, Fetch.Style.parse( fetchStyleName ) );
	}

	/**
	 * Add a fetch to the profile.
	 *
	 * @param association The association to be fetched
	 * @param style The style to apply
	 */
	public void addFetch(Association association, Fetch.Style style) {
		addFetch( new Fetch( association, style ) );
	}

	/**
	 * Add a fetch to the profile.
	 *
	 * @param fetch The fetch to add.
	 */
	public void addFetch(Fetch fetch) {
		Type associationType = fetch.getAssociation().getOwner().getPropertyType( fetch.getAssociation().getAssociationPath() );
		if ( associationType.isCollectionType() ) {
			log.trace( "handling request to add collection fetch [{}]", fetch.getAssociation().getRole() );

			// couple of things for which to account in the case of collection
			// join fetches
			if ( Fetch.Style.JOIN == fetch.getStyle() ) {
				// first, if this is a bag we need to ignore it if we previously
				// processed collection join fetches
				if ( BagType.class.isInstance( associationType ) ) {
					if ( containsJoinFetchedCollection ) {
						log.warn( "Ignoring bag join fetch [{}] due to prior collection join fetch", fetch.getAssociation().getRole() );
						return; // EARLY EXIT!!!
					}
				}

				// also, in cases where we are asked to add a collection join
				// fetch where we had already added a bag join fetch previously,
				// we need to go back and ignore that previous bag join fetch.
				if ( containsJoinFetchedBag ) {
					if ( fetches.remove( bagJoinFetch.getAssociation().getRole() ) != bagJoinFetch ) {
						// just for safety...
						log.warn( "Unable to erase previously added bag join fetch" );
					}
					bagJoinFetch = null;
					containsJoinFetchedBag = false;
				}

				containsJoinFetchedCollection = true;
			}
		}
		fetches.put( fetch.getAssociation().getRole(), fetch );
	}

	/**
	 * Getter for property 'name'.
	 *
	 * @return Value for property 'name'.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Getter for property 'fetches'.  Map of {@link Fetch} instances, keyed by association <tt>role</tt>
	 *
	 * @return Value for property 'fetches'.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public Map<String,Fetch> getFetches() {
		return fetches;
	}

	public Fetch getFetchByRole(String role) {
		return fetches.get( role );
	}

	/**
	 * Getter for property 'containsJoinFetchedCollection', which flags whether
	 * this fetch profile contained any collection join fetches.
	 *
	 * @return Value for property 'containsJoinFetchedCollection'.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public boolean isContainsJoinFetchedCollection() {
		return containsJoinFetchedCollection;
	}

	/**
	 * Getter for property 'containsJoinFetchedBag', which flags whether this
	 * fetch profile contained any bag join fetches
	 *
	 * @return Value for property 'containsJoinFetchedBag'.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public boolean isContainsJoinFetchedBag() {
		return containsJoinFetchedBag;
	}
}
