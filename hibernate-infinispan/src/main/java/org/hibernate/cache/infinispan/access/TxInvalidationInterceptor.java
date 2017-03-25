/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;

import org.infinispan.commands.AbstractVisitor;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.control.LockControlCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.util.InfinispanCollections;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.LocalTxInvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.jmx.annotations.MBean;
import org.infinispan.remoting.transport.Address;

/**
 * This interceptor acts as a replacement to the replication interceptor when the CacheImpl is configured with
 * ClusteredSyncMode as INVALIDATE.
 * <p/>
 * The idea is that rather than replicating changes to all caches in a cluster when write methods are called, simply
 * broadcast an {@link InvalidateCommand} on the remote caches containing all keys modified.  This allows the remote
 * cache to look up the value in a shared cache loader which would have been updated with the changes.
 *
 * @author Manik Surtani
 * @author Galder Zamarreño
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@MBean(objectName = "Invalidation", description = "Component responsible for invalidating entries on remote caches when entries are written to locally.")
public class TxInvalidationInterceptor extends BaseInvalidationInterceptor {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( TxInvalidationInterceptor.class );

	@Override
	public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
		if ( !isPutForExternalRead( command ) ) {
			return handleInvalidate( ctx, command, command.getKey() );
		}
		return invokeNextInterceptor( ctx, command );
	}

	@Override
	public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) throws Throwable {
		return handleInvalidate( ctx, command, command.getKey() );
	}

	@Override
	public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
		return handleInvalidate( ctx, command, command.getKey() );
	}

	@Override
	public Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {
		Object retval = invokeNextInterceptor( ctx, command );
		if ( !isLocalModeForced( command ) ) {
			// just broadcast the clear command - this is simplest!
			if ( ctx.isOriginLocal() ) {
				rpcManager.invokeRemotely( getMembers(), command, isSynchronous(command) ? syncRpcOptions : asyncRpcOptions );
			}
		}
		return retval;
	}

	@Override
	public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) throws Throwable {
		return handleInvalidate( ctx, command, command.getMap().keySet().toArray() );
	}

	@Override
	public Object visitPrepareCommand(TxInvocationContext ctx, PrepareCommand command) throws Throwable {
		Object retval = invokeNextInterceptor( ctx, command );
		log.tracef( "Entering InvalidationInterceptor's prepare phase.  Ctx flags are empty" );
		// fetch the modifications before the transaction is committed (and thus removed from the txTable)
		if ( shouldInvokeRemoteTxCommand( ctx ) ) {
			if ( ctx.getTransaction() == null ) {
				throw new IllegalStateException( "We must have an associated transaction" );
			}

			List<WriteCommand> mods = Arrays.asList( command.getModifications() );
			broadcastInvalidateForPrepare( mods, ctx );
		}
		else {
			log.tracef( "Nothing to invalidate - no modifications in the transaction." );
		}
		return retval;
	}

	@Override
	public Object visitLockControlCommand(TxInvocationContext ctx, LockControlCommand command) throws Throwable {
		Object retVal = invokeNextInterceptor( ctx, command );
		if ( ctx.isOriginLocal() ) {
			//unlock will happen async as it is a best effort
			boolean sync = !command.isUnlock();
			List<Address> members = getMembers();
			( (LocalTxInvocationContext) ctx ).remoteLocksAcquired(members);
			rpcManager.invokeRemotely(members, command, sync ? syncRpcOptions : asyncRpcOptions );
		}
		return retVal;
	}

	private Object handleInvalidate(InvocationContext ctx, WriteCommand command, Object... keys) throws Throwable {
		Object retval = invokeNextInterceptor( ctx, command );
		if ( command.isSuccessful() && !ctx.isInTxScope() ) {
			if ( keys != null && keys.length != 0 ) {
				if ( !isLocalModeForced( command ) ) {
					invalidateAcrossCluster( isSynchronous( command ), keys, ctx );
				}
			}
		}
		return retval;
	}

	private void broadcastInvalidateForPrepare(List<WriteCommand> modifications, InvocationContext ctx) throws Throwable {
		// A prepare does not carry flags, so skip checking whether is local or not
		if ( ctx.isInTxScope() ) {
			if ( modifications.isEmpty() ) {
				return;
			}

			InvalidationFilterVisitor filterVisitor = new InvalidationFilterVisitor( modifications.size() );
			filterVisitor.visitCollection( null, modifications );

			if ( filterVisitor.containsPutForExternalRead ) {
				log.debug( "Modification list contains a putForExternalRead operation.  Not invalidating." );
			}
			else if ( filterVisitor.containsLocalModeFlag ) {
				log.debug( "Modification list contains a local mode flagged operation.  Not invalidating." );
			}
			else {
				try {
					invalidateAcrossCluster( defaultSynchronous, filterVisitor.result.toArray(), ctx );
				}
				catch (Throwable t) {
					log.unableToRollbackInvalidationsDuringPrepare( t );
					if ( t instanceof RuntimeException ) {
						throw t;
					}
					else {
						throw new RuntimeException( "Unable to broadcast invalidation messages", t );
					}
				}
			}
		}
	}

	public static class InvalidationFilterVisitor extends AbstractVisitor {

		Set<Object> result;
		public boolean containsPutForExternalRead = false;
		public boolean containsLocalModeFlag = false;

		public InvalidationFilterVisitor(int maxSetSize) {
			result = new HashSet<Object>( maxSetSize );
		}

		private void processCommand(FlagAffectedCommand command) {
			containsLocalModeFlag = containsLocalModeFlag || ( command.getFlags() != null && command.getFlags().contains( Flag.CACHE_MODE_LOCAL ) );
		}

		@Override
		public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
			processCommand( command );
			containsPutForExternalRead =
					containsPutForExternalRead || ( command.getFlags() != null && command.getFlags().contains( Flag.PUT_FOR_EXTERNAL_READ ) );
			result.add( command.getKey() );
			return null;
		}

		@Override
		public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
			processCommand( command );
			result.add( command.getKey() );
			return null;
		}

		@Override
		public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) throws Throwable {
			processCommand( command );
			result.addAll( command.getAffectedKeys() );
			return null;
		}
	}

	private void invalidateAcrossCluster(boolean synchronous, Object[] keys, InvocationContext ctx) throws Throwable {
		// increment invalidations counter if statistics maintained
		incrementInvalidations();
		final InvalidateCommand invalidateCommand = commandsFactory.buildInvalidateCommand( InfinispanCollections.<Flag>emptySet(), keys );
		if ( log.isDebugEnabled() ) {
			log.debug( "Cache [" + rpcManager.getAddress() + "] replicating " + invalidateCommand );
		}

		ReplicableCommand command = invalidateCommand;
		if ( ctx.isInTxScope() ) {
			TxInvocationContext txCtx = (TxInvocationContext) ctx;
			// A Prepare command containing the invalidation command in its 'modifications' list is sent to the remote nodes
			// so that the invalidation is executed in the same transaction and locks can be acquired and released properly.
			// This is 1PC on purpose, as an optimisation, even if the current TX is 2PC.
			// If the cache uses 2PC it's possible that the remotes will commit the invalidation and the originator rolls back,
			// but this does not impact consistency and the speed benefit is worth it.
			command = commandsFactory.buildPrepareCommand( txCtx.getGlobalTransaction(), Collections.<WriteCommand>singletonList( invalidateCommand ), true );
		}
		rpcManager.invokeRemotely( getMembers(), command, synchronous ? syncRpcOptions : asyncRpcOptions );
	}
}
