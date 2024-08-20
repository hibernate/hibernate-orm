/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.profile;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * The runtime representation of a Hibernate
 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}
 * defined in annotations.
 * <p>
 * Fetch profiles compete with JPA-defined
 * {@linkplain jakarta.persistence.NamedEntityGraph named entity graphs}.
 * The semantics of these two facilities are not identical, however,
 * since a fetch profile is a list, not a graph, and is not by nature
 * rooted at any one particular entity. Instead, given a root entity as
 * input, an active fetch profile contributes to the determination of
 * the fetch graph.
 * <p>
 * A named fetch profile may be enabled in a given session
 * by calling {@link org.hibernate.Session#enableFetchProfile(String)}.
 *
 * @see org.hibernate.mapping.FetchProfile
 *
 * @author Steve Ebersole
 */
public class FetchProfile {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( FetchProfile.class );

	private final String name;
	private final Map<String,Fetch> fetches = new HashMap<>();

	private boolean containsJoinFetchedCollection;
	private boolean containsJoinFetchedBag;
	private Fetch bagJoinFetch;

	/**
	 * Constructs a FetchProfile, supplying its unique name (unique within the SessionFactory).
	 *
	 * @param name The name under which we are bound in the sessionFactory
	 */
	public FetchProfile(String name) {
		this.name = name;
	}

	/**
	 * Add a fetch override to the profile.
	 *
	 * @param association The association to be fetched
	 * @param fetchStyleName The name of the fetch style to apply
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public void addFetch(Association association, String fetchStyleName) {
		addFetch( association, Fetch.Style.parse( fetchStyleName ) );
	}

	/**
	 * Add a fetch override to the profile.
	 *
	 * @param association The association to be fetched
	 * @param style The style to apply
	 */
	public void addFetch(Association association, Fetch.Style style) {
		addFetch( new Fetch( association, style ) );
	}

	/**
	 * Add a fetch override to the profile.
	 *
	 * @param fetch The fetch to add.
	 */
	public void addFetch(final Fetch fetch) {
		final String fetchAssociactionRole = fetch.getAssociation().getRole();
		final Type associationType = fetch.getAssociation().getOwner().getPropertyType( fetch.getAssociation().getAssociationPath() );
		if ( associationType instanceof CollectionType ) {
			LOG.tracev( "Handling request to add collection fetch [{0}]", fetchAssociactionRole );

			// couple of things for which to account in the case of collection
			// join fetches
			if ( Fetch.Style.JOIN == fetch.getStyle() ) {
				// first, if this is a bag we need to ignore it if we previously
				// processed collection join fetches
				if ( associationType instanceof BagType ) {
					if ( containsJoinFetchedCollection ) {
						LOG.containsJoinFetchedCollection( fetchAssociactionRole );
						// EARLY EXIT!!!
						return;
					}
				}

				// also, in cases where we are asked to add a collection join
				// fetch where we had already added a bag join fetch previously,
				// we need to go back and ignore that previous bag join fetch.
				if ( containsJoinFetchedBag ) {
					// just for safety...
					if ( fetches.remove( bagJoinFetch.getAssociation().getRole() ) != bagJoinFetch ) {
						LOG.unableToRemoveBagJoinFetch();
					}
					bagJoinFetch = null;
					containsJoinFetchedBag = false;
				}

				containsJoinFetchedCollection = true;
			}
		}
		fetches.put( fetchAssociactionRole, fetch );
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
	 * Getter for property 'fetches'.  Map of {@link Fetch} instances, keyed by association {@code role}
	 *
	 * @return Value for property 'fetches'.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public Map<String,Fetch> getFetches() {
		return fetches;
	}

	/**
	 * Obtain the fetch associated with the given role.
	 *
	 * @param role The role identifying the fetch
	 *
	 * @return The fetch, or {@code null} if a matching one was not found
	 */
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
