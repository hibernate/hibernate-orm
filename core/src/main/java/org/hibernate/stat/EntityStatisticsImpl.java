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
 * Entity related statistics
 *
 * @author Gavin King
 */
public class EntityStatisticsImpl extends CategorizedStatistics implements EntityStatistics {

	EntityStatisticsImpl(String name) {
		super(name);
	}

	long loadCount;
	long updateCount;
	long insertCount;
	long deleteCount;
	long fetchCount;
	long optimisticFailureCount;

	public long getDeleteCount() {
		return deleteCount;
	}

	public long getInsertCount() {
		return insertCount;
	}

	public long getLoadCount() {
		return loadCount;
	}

	public long getUpdateCount() {
		return updateCount;
	}

	public long getFetchCount() {
		return fetchCount;
	}

	public long getOptimisticFailureCount() {
		return optimisticFailureCount;
	}

	public String toString() {
		return new StringBuilder()
				.append("EntityStatistics")
				.append("[loadCount=").append(this.loadCount)
				.append(",updateCount=").append(this.updateCount)
				.append(",insertCount=").append(this.insertCount)
				.append(",deleteCount=").append(this.deleteCount)
				.append(",fetchCount=").append(this.fetchCount)
				.append(",optimisticLockFailureCount=").append(this.optimisticFailureCount)
				.append(']')
				.toString();
	}

}
