/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqlAstHelper {
	/**
	 * Singleton access
	 */
	public static final SqlAstHelper INSTANCE = new SqlAstHelper();

	private SqlAstHelper() {
	}


	public static Predicate combinePredicates(Predicate baseRestriction, Predicate incomingRestriction) {
		if ( baseRestriction == null ) {
			return incomingRestriction;
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
		}

		combinedPredicate.add( incomingRestriction );

		return combinedPredicate;
	}
}
