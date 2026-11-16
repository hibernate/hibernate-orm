package org.hibernate.temporal.audit;

import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.AuditWorkQueue;
import org.hibernate.audit.spi.AuditWriter;
import org.hibernate.audit.spi.CollectionAuditWriter;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.AfterCompletionCallback;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Rollback must discard deferred audit work even when its before-completion
/// callback was never invoked; later transactions must register their own work.
///
/// @author Steve Ebersole
class AuditWorkQueueCompletionTest {
	@ParameterizedTest
	@CsvSource({"false,rollback", "true,rollback", "false,commit", "true,commit", "false,failure", "true,failure"})
	void completionReleasesPendingWork(boolean collectionFirst, String completion) {
		var session = mock( SharedSessionContractImplementor.class );
		var factory = mock( SessionFactoryImplementor.class );
		when( session.getFactory() ).thenReturn( factory );
		when( factory.getServiceRegistry() ).thenReturn( mock( ServiceRegistryImplementor.class ) );
		when( factory.getSessionFactoryOptions() ).thenReturn( mock( SessionFactoryOptions.class ) );
		var callbacks = spy( new TransactionCompletionCallbacksImpl( session ) );
		when( session.getTransactionCompletionCallbacks() ).thenReturn( callbacks );
		var queue = new AuditWorkQueue();
		var entity = mock( AuditWriter.class );
		var collection = mock( CollectionAuditWriter.class );
		var key = mock( EntityKey.class );
		var persister = mock( CollectionPersister.class );
		when( persister.getRole() ).thenReturn( "audit.values" );
		when( persister.getKeyType() ).thenReturn( mock( Type.class ) );
		when( persister.getOwnerEntityPersister() ).thenReturn( mock( EntityPersister.class ) );
		var values = mock( PersistentCollection.class );
		Runnable enqueueEntity = () -> queue.enqueue( key, new Object(), new Object[0], ModificationType.ADD, entity, session );
		Runnable enqueueCollection = () -> queue.enqueueCollection( persister, values, 1, null, collection, session );
		if ( collectionFirst ) { enqueueCollection.run(); enqueueEntity.run(); }
		else { enqueueEntity.run(); enqueueCollection.run(); }
		verify( callbacks, times( 1 ) ).registerCallback( any( BeforeCompletionCallback.class ) );
		verify( callbacks, times( 1 ) ).registerCallback( any( AfterCompletionCallback.class ) );
		if ( completion.equals( "failure" ) ) {
			doThrow( new IllegalStateException( "audit writer failed" ) ).when( entity )
					.writeAuditRow( any(), any(), any(), any(), any() );
			assertThatThrownBy( callbacks::beforeTransactionCompletion ).hasMessageContaining( "audit writer failed" );
		}
		else if ( completion.equals( "commit" ) ) callbacks.beforeTransactionCompletion();
		if ( !completion.equals( "commit" ) ) callbacks.clearBeforeTransactionCallbacks();
		callbacks.afterTransactionCompletion( completion.equals( "commit" ) );
		callbacks.afterTransactionCompletion( false ); // Repeated cleanup cannot restore discarded work.
		if ( completion.equals( "rollback" ) ) verifyNoInteractions( entity, collection );
		var next = mock( AuditWriter.class );
		queue.enqueue( mock( EntityKey.class ), new Object(), new Object[0], ModificationType.ADD, next, session );
		verify( callbacks, times( 2 ) ).registerCallback( any( BeforeCompletionCallback.class ) );
		verify( callbacks, times( 2 ) ).registerCallback( any( AfterCompletionCallback.class ) );
		callbacks.beforeTransactionCompletion(); callbacks.afterTransactionCompletion( true );
		verify( next, times( 1 ) ).writeAuditRow( any(), any(), any(), any(), any() );
		verify( entity, times( completion.equals( "rollback" ) ? 0 : 1 ) ).writeAuditRow( any(), any(), any(), any(), any() );
		verify( collection, times( completion.equals( "commit" ) ? 1 : 0 ) ).writeCollectionAuditRows( any(), any(), isNull(), any() );
	}
}
