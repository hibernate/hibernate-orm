/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityFetchDelayedInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	private final NavigablePath navigablePath;
	private final EntityPersister concreteDescriptor;
	private final DomainResultAssembler identifierAssembler;

	private Object entityInstance;
	private Object identifier;

	protected EntityFetchDelayedInitializer(
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler) {
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
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
		if ( entityInstance != null ) {
			return;
		}
		identifier = identifierAssembler.assemble( rowProcessingState );

		// todo (6.0) : technically the entity could be managed or cached already.  who/what handles that?

		// todo (6.0) : could also be getting loaded elsewhere (LoadingEntityEntry)
		if ( identifier == null ) {
			// todo (6.0) : check this is the correct behaviour
			entityInstance = null;
		}
		else {
			if ( concreteDescriptor.hasProxy() ) {
				entityInstance = concreteDescriptor.createProxy(
						identifier,
						rowProcessingState.getSession()
				);
			}
			else if ( concreteDescriptor
					.getBytecodeEnhancementMetadata()
					.isEnhancedForLazyLoading() ) {
				entityInstance = concreteDescriptor.instantiate(
						identifier,
						rowProcessingState.getSession()
				);
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
		identifier = null;

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
