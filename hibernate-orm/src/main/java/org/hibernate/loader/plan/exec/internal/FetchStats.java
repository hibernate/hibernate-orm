/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import java.util.Set;

import org.hibernate.loader.plan.spi.CollectionAttributeFetch;

/**
 * Contract used to report collected information about fetches.  For now that is only whether there were
 * subselect fetches found
 *
 * @author Steve Ebersole
 */
public interface FetchStats {
	/**
	 * Were any subselect fetches encountered?
	 *
	 * @return {@code true} if subselect fetches were encountered; {@code false} otherwise.
	 */
	public boolean hasSubselectFetches();

	/**
	 * Returns a set of bag attributes that are join-fetched.
	 *
	 * @return a set of bag attributes that are join-fetched.
	 */
	public Set<CollectionAttributeFetch> getJoinedBagAttributeFetches();
}
