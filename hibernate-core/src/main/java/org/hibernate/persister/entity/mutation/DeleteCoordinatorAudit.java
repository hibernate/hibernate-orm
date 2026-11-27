package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.audit.ModificationType;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

import static java.util.Objects.requireNonNull;

/**
 * Delete coordinator for audited entities.
 */
@Internal
public class DeleteCoordinatorAudit extends AbstractAuditCoordinator implements DeleteCoordinator {
	private final DeleteCoordinator currentDeleteCoordinator;

	public DeleteCoordinatorAudit(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull DeleteCoordinator currentDeleteCoordinator) {
		super( entityPersister, factory );
		this.currentDeleteCoordinator = currentDeleteCoordinator;
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentDeleteCoordinator.getStaticMutationOperationGroup();
	}

	@Override
	public void delete(
			@Nullable Object instance,
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		final var entity = requireNonNull( instance );
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		final var entityKey = entityEntry == null ? new EntityKey( id, entityPersister() ) : entityEntry.getEntityKey();
		final var loadedState = entityEntry == null ? null : entityEntry.getLoadedState();
		final var state = entityPersister().getEntityAuditSupport().deletionState( entityKey,
				loadedState == null ? entityPersister().getValues( entity ) : loadedState, loadedState != null, session );

		currentDeleteCoordinator.delete( entity, id, version, session );

		enqueueAuditEntry( entityKey, entity, state, ModificationType.DEL, session );
	}
}
