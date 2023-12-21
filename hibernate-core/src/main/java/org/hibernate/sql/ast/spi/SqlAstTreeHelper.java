/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public final class SqlAstTreeHelper {

	private SqlAstTreeHelper() {
	}

	public static Predicate combinePredicates(Predicate baseRestriction, Predicate incomingRestriction) {
		if ( baseRestriction == null ) {
			return incomingRestriction;
		}
		if ( incomingRestriction == null ) {
			return baseRestriction;
		}

		final Junction combinedPredicate;

		if ( baseRestriction instanceof Junction ) {
			final Junction junction = (Junction) baseRestriction;
			if ( junction.isEmpty() ) {
				return incomingRestriction;
			}

			if ( junction.getNature() == Junction.Nature.CONJUNCTION ) {
				combinedPredicate = junction;
			}
			else {
				combinedPredicate = new Junction( Junction.Nature.CONJUNCTION );
				combinedPredicate.add( baseRestriction );
			}
		}
		else {
			combinedPredicate = new Junction( Junction.Nature.CONJUNCTION );
			combinedPredicate.add( baseRestriction );
		}

		final Junction secondJunction;
		if ( incomingRestriction instanceof Junction
				&& ( secondJunction = (Junction) incomingRestriction ).getNature() == Junction.Nature.CONJUNCTION ) {
			for ( Predicate predicate : secondJunction.getPredicates() ) {
				combinedPredicate.add( predicate );
			}
		}
		else {
			combinedPredicate.add( incomingRestriction );
		}

		return combinedPredicate;
	}
}
