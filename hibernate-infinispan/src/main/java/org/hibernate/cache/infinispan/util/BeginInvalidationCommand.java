/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.hibernate.internal.util.compare.EqualsHelper;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.CacheNotifier;

import java.util.Arrays;
import java.util.Set;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class BeginInvalidationCommand extends InvalidateCommand {
	private Object lockOwner;

	public BeginInvalidationCommand() {
	}

	public BeginInvalidationCommand(CacheNotifier notifier, Set<Flag> flags, Object[] keys, Object lockOwner) {
		super(notifier, flags, keys);
		this.lockOwner = lockOwner;
	}

	public Object getLockOwner() {
		return lockOwner;
	}

	@Override
	public Object[] getParameters() {
		if (keys == null || keys.length == 0) {
			return new Object[]{0, lockOwner};
		}
		if (keys.length == 1) {
			return new Object[]{1, keys[0], lockOwner};
		}
		Object[] retval = new Object[keys.length + 2];
		retval[0] = keys.length;
		System.arraycopy(keys, 0, retval, 1, keys.length);
		return retval;
	}

	@Override
	public void setParameters(int commandId, Object[] args) {
		if (commandId != CacheCommandIds.BEGIN_INVALIDATION) {
			throw new IllegalStateException("Invalid method id");
		}
		int size = (Integer) args[0];
		keys = new Object[size];
		if (size == 1) {
			keys[0] = args[1];
		}
		else if (size > 0) {
			System.arraycopy(args, 1, keys, 0, size);
		}
		lockOwner = args[args.length - 1];
	}


	@Override
	public byte getCommandId() {
		return CacheCommandIds.BEGIN_INVALIDATION;
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		if (o instanceof BeginInvalidationCommand) {
			BeginInvalidationCommand bic = (BeginInvalidationCommand) o;
			return EqualsHelper.equals(lockOwner, bic.lockOwner);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return super.hashCode() + (lockOwner == null ? 0 : lockOwner.hashCode());
	}

	@Override
	public String toString() {
		return "BeginInvalidateCommand{keys=" + Arrays.toString(keys) +
				", lockOwner=" + lockOwner + '}';
	}
}
