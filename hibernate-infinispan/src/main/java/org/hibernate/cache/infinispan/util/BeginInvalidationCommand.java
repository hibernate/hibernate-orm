/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.hibernate.internal.util.compare.EqualsHelper;
import org.infinispan.commands.write.AbstractDataWriteCommand;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.remoting.transport.Address;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class BeginInvalidationCommand extends InvalidateCommand {
	// this is a hack to keep compatibility with both Infinispan 7 and 8
	// TODO: remove this when rebasing on Infinispan 8
	private static final Field commandInvocationIdField;
	private static final Method generateIdMethod;

	static {
		Field commandInvocationId = null;
		Method generateId = null;
		try {
			commandInvocationId = AbstractDataWriteCommand.class.getDeclaredField("commandInvocationId");
			commandInvocationId.setAccessible(true);
			Class commandInvocationIdClass = Class.forName("org.infinispan.commands.CommandInvocationId");
			generateId = commandInvocationIdClass.getMethod("generateId", Address.class);
		}
		catch (NoSuchFieldException e) {
		}
		catch (ClassNotFoundException e) {
			// already found field and not the class?
			throw new IllegalStateException(e);
		}
		catch (NoSuchMethodException e) {
			// already found field and not the method?
			throw new IllegalStateException(e);
		}
		commandInvocationIdField = commandInvocationId;
		generateIdMethod = generateId;
	}

	private Object sessionTransactionId;

	public BeginInvalidationCommand() {
	}

	public BeginInvalidationCommand(CacheNotifier notifier, Set<Flag> flags, Object[] keys, Address address, Object sessionTransactionId) {
		super();
		this.notifier = notifier;
		this.flags = flags;
		this.keys = keys;
		this.sessionTransactionId = sessionTransactionId;
		if (commandInvocationIdField != null) {
			try {
				commandInvocationIdField.set(this, generateIdMethod.invoke(null, address));
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
			catch (InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public Object getSessionTransactionId() {
		return sessionTransactionId;
	}

	@Override
	public Object[] getParameters() {
		Object commandInvocationId = null;
		if (commandInvocationIdField != null) {
			try {
				commandInvocationId = commandInvocationIdField.get(this);
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		if (keys == null || keys.length == 0) {
			return new Object[]{flags, sessionTransactionId, commandInvocationId, 0};
		}
		if (keys.length == 1) {
			return new Object[]{flags, sessionTransactionId, commandInvocationId, 1, keys[0]};
		}
		Object[] retval = new Object[keys.length + 4];
		retval[0] = flags;
		retval[1] = sessionTransactionId;
		retval[2] = commandInvocationId;
		retval[3] = keys.length;
		System.arraycopy(keys, 0, retval, 4, keys.length);
		return retval;
	}

	@Override
	public void setParameters(int commandId, Object[] args) {
		if (commandId != CacheCommandIds.BEGIN_INVALIDATION) {
			throw new IllegalStateException("Invalid method id");
		}
		flags = (Set<Flag>) args[0];
		sessionTransactionId = args[1];
		Object commandInvocationId = args[2];
		if (commandInvocationIdField != null) {
			try {
				commandInvocationIdField.set(this, commandInvocationId);
			}
			catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		int size = (Integer) args[3];
		keys = new Object[size];
		if (size == 1) {
			keys[0] = args[4];
		}
		else if (size > 0) {
			System.arraycopy(args, 4, keys, 0, size);
		}
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
			return EqualsHelper.equals(sessionTransactionId, bic.sessionTransactionId);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return super.hashCode() + (sessionTransactionId == null ? 0 : sessionTransactionId.hashCode());
	}

	@Override
	public String toString() {
		return "BeginInvalidateCommand{keys=" + Arrays.toString(keys) +
				", sessionTransactionId=" + sessionTransactionId + '}';
	}
}
