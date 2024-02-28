/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchInitializer implements EntityInitializer {
	private static final String CONCRETE_NAME = EntitySelectFetchInitializer.class.getSimpleName();

	protected final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private final FetchParentAccess owningParent;
	private final EntityMappingType ownedModelPartDeclaringType;
	private final boolean isPartOfKey;
	private final boolean isEnhancedForLazyLoading;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> keyAssembler;
	private final ToOneAttributeMapping toOneMapping;

	protected boolean parentShallowCached;

	// per-row state
	protected State state = State.UNINITIALIZED;
	protected Object entityIdentifier;
	protected Object entityInstance;

	public EntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping toOneMapping,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> keyAssembler) {
		this.parentAccess = parentAccess;
		this.toOneMapping = toOneMapping;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parentAccess );
		this.owningParent = FetchParentAccess.determineOwningParent( parentAccess );
		this.ownedModelPartDeclaringType = FetchParentAccess.determineOwnedModelPartDeclaringType( toOneMapping, parentAccess, owningParent );
		this.concreteDescriptor = concreteDescriptor;
		this.keyAssembler = keyAssembler;
		this.isEnhancedForLazyLoading = concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	public ModelPart getInitializedPart(){
		return toOneMapping;
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
		if ( state != State.UNINITIALIZED ) {
			return;
		}
		state = State.RESOLVED;

		// We can avoid processing further if the parent is already initialized or missing,
		// as the value produced by this initializer will never be used anyway.
		if ( parentShallowCached || shouldSkipInitializer( rowProcessingState ) ) {
			state = State.INITIALIZED;
			return;
		}

		entityIdentifier = keyAssembler.assemble( rowProcessingState );

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
			entityInstance = persistenceContext.proxyFor( holder, concreteDescriptor );
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
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final String entityName = concreteDescriptor.getEntityName();

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Invoking session#internalLoad for entity (%s) : %s",
					CONCRETE_NAME,
					toLoggableString( getNavigablePath(), entityIdentifier ),
					entityIdentifier
			);
		}
		entityInstance = session.internalLoad(
				entityName,
				entityIdentifier,
				true,
				toOneMapping.isInternalLoadNullable()
		);

		if ( entityInstance == null ) {
			if ( toOneMapping.getNotFoundAction() == NotFoundAction.EXCEPTION ) {
				throw new FetchNotFoundException( entityName, entityIdentifier );
			}
			rowProcessingState.getSession().getPersistenceContextInternal().claimEntityHolderIfPossible(
					new EntityKey( entityIdentifier, concreteDescriptor ),
					entityInstance,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			);
		}

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Entity [%s] : %s has being loaded by session.internalLoad.",
					CONCRETE_NAME,
					toLoggableString( getNavigablePath(), entityIdentifier ),
					entityIdentifier
			);
		}

		final boolean unwrapProxy = toOneMapping.isUnwrapProxy() && isEnhancedForLazyLoading;
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entityInstance );
		if ( lazyInitializer != null ) {
			lazyInitializer.setUnwrap( unwrapProxy );
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, RowProcessingState rowProcessingState) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		Hibernate.initialize( instance );
		entityInstance = instance;
		state = State.INITIALIZED;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
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
		return false;
	}

	@Override
	public String toString() {
		return "EntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	protected enum State {
		UNINITIALIZED,
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
