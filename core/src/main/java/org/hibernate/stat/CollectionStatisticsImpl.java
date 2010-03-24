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
package org.hibernate.stat;

/**
 * Collection related statistics
 *
 * @author Gavin King
 */
public class CollectionStatisticsImpl extends CategorizedStatistics implements CollectionStatistics {

	CollectionStatisticsImpl(String role) {
		super(role);
	}

	long loadCount;
	long fetchCount;
	long updateCount;
	long removeCount;
	long recreateCount;

	public long getLoadCount() {
		return loadCount;
	}

	public long getFetchCount() {
		return fetchCount;
	}

	public long getRecreateCount() {
		return recreateCount;
	}

	public long getRemoveCount() {
		return removeCount;
	}

	public long getUpdateCount() {
		return updateCount;
	}

	public String toString() {
		return new StringBuilder()
				.append("CollectionStatistics")
				.append("[loadCount=").append(this.loadCount)
				.append(",fetchCount=").append(this.fetchCount)
				.append(",recreateCount=").append(this.recreateCount)
				.append(",removeCount=").append(this.removeCount)
				.append(",updateCount=").append(this.updateCount)
				.append(']')
				.toString();
	}
}
