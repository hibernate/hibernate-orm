/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParentAccess;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Andrea Boriero
 */
public class SelectEntityInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	private final NavigablePath navigablePath;
	private final EntityPersister concreteDescriptor;
	private final DomainResultAssembler identifierAssembler;
	private final boolean unwrapProxy;
	private final boolean nullable;

	private Object entityInstance;

	protected SelectEntityInitializer(
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler,
			boolean unwrapProxy,
			boolean nullable) {
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
		this.unwrapProxy = unwrapProxy;
		this.nullable = nullable;
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

	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( entityInstance != null ) {
			return;
		}

		final Object id = identifierAssembler.assemble( rowProcessingState );
		if ( id == null ) {
			return;
		}

		final String entityName = concreteDescriptor.getEntityName();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();

		entityInstance = session.internalLoad(
				entityName,
				id,
				false,
				nullable
		);

		if ( entityInstance instanceof HibernateProxy ) {
			final boolean isProxyUnwrapEnabled = unwrapProxy && concreteDescriptor.isInstrumented();

			( (HibernateProxy) entityInstance ).getHibernateLazyInitializer().setUnwrap( isProxyUnwrapEnabled );
		}
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;

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
