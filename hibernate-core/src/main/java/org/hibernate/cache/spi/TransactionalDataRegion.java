/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.cache.spi;

/**
 * Defines contract for regions which hold transactionally-managed data.
 * <p/>
 * The data is not transactionally managed within the region; merely it is
 * transactionally-managed in relation to its association with a particular
 * {@link org.hibernate.Session}.
 *
 * @author Steve Ebersole
 */
public interface TransactionalDataRegion extends Region {
	/**
	 * Is the underlying cache implementation aware of (and "participating in")
	 * ongoing JTA transactions?
	 * <p/>
	 * Regions which report that they are transaction-aware are considered
	 * "synchronous", in that we assume we can immediately (i.e. synchronously)
	 * write the changes to the cache and that the cache will properly manage
	 * application of the written changes within the bounds of ongoing JTA
	 * transactions.  Conversely, regions reporting false are considered
	 * "asynchronous", where it is assumed that changes must be manually
	 * delayed by Hibernate until we are certain that the current transaction
	 * is successful (i.e. maintaining READ_COMMITTED isolation).
	 *
	 * @return True if transaction aware; false otherwise.
	 */
	public boolean isTransactionAware();

	public CacheDataDescription getCacheDataDescription();
}
