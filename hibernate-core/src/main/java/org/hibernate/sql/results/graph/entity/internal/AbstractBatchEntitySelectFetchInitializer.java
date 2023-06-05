/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

public abstract class AbstractBatchEntitySelectFetchInitializer extends AbstractFetchParentAccess
		implements EntityInitializer {

	protected final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> identifierAssembler;
	protected final ToOneAttributeMapping referencedModelPart;
	protected final EntityInitializer firstEntityInitializer;

	protected Object entityInstance;
	protected EntityKey entityKey;

	public AbstractBatchEntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		this.parentAccess = parentAccess;
		this.referencedModelPart = referencedModelPart;
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
		this.firstEntityInitializer = parentAccess.findFirstEntityInitializer();
		assert firstEntityInitializer != null : "This initializer requires parentAccess.findFirstEntityInitializer() to not be null";
	}

	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		final Object entityIdentifier = identifierAssembler.assemble( rowProcessingState );
		if ( entityIdentifier == null ) {
			return;
		}
		entityKey = new EntityKey( entityIdentifier, concreteDescriptor );

		rowProcessingState.getSession().getPersistenceContext()
				.getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );

		registerResolutionListener();
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
	}

	protected abstract void registerResolutionListener();

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		entityKey = null;
		clearResolutionListeners();
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
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public Object getParentKey() {
		return findFirstEntityInitializer().getEntityKey().getIdentifier();
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

	protected static Object loadInstance(
			EntityKey entityKey,
			ToOneAttributeMapping referencedModelPart,
			SharedSessionContractImplementor session) {
		return session.internalLoad(
				entityKey.getEntityName(),
				entityKey.getIdentifier(),
				true,
				referencedModelPart.isInternalLoadNullable()
		);
	}

	protected AttributeMapping getParentEntityAttribute(String attributeName) {
		final AttributeMapping parentAttribute = firstEntityInitializer.getConcreteDescriptor()
				.findAttributeMapping( attributeName );
		if ( parentAttribute != null && parentAttribute.getDeclaringType() == referencedModelPart.getDeclaringType()
				.findContainingEntityMapping() ) {
			// These checks are needed to avoid setting the instance using the wrong (child's) model part or
			// setting it multiple times in case parent and child share the same attribute name for the association.
			return parentAttribute;
		}
		return null;
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return parentAccess;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
	}

}
