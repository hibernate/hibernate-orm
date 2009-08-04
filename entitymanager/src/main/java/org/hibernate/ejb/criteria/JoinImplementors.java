/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.ejb.criteria;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.SetJoin;

/**
 * Consolidates the {@link Join} and {@link Fetch} hierarchies since that is how we implement them.
 * This allows us to treat them polymorphically.
 *
 * @author Steve Ebersole
 */
public interface JoinImplementors {
	public static interface JoinImplementor<Z,X> extends Join<Z,X>, Fetch<Z,X> {
	}

	public static interface CollectionJoinImplementor<Z,X> extends CollectionJoin<Z,X>, Fetch<Z,X> {
	}

	public static interface SetJoinImplementor<Z,X> extends SetJoin<Z,X>, Fetch<Z,X> {
	}

	public static interface ListJoinImplementor<Z,X> extends ListJoin<Z,X>, Fetch<Z,X> {
	}

	public static interface MapJoinImplementor<Z,K,V> extends MapJoin<Z,K,V>, Fetch<Z,V> {
	}
}
