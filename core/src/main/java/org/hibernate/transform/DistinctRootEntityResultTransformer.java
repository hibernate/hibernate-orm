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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gavin King
 */
public class DistinctRootEntityResultTransformer implements ResultTransformer {

	private static final Logger log = LoggerFactory.getLogger(DistinctRootEntityResultTransformer.class);

	static final class Identity {
		final Object entity;
		Identity(Object entity) {
			this.entity = entity;
		}
		public boolean equals(Object other) {
			Identity that = (Identity) other;
			return entity==that.entity;
		}
		public int hashCode() {
			return System.identityHashCode(entity);
		}
	}

	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple[ tuple.length-1 ];
	}

	public List transformList(List list) {
		List result = new ArrayList( list.size() );
		Set distinct = new HashSet();
		for ( int i=0; i<list.size(); i++ ) {
			Object entity = list.get(i);
			if ( distinct.add( new Identity(entity) ) ) {
				result.add(entity);
			}
		}
		if ( log.isDebugEnabled() ) log.debug(
			"transformed: " +
			list.size() + " rows to: " +
			result.size() + " distinct results"
		);
		return result;
	}

}
