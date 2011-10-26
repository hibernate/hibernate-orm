/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;

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

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       DistinctResultTransformer.class.getName());

	/**
	 * Helper class to handle distincting
	 */
	private static final class Identity {
		final Object entity;

		private Identity(Object entity) {
			this.entity = entity;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
        public boolean equals(Object other) {
			return Identity.class.isInstance( other )
					&& this.entity == ( ( Identity ) other ).entity;
		}

		/**
		 * {@inheritDoc}
		 */
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
		List result = new ArrayList( list.size() );
		Set distinct = new HashSet();
		for ( int i = 0; i < list.size(); i++ ) {
			Object entity = list.get( i );
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
