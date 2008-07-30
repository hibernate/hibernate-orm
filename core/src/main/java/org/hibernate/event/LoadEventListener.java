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
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for handling of load events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface LoadEventListener extends Serializable {

	/** 
	 * Handle the given load event.
     *
     * @param event The load event to be handled.
     * @return The result (i.e., the loaded entity).
     * @throws HibernateException
     */
	public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException;

	public static final LoadType RELOAD = new LoadType("GET")
			.setAllowNulls(false)
			.setAllowProxyCreation(false)
			.setCheckDeleted(true)
			.setNakedEntityReturned(false);

	public static final LoadType GET = new LoadType("GET")
			.setAllowNulls(true)
			.setAllowProxyCreation(false)
			.setCheckDeleted(true)
			.setNakedEntityReturned(false);
	
	public static final LoadType LOAD = new LoadType("LOAD")
			.setAllowNulls(false)
			.setAllowProxyCreation(true)
			.setCheckDeleted(true)
			.setNakedEntityReturned(false);
	
	public static final LoadType IMMEDIATE_LOAD = new LoadType("IMMEDIATE_LOAD")
			.setAllowNulls(true)
			.setAllowProxyCreation(false)
			.setCheckDeleted(false)
			.setNakedEntityReturned(true);
	
	public static final LoadType INTERNAL_LOAD_EAGER = new LoadType("INTERNAL_LOAD_EAGER")
			.setAllowNulls(false)
			.setAllowProxyCreation(false)
			.setCheckDeleted(false)
			.setNakedEntityReturned(false);
	
	public static final LoadType INTERNAL_LOAD_LAZY = new LoadType("INTERNAL_LOAD_LAZY")
			.setAllowNulls(false)
			.setAllowProxyCreation(true)
			.setCheckDeleted(false)
			.setNakedEntityReturned(false);
	
	public static final LoadType INTERNAL_LOAD_NULLABLE = new LoadType("INTERNAL_LOAD_NULLABLE")
			.setAllowNulls(true)
			.setAllowProxyCreation(false)
			.setCheckDeleted(false)
			.setNakedEntityReturned(false);

	public static final class LoadType {
		private String name;

		private boolean nakedEntityReturned;
		private boolean allowNulls;
		private boolean checkDeleted;
		private boolean allowProxyCreation;

        private LoadType(String name) {
	        this.name = name;
        }

		public boolean isAllowNulls() {
			return allowNulls;
		}

		private LoadType setAllowNulls(boolean allowNulls) {
			this.allowNulls = allowNulls;
			return this;
		}

		public boolean isNakedEntityReturned() {
			return nakedEntityReturned;
		}

		private LoadType setNakedEntityReturned(boolean immediateLoad) {
			this.nakedEntityReturned = immediateLoad;
			return this;
		}

		public boolean isCheckDeleted() {
			return checkDeleted;
		}

		private LoadType setCheckDeleted(boolean checkDeleted) {
			this.checkDeleted = checkDeleted;
			return this;
		}

		public boolean isAllowProxyCreation() {
			return allowProxyCreation;
		}

		private LoadType setAllowProxyCreation(boolean allowProxyCreation) {
			this.allowProxyCreation = allowProxyCreation;
			return this;
		}

		public String getName() {
			return name;
		}
		
		public String toString() {
			return name;
		}
	}
}
