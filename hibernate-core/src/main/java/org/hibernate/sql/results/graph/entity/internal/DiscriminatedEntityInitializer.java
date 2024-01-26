/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * Initializer for discriminated mappings.
 */
public class DiscriminatedEntityInitializer implements EntityInitializer {
	private static final String CONCRETE_NAME = DiscriminatedEntityInitializer.class.getSimpleName();

	protected final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private final FetchParentAccess owningParent;
	private final EntityMappingType ownedModelPartDeclaringType;
	private final boolean isPartOfKey;

	private final DomainResultAssembler<?> discriminatorValueAssembler;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final DiscriminatedAssociationModelPart fetchedPart;
	private final boolean eager;
	private final boolean resultInitializer;

	protected boolean parentShallowCached;

	// per-row state
	protected State state = State.UNINITIALIZED;
	protected EntityPersister concreteDescriptor;
	protected Object entityIdentifier;
	protected Object entityInstance;

	public DiscriminatedEntityInitializer(
			FetchParentAccess parentAccess,
			DiscriminatedAssociationModelPart fetchedPart,
			NavigablePath fetchedNavigable,
			Fetch discriminatorFetch,
			Fetch keyFetch,
			boolean eager,
			boolean resultInitializer,
			AssemblerCreationState creationState) {
		this.parentAccess = parentAccess;
		this.fetchedPart = fetchedPart;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parentAccess );
		this.owningParent = FetchParentAccess.determineOwningParent( parentAccess );
		this.ownedModelPartDeclaringType = FetchParentAccess.determineOwnedModelPartDeclaringType( fetchedPart, parentAccess, owningParent );
		this.discriminatorValueAssembler = discriminatorFetch.createAssembler( this, creationState );
		this.keyValueAssembler = keyFetch.createAssembler( this, creationState );
		this.eager = eager;
		this.resultInitializer = resultInitializer;
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return parentAccess;
	}

	@Override
	public @Nullable FetchParentAccess getOwningParent() {
		return owningParent;
	}

	@Override
	public @Nullable EntityMappingType getOwnedModelPartDeclaringType() {
		return ownedModelPartDeclaringType;
	}

	public ModelPart getInitializedPart(){
		return fetchedPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( state != State.UNINITIALIZED ) {
			return;
		}

		// resolve the key and the discriminator, and then use those to load the indicated entity

		final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState );

		if ( discriminatorValue == null ) {
			state = State.INITIALIZED;
			// null association
			assert keyValueAssembler.assemble( rowProcessingState ) == null;
		}
		else {
			state = State.KEY_RESOLVED;
			concreteDescriptor = fetchedPart.resolveDiscriminatorValue( discriminatorValue ).getEntityPersister();
			entityIdentifier = keyValueAssembler.assemble( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}
		state = State.RESOLVED;

		// We can avoid processing further if the parent is already initialized or missing,
		// as the value produced by this initializer will never be used anyway.
		if ( parentShallowCached || shouldSkipInitializer( rowProcessingState ) ) {
			state = State.INITIALIZED;
			return;
		}

		entityIdentifier = keyValueAssembler.assemble( rowProcessingState );

		if ( entityIdentifier == null ) {
			state = State.INITIALIZED;
			return;
		}

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityIdentifier
			);
		}
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final EntityKey entityKey = new EntityKey( entityIdentifier, concreteDescriptor );

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null ) {
			if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						CONCRETE_NAME,
						toLoggableString(
								getNavigablePath(),
								entityIdentifier
						)
				);
			}
			entityInstance = holder.getEntity();
			if ( holder.getEntityInitializer() == null ) {
				if ( entityInstance != null && Hibernate.isInitialized( entityInstance ) ) {
					state = State.INITIALIZED;
					return;
				}
			}
			else if ( holder.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
					EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							CONCRETE_NAME,
							toLoggableString( getNavigablePath(), entityIdentifier ),
							holder.getEntityInitializer()
					);
				}
				state = State.INITIALIZED;
				return;
			}
			else if ( entityInstance == null ) {
				state = State.INITIALIZED;
				return;
			}
		}

		// Defer the select by default to the initialize phase
		// We only need to select in this phase if this is part of an identifier or foreign key
		NavigablePath np = navigablePath.getParent();
		while ( np != null ) {
			if ( np instanceof EntityIdentifierNavigablePath
					|| ForeignKeyDescriptor.PART_NAME.equals( np.getLocalName() )
					|| ForeignKeyDescriptor.TARGET_PART_NAME.equals( np.getLocalName() )) {

				initializeInstance( rowProcessingState );
				return;
			}
			np = np.getParent();
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( state != State.RESOLVED ) {
			return;
		}

		state = State.INITIALIZED;

		entityInstance = rowProcessingState.getSession().internalLoad(
				concreteDescriptor.getEntityName(),
				entityIdentifier,
				eager,
				// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
				false
		);
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, RowProcessingState rowProcessingState) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		if ( eager ) {
			Hibernate.initialize( instance );
		}
		entityInstance = instance;
		state = State.INITIALIZED;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		entityIdentifier = null;
		concreteDescriptor = null;
		state = State.UNINITIALIZED;
	}

	@Override
	public void markShallowCached() {
		parentShallowCached = true;
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		parentShallowCached = false;
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
	public boolean isEntityInitialized() {
		return state == State.INITIALIZED;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isResultInitializer() {
		return resultInitializer;
	}

	@Override
	public String toString() {
		return "DiscriminatedEntityInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	protected enum State {
		UNINITIALIZED,
		KEY_RESOLVED,
		RESOLVED,
		INITIALIZED;
	}

	@Override
	public EntityKey getEntityKey() {
		throw new UnsupportedOperationException(
				"This should never happen, because this initializer has not child initializers" );
	}

	@Override
	public Object getParentKey() {
		throw new UnsupportedOperationException(
				"This should never happen, because this initializer has not child initializers" );
	}

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		throw new UnsupportedOperationException(
				"This should never happen, because this initializer has not child initializers" );
	}
}
