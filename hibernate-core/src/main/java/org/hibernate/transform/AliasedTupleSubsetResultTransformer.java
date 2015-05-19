/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;

/**
 * An implementation of TupleSubsetResultTransformer that ignores a
 * tuple element if its corresponding alias is null.
 *
 * @author Gail Badner
 */
public abstract class AliasedTupleSubsetResultTransformer
		extends BasicTransformerAdapter
		implements TupleSubsetResultTransformer {

	@Override
	public boolean[] includeInTransform(String[] aliases, int tupleLength) {
		if ( aliases == null ) {
			throw new IllegalArgumentException( "aliases cannot be null" );
		}
		if ( aliases.length != tupleLength ) {
			throw new IllegalArgumentException(
					"aliases and tupleLength must have the same length; " +
							"aliases.length=" + aliases.length + "tupleLength=" + tupleLength
			);
		}
		boolean[] includeInTransform = new boolean[tupleLength];
		for ( int i = 0 ; i < aliases.length ; i++ ) {
			if ( aliases[ i ] != null ) {
				includeInTransform[ i ] = true;
			}
		}
		return includeInTransform;
	}
}
