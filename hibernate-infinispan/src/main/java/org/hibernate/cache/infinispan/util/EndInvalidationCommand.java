/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import java.util.Arrays;

import org.hibernate.cache.infinispan.access.PutFromLoadValidator;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.context.InvocationContext;

/**
 * Sent in commit phase (afterQuery DB commit) to remote nodes in order to stop invalidating
 * putFromLoads.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class EndInvalidationCommand extends BaseRpcCommand {
	private Object[] keys;
	private Object sessionTransactionId;
	private PutFromLoadValidator putFromLoadValidator;

	public EndInvalidationCommand(String cacheName) {
		this(cacheName, null, null);
	}

	/**
	 * @param cacheName name of the cache to evict
	 */
	public EndInvalidationCommand(String cacheName, Object[] keys, Object sessionTransactionId) {
		super(cacheName);
		this.keys = keys;
		this.sessionTransactionId = sessionTransactionId;
	}

	@Override
	public Object perform(InvocationContext ctx) throws Throwable {
		for (Object key : keys) {
			putFromLoadValidator.endInvalidatingKey(sessionTransactionId, key);
		}
		return null;
	}

	@Override
	public byte getCommandId() {
		return CacheCommandIds.END_INVALIDATION;
	}

	@Override
	public Object[] getParameters() {
		return new Object[] { keys, sessionTransactionId};
	}

	@Override
	public void setParameters(int commandId, Object[] parameters) {
		keys = (Object[]) parameters[0];
		sessionTransactionId = parameters[1];
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof EndInvalidationCommand)) {
			return false;
		}

		EndInvalidationCommand that = (EndInvalidationCommand) o;

		if (cacheName == null ? cacheName != null : !cacheName.equals(that.cacheName)) {
			return false;
		}
		if (!Arrays.equals(keys, that.keys)) {
			return false;
		}
		return !(sessionTransactionId != null ? !sessionTransactionId.equals(that.sessionTransactionId) : that.sessionTransactionId != null);

	}

	@Override
	public int hashCode() {
		int result = cacheName != null ? cacheName.hashCode() : 0;
		result = 31 * result + (keys != null ? Arrays.hashCode(keys) : 0);
		result = 31 * result + (sessionTransactionId != null ? sessionTransactionId.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("EndInvalidationCommand{");
		sb.append("cacheName=").append(cacheName);
		sb.append(", keys=").append(Arrays.toString(keys));
		sb.append(", sessionTransactionId=").append(sessionTransactionId);
		sb.append('}');
		return sb.toString();
	}
}
