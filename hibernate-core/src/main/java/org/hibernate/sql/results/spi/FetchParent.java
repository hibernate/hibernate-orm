/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;

/**
 * Contract for things that can be the parent of a fetch
 *
 * @author Steve Ebersole
 */
public interface FetchParent {
	/**
	 * Access to the NavigableContainer that contains the Navigable being fetched.
	 */
	NavigableContainer getFetchContainer();

	/**
	 * Get the property path to this parent
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	/**
	 * todo (6.0) : better way to handle this rather than allowing mutation here?
	 */
	void addFetch(Fetch fetch);

	/**
	 * Retrieve the fetches owned by this fetch source.
	 * <p/>
	 * This is why generics suck :(  Ideally this would override
	 * FetchSource#getFetches and give a covariant return of a List of
	 * org.hibernate.sql.results.spi.Fetch
	 *
	 * @return The owned fetches.
	 */
	List<Fetch> getFetches();
}
