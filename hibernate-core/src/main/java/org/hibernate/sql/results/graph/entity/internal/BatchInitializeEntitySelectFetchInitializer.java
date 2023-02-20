/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Loads entities from the persistence context or creates proxies if not found there,
 * and initializes all proxies in a batch.
 */
public class BatchInitializeEntitySelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer {

	private final Set<EntityKey> toBatchLoad = new HashSet<>();
	private State state = State.UNINITIALIZED;

	public BatchInitializeEntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, identifierAssembler );
	}

	@Override
	protected void registerResolutionListener() {
		// No-op, because we resolve a proxy
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( state != State.UNINITIALIZED ) {
			return;
		}
		super.resolveKey( rowProcessingState );
		state = entityKey == null ? State.MISSING : State.KEY_RESOLVED;
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}

		state = State.INITIALIZED;
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		entityInstance = session.getPersistenceContext().getEntity( entityKey );
		if ( entityInstance == null ) {
			final LoadingEntityEntry loadingEntityEntry = rowProcessingState.getJdbcValuesSourceProcessingState()
					.findLoadingEntityLocally( entityKey );
			if ( loadingEntityEntry != null ) {
				loadingEntityEntry.getEntityInitializer().resolveInstance( rowProcessingState );
				entityInstance = loadingEntityEntry.getEntityInstance();
			}
			else {
				// Force creating a proxy
				entityInstance = session.internalLoad(
						entityKey.getEntityName(),
						entityKey.getIdentifier(),
						false,
						false
				);
				toBatchLoad.add( entityKey );
			}
		}
	}

	@Override
	public boolean isEntityInitialized() {
		return state == State.INITIALIZED;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		super.finishUpRow( rowProcessingState );
		state = State.UNINITIALIZED;
	}

	@Override
	public void endLoading(ExecutionContext context) {
		final SharedSessionContractImplementor session = context.getSession();
		for ( EntityKey key : toBatchLoad ) {
			loadInstance( key, referencedModelPart, session );
		}
		toBatchLoad.clear();
		parentAccess = null;
	}

	@Override
	public String toString() {
		return "BatchInitializeEntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	enum State {
		UNINITIALIZED,
		MISSING,
		KEY_RESOLVED,
		INITIALIZED
	}

}
