/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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

	/**
	 * {@inheritDoc}
	 */
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