/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate;

/**
 * Contains locking details (LockMode, Timeout and Scope).
 * 
 * @author Scott Marlow
 */
public class LockOptions
{
	/**
	 * NO_WAIT timeout value will not block for pessimistic locking
	 */
	public static final int NO_WAIT = 0;
	/**
	 * WAIT_FOREVER timeout value will block until pessimistic lock is obtained
	 */
	public static final int WAIT_FOREVER = -1;
	/**
	 * NONE represents LockMode.NONE (timeout + scope do not apply)
	 */
	public static final LockOptions NONE = new LockOptions(LockMode.NONE);
	/**
	 * READ represents LockMode.READ (timeout + scope do not apply)
	 */
	public static final LockOptions READ = new LockOptions(LockMode.READ);
	/**
	 * UPGRADE represents LockMode.UPGRADE (will wait forever for lock and
	 * scope of false meaning only entity is locked)
	 */
	public static final LockOptions UPGRADE = new LockOptions(LockMode.UPGRADE);

	private LockMode lockMode = LockMode.NONE;

	private int timeout = WAIT_FOREVER;	// timeout in milliseconds, 0 = no wait, -1 = wait indefinitely

	private boolean scope=false;// if true, cascade (pessimistic only) lock to collections and relationships
										 // owned by the entity.

	public LockOptions() {

	}


	public LockOptions( LockMode lockMode) {
		this.lockMode = lockMode;
	}

	/**
	 * Get the lock mode.
	 * @return the lock mode.
	 */
	public LockMode getLockMode() {
		return lockMode;
	}

	/**
	 * Specify the LockMode to be used.  The default is LockMode.none.
	 *
	 * @param lockMode
	 * @return this LockRequest instance for operation chaining.
	 */
	public LockOptions setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	/**
	 * Get the timeout setting.
	 *
	 * @return timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 */
	public int getTimeOut() {
		return timeout;
	}

	/**
	 * Specify the pessimistic lock timeout (check if your dialect supports this option).
	 * The default pessimistic lock behavior is to wait forever for the lock.
	 *
	 * @param timeout is time in milliseconds to wait for lock.  -1 means wait forever and 0 means no wait.
	 * @return this LockRequest instance for operation chaining.
	 */
	public LockOptions setTimeOut(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Check if locking is cascaded to owned collections and relationships.
	 * @return true if locking will be extended to owned collections and relationships.
	 */
	public boolean getScope() {
		return scope;
	}

	/**
	 * Specify if LockMode should be cascaded to owned collections and relationships.
	 * The association must be mapped with <tt>cascade="lock" for scope=true to work.
	 *
	 * @param scope
	 * @return
	 */
	public LockOptions setScope(boolean scope) {
		this.scope = scope;
		return this;
	}

	/**
	 * Copy From to Dest
	 * @param from is copied from
	 * @param dest is copied to
	 * @return dest
	 */
	public static LockOptions copy(LockOptions from, LockOptions dest) {
		dest.setLockMode(from.getLockMode());
		dest.setScope(from.getScope());
		dest.setTimeOut(from.getTimeOut());
		return dest;
	}

}
