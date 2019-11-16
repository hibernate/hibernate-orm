/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParentAccess;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class DelayedEntityFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	private final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private FetchStrategy mappedFetchedStrategy;
	private LockMode lockMode;
	private final EntityPersister concreteDescriptor;
	private final DomainResultAssembler fkValueAssembler;


	private Object entityInstance;
	private Object fkValue;

	protected DelayedEntityFetchInitializer(
			FetchParentAccess parentAccess,
			NavigablePath fetchedNavigable,
			FetchStrategy mappedFetchedStrategy,
			LockMode lockMode,
			EntityPersister concreteDescriptor,
			DomainResultAssembler fkValueAssembler
	) {
		this.parentAccess = parentAccess;
		this.navigablePath = fetchedNavigable;
		this.mappedFetchedStrategy = mappedFetchedStrategy;
		this.lockMode = lockMode;
		this.concreteDescriptor = concreteDescriptor;
		this.fkValueAssembler = fkValueAssembler;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		fkValue = fkValueAssembler.assemble( rowProcessingState );

		// todo (6.0) : technically the entity could be managed or cached already.  who/what handles that?

		// todo (6.0) : could also be getting loaded elsewhere (LoadingEntityEntry)
		if ( fkValue == null ) {
			// todo (6.0) : check this is the correct behaviour
			entityInstance = null;
		}
		else {
			if ( mappedFetchedStrategy.getTiming() != FetchTiming.IMMEDIATE ) {
				if ( concreteDescriptor.hasProxy() ) {
					entityInstance = concreteDescriptor.createProxy(
							fkValue,
							rowProcessingState.getSession()
					);
				}
				else if ( concreteDescriptor
						.getBytecodeEnhancementMetadata()
						.isEnhancedForLazyLoading() ) {
					entityInstance = concreteDescriptor.instantiate(
							fkValue,
							rowProcessingState.getSession()
					);
				}
			}
			else {
				entityInstance = rowProcessingState.getSession().immediateLoad( concreteDescriptor.getEntityName(), fkValue );
			}

			notifyParentResolutionListeners( entityInstance );
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		fkValue = null;

		clearParentResolutionListeners();
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public Object getParentKey() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( entityInstance != null ) {
			listener.accept( entityInstance );
		}
		else {
			super.registerResolutionListener( listener );
		}
	}
}
