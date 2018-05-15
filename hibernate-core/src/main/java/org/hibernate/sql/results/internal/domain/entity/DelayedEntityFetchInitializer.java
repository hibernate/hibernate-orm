/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AbstractFetchParentAccess;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * The initializer created from {@link DelayedEntityFetch}
 *
 * @author Steve Ebersole
 */
public class DelayedEntityFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	// todo (6.0) : what (if anything) do we need to do with `FetchParentAccess`?

	private final EntityValuedNavigable fetchedNavigable;
	private final FetchParentAccess parentAccess;
	private final DomainResultAssembler fkValueAssembler;

	private final NavigablePath path;

	// per-row state
	private Object entityInstance;
	private Object fkValue;

	protected DelayedEntityFetchInitializer(
			EntityValuedNavigable fetchedNavigable,
			FetchParentAccess parentAccess,
			DomainResultAssembler fkValueAssembler) {
		this.fetchedNavigable = fetchedNavigable;
		this.parentAccess = parentAccess;
		this.fkValueAssembler = fkValueAssembler;
		this.path = parentAccess.getNavigablePath().append( fetchedNavigable.getNavigableName() );
	}

	@Override
	public EntityTypeDescriptor getEntityDescriptor() {
		return fetchedNavigable.getEntityDescriptor();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return path;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
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
			if ( fetchedNavigable.getEntityDescriptor().hasProxy() ) {

				entityInstance = fetchedNavigable.getEntityDescriptor().createProxy(
						fkValue,
						rowProcessingState.getSession()
				);
			}
			else if ( fetchedNavigable.getEntityDescriptor()
					.getBytecodeEnhancementMetadata()
					.isEnhancedForLazyLoading() ) {
				entityInstance = fetchedNavigable.getEntityDescriptor().instantiate(
						fkValue,
						rowProcessingState.getSession()
				);
			}

			notifyParentResolutionListeners( entityInstance );
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		// nothing to initialize (its lazy)
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		fkValue = null;
		entityInstance = null;

		clearParentResolutionListeners();
	}

}
