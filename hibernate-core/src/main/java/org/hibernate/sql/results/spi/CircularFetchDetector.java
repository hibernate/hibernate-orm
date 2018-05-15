/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.results.internal.BiDirectionalFetchImpl;

/**
 * Maintains state while processing a Fetch graph to be able to detect
 * and handle circular bi-directional references
 *
 * @author Steve Ebersole
 */
public class CircularFetchDetector {

	public Fetch findBiDirectionalFetch(FetchParent fetchParent, Fetchable fetchable) {
		// bi-directional references are a special case that need special treatment.
		//
		// `p.address.resident.homeAddress
		//
		// what we mean is a fetch path like `a.parent.child.parent`.  here the terminal
		// `parent` name is the same reference as its parent's (`a.parent.child`)
		// parent's (a.parent`) path.
		//
		// In such a case we want to (mostly) reuse the "parent parent" path fetch
		//
		// see if we have such a case...

		final NavigablePath parentParentPath = fetchParent.getNavigablePath().getParent();
		if ( parentParentPath != null ) {
			boolean isCircular = fetchable.isCircular( fetchParent );
			if ( isCircular ) {
				final FetchParent parentFetchParent = ( (Fetch) fetchParent ).getFetchParent();

				// we do...
				//
				// in other words, the `Fetchable`'s `NavigablePath`, relative to its FetchParent here would
				// be:
				// 		a.parent.child.parent
				//
				// it's parentPath is `a.parent.child` so its parentParentPath is `a.parent`.  so this Fetchable's
				// path is really the same reference as its parentParentPath.  This is a special case, handled here...

				// first, this *should* mean we have already "seen" the Fetch generated parentParentPath.  So
				// look up in the `navigablePathFetchMap` to get that Fetch

				// and use it to create and register the "bi directional" form

				final NavigablePath fetchableNavigablePath = fetchParent.getNavigablePath()
						.append( fetchable.getNavigableRole().getNavigableName() );

				final Fetch biDirectionalFetch = new BiDirectionalFetchImpl(
						parentFetchParent,
						fetchableNavigablePath
				);

				return biDirectionalFetch;
			}
		}

		return null;
	}

}
