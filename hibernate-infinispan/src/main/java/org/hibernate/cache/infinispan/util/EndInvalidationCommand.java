/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.context.InvocationContext;

import java.util.Arrays;

/**
 * Sent in commit phase (after DB commit) to remote nodes in order to stop invalidating
 * putFromLoads.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class EndInvalidationCommand extends BaseRpcCommand {
	private Object[] keys;
	private Object lockOwner;
	private PutFromLoadValidator putFromLoadValidator;

	public EndInvalidationCommand(String cacheName) {
		this(cacheName, null, null);
	}

	/**
	 * @param cacheName name of the cache to evict
	 */
	public EndInvalidationCommand(String cacheName, Object[] keys, Object lockOwner) {
		super(cacheName);
		this.keys = keys;
		this.lockOwner = lockOwner;
	}

	@Override
	public Object perform(InvocationContext ctx) throws Throwable {
		for (Object key : keys) {
			putFromLoadValidator.endInvalidatingKey(lockOwner, key);
		}
		return null;
	}

	@Override
	public byte getCommandId() {
		return CacheCommandIds.END_INVALIDATION;
	}

	@Override
	public Object[] getParameters() {
		return new Object[] { keys, lockOwner };
	}

	@Override
	public void setParameters(int commandId, Object[] parameters) {
		keys = (Object[]) parameters[0];
		lockOwner = parameters[1];
	}

	@Override
	public boolean isReturnValueExpected() {
		return false;
	}

	@Override
	public boolean canBlock() {
		return true;
	}

	public void setPutFromLoadValidator(PutFromLoadValidator putFromLoadValidator) {
		this.putFromLoadValidator = putFromLoadValidator;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("EndInvalidationCommand{");
		sb.append("cacheName=").append(cacheName);
		sb.append(", keys=").append(Arrays.toString(keys));
		sb.append(", lockOwner=").append(lockOwner);
		sb.append('}');
		return sb.toString();
	}
}
