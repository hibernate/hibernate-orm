//$Id: LoadEventListener.java 7785 2005-08-08 23:24:44Z oneovthafew $
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
