/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.CoreMessageLogger;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Distinctions the result tuples in the final result based on the defined
 * equality of the tuples.
 * <p/>
 * Since this transformer is stateless, all instances would be considered equal.
 * So for optimization purposes we limit it to a single, singleton {@link #INSTANCE instance}.
 *
 * @author Steve Ebersole
 */
public class DistinctResultTransformer extends BasicTransformerAdapter {
	public static final DistinctResultTransformer INSTANCE = new DistinctResultTransformer();

	private static final CoreMessageLogger LOG = messageLogger( DistinctResultTransformer.class );

	/**
	 * Helper class to handle distincting
	 */
	private static final class Identity {
		final Object entity;

		private Identity(Object entity) {
			this.entity = entity;
		}

		@Override
		public boolean equals(Object other) {
			return Identity.class.isInstance( other )
					&& this.entity == ( (Identity) other ).entity;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode( entity );
		}
	}

	/**
	 * Disallow instantiation of DistinctResultTransformer.
	 */
	private DistinctResultTransformer() {
	}

	/**
	 * Uniquely distinct each tuple row here.
	 */
	@Override
	public List transformList(List list) {
		List<Object> result = new ArrayList<Object>( list.size() );
		Set<Identity> distinct = new HashSet<Identity>();
		for ( Object entity : list ) {
			if ( distinct.add( new Identity( entity ) ) ) {
				result.add( entity );
			}
		}
		LOG.debugf( "Transformed: %s rows to: %s distinct results", list.size(), result.size() );
		return result;
	}

	/**
	 * Serialization hook for ensuring singleton uniqueing.
	 *
	 * @return The singleton instance : {@link #INSTANCE}
	 */
	private Object readResolve() {
		return INSTANCE;
	}
}
