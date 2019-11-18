/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;


import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.results.internal.domain.BiDirectionalFetchImpl;

/**
 * Maintains state while processing a Fetch graph to be able to detect
 * and handle circular bi-directional references
 *
 * @author Steve Ebersole
 */
public class CircularFetchDetector {

	public Fetch findBiDirectionalFetch(FetchParent fetchParent, Fetchable fetchable, SqlAstProcessingState creationState) {
		if ( !fetchable.isCircular( fetchParent, creationState ) ) {
			return null;
		}

		final NavigablePath navigablePath = fetchParent.getNavigablePath();
		if ( navigablePath.getParent().getParent() == null ) {
			return new BiDirectionalFetchImpl(
					navigablePath,
					fetchParent,
					fetchable,
					fetchParent.getNavigablePath().getParent()
			);
		}
		else {
			return new BiDirectionalFetchImpl(
					navigablePath.append( fetchable.getFetchableName() ),
					fetchParent,
					fetchable,
					navigablePath.getParent()
			);
		}
	}
}
