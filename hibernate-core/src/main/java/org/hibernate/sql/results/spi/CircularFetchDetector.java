/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;


import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.BiDirectionalFetchImpl;
import org.hibernate.sql.results.internal.domain.RootBiDirectionalFetchImpl;

/**
 * Maintains state while processing a Fetch graph to be able to detect
 * and handle circular bi-directional references
 *
 * @author Steve Ebersole
 */
public class CircularFetchDetector {

	public Fetch findBiDirectionalFetch(FetchParent fetchParent, Fetchable fetchable) {
		if ( ! fetchable.isCircular( fetchParent ) ) {
			return null;
		}

		assert fetchParent instanceof Fetch;
		final Fetch fetchParentAsFetch = (Fetch) fetchParent;

		final NavigablePath parentParentPath = fetchParent.getNavigablePath().getParent();
		assert fetchParent.getNavigablePath().getParent() != null;

		assert fetchParentAsFetch.getFetchParent().getNavigablePath().equals( parentParentPath );

		if ( fetchParentAsFetch.getFetchParent() instanceof Fetch ) {
			return new BiDirectionalFetchImpl(
					fetchParent.getNavigablePath().append( fetchable.getFetchableName() ),
					fetchParent,
					fetchParentAsFetch
			);
		}
		else {
			assert fetchParentAsFetch instanceof EntityResult;

			// note : the "`fetchParentAsFetch` is `RootBiDirectionalFetchImpl`" case would
			// 		be handled in the `Fetch` block since `RootBiDirectionalFetchImpl` is a Fetch

			return new RootBiDirectionalFetchImpl(
					fetchParent.getNavigablePath().append( fetchable.getFetchableName() ),
					fetchParent,
					(EntityResult) fetchParentAsFetch
			);
		}
	}
}
