/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.profile;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.FetchStyle.JOIN;
import static org.hibernate.engine.FetchStyle.SUBSELECT;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

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

	private final String name;
	private final Map<String,Fetch> fetches = new HashMap<>();

	private boolean containsJoinFetchedCollection;
	private boolean containsJoinFetchedBag;
	private @Nullable Fetch bagJoinFetch;

	/**
	 * Constructs a {@link FetchProfile} with the given unique name.
	 * Fetch profile names must be unique within a given {@code SessionFactory}.
	 *
	 * @param name The name under which we are bound in the sessionFactory
	 */
	public FetchProfile(String name) {
		this.name = name;
	}

	/**
	 * Add a {@linkplain Fetch fetch override} to the profile.
	 *
	 * @param fetch The fetch override to add.
	 */
	@Internal
	public void addFetch(final Fetch fetch) {
		final Association association = fetch.getAssociation();
		final String role = association.getRole();
		final Type associationType =
				association.getOwner().getPropertyType( association.getAssociationPath() );
		if ( associationType instanceof CollectionType ) {
			CORE_LOGGER.tracev( "Handling request to add collection fetch [{0}]", role );

			// couple of things for which to account in the case of collection
			// join fetches
			if ( fetch.getMethod() == JOIN ) {
				// first, if this is a bag we need to ignore it if we previously
				// processed collection join fetches
				if ( associationType instanceof BagType ) {
					if ( containsJoinFetchedCollection ) {
						CORE_LOGGER.containsJoinFetchedCollection( role );
						// EARLY EXIT!!!
						return;
					}
				}

				// also, in cases where we are asked to add a collection join
				// fetch where we had already added a bag join fetch previously,
				// we need to go back and ignore that previous bag join fetch.
				if ( containsJoinFetchedBag ) {
					// just for safety...
					if ( bagJoinFetch != null && fetches.remove( bagJoinFetch.getAssociation().getRole() ) != bagJoinFetch ) {
						CORE_LOGGER.unableToRemoveBagJoinFetch();
					}
					bagJoinFetch = null;
					containsJoinFetchedBag = false;
				}

				containsJoinFetchedCollection = true;
			}
		}
		fetches.put( role, fetch );
	}

	/**
	 * The name of this fetch profile
	 */
	public String getName() {
		return name;
	}

	/**
	 * A map of {@link Fetch} instances, keyed by association role
	 */
	public Map<String,Fetch> getFetches() {
		return fetches;
	}

	/**
	 * Obtain the {@linkplain Fetch fetch override} associated with
	 * the given role.
	 *
	 * @param role The role name identifying the association
	 *
	 * @return The {@code Fetch}, or {@code null} if there was
	 *         no {@code Fetch} for the given association
	 */
	public @Nullable Fetch getFetchByRole(String role) {
		return fetches.get( role );
	}

	@Override
	public String toString() {
		return "FetchProfile[" + name + "]";
	}

	public boolean hasSubselectLoadableCollectionsEnabled(EntityPersister persister) {
		for ( Fetch fetch : getFetches().values() ) {
			if ( fetch.getMethod() == SUBSELECT
					&& fetch.getAssociation().getOwner() == persister ) {
				return true;
			}
		}
		return false;
	}
}
