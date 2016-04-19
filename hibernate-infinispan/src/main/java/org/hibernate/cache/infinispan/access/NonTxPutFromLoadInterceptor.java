/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.util.BeginInvalidationCommand;
import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.EndInvalidationCommand;

import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.rpc.RpcManager;

/**
 * Non-transactional counterpart of {@link TxPutFromLoadInterceptor}.
 * Invokes {@link PutFromLoadValidator#beginInvalidatingKey(Object, Object)} for each invalidation from
 * remote node ({@link BeginInvalidationCommand} and sends {@link EndInvalidationCommand} afterQuery the transaction
 * is complete, with help of {@link InvalidationSynchronization};
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class NonTxPutFromLoadInterceptor extends BaseCustomInterceptor {
	private final String cacheName;
	private final PutFromLoadValidator putFromLoadValidator;
	private CacheCommandInitializer commandInitializer;
	private RpcManager rpcManager;

	public NonTxPutFromLoadInterceptor(PutFromLoadValidator putFromLoadValidator, String cacheName) {
		this.putFromLoadValidator = putFromLoadValidator;
		this.cacheName = cacheName;
	}

	@Inject
	public void injectDependencies(CacheCommandInitializer commandInitializer, RpcManager rpcManager) {
		this.commandInitializer = commandInitializer;
		this.rpcManager = rpcManager;
	}

	@Override
	public Object visitInvalidateCommand(InvocationContext ctx, InvalidateCommand command) throws Throwable {
		if (!ctx.isOriginLocal() && command instanceof BeginInvalidationCommand) {
			for (Object key : command.getKeys()) {
				putFromLoadValidator.beginInvalidatingKey(((BeginInvalidationCommand) command).getSessionTransactionId(), key);
			}
		}
		return invokeNextInterceptor(ctx, command);
	}

	public void broadcastEndInvalidationCommand(Object[] keys, Object sessionTransactionId) {
		assert sessionTransactionId != null;
		EndInvalidationCommand endInvalidationCommand = commandInitializer.buildEndInvalidationCommand(
				cacheName, keys, sessionTransactionId);
		rpcManager.invokeRemotely(null, endInvalidationCommand, rpcManager.getDefaultRpcOptions(false, DeliverOrder.NONE));
	}
}
