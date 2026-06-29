/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.persister.entity.EntityPersister;


/**
 * The action for performing entity insertions when entity is using {@code IDENTITY} column identifier generation
 *
 * @see EntityInsertAction
 */
public class EntityIdentityInsertAction extends AbstractEntityInsertAction  {

	private final boolean isDelayed;
	private final @Nullable EntityKey delayedEntityKey;
	private @Nullable EntityKey entityKey;
	private @Nullable Object generatedId;
	private @Nullable Object rowId;

	/**
	 * Constructs an EntityIdentityInsertAction
	 *
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param persister The entity persister
	 * @param session The session
	 * @param isDelayed Are we in a situation which allows the insertion to be delayed?
	 */
	public EntityIdentityInsertAction(
			final @Nonnull Object[] state,
			final @Nonnull Object instance,
			final @Nonnull EntityPersister persister,
			final @Nonnull EventSource session,
			final boolean isDelayed) {
		this( state, instance, persister, session, isDelayed, isDelayed );
	}

	private EntityIdentityInsertAction(
			final @Nonnull Object[] state,
			final @Nonnull Object instance,
			final @Nonnull EntityPersister persister,
			final @Nonnull EventSource session,
			final boolean isDelayed,
			final boolean useDelayedIdentifier) {
		super(
				useDelayedIdentifier ? generateDelayedPostInsertIdentifier() : null,
				state,
				instance,
				persister,
				session
		);
		this.isDelayed = isDelayed;
		this.delayedEntityKey = useDelayedIdentifier ? generateDelayedEntityKey() : null;
	}

	@Nonnull
	public static EntityIdentityInsertAction delayedCopy(@Nonnull EntityIdentityInsertAction action) {
		return new EntityIdentityInsertAction(
				action.getState(),
				action.getInstance(),
				action.getPersister(),
				action.getSession(),
				true,
				true
		);
	}

	@Override
	public void execute() {
		nullifyTransientReferencesIfNotAlready();

		final var persister = getPersister();
		final var session = getSession();
		final Object instance = getInstance();

		setVeto( preInsert() );

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !isVeto() ) {
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginEntityInsertEvent();
			boolean success = false;
			final Object[] state = getState();
			final GeneratedValues generatedValues;
			try {
				generatedValues = persister.getInsertCoordinator().insert( instance, state, session );
				generatedId =
						generatedValues == null
								? null
								: generatedValues.getGeneratedValue( persister.getIdentifierMapping() );
				success = true;
			}
			catch (ConstraintViolationException cve) {
				throw convertException( cve, session );
			}
			finally {
				eventMonitor.completeEntityInsertEvent( event, generatedId, persister.getEntityName(), success, session );
			}
			final var persistenceContext = session.getPersistenceContextInternal();
			if ( persister.getRowIdMapping() != null ) {
				rowId = generatedValues.getGeneratedValue( persister.getRowIdMapping() );
				if ( rowId != null && isDelayed ) {
					persistenceContext.replaceEntityEntryRowId( getInstance(), rowId );
				}
			}
			if ( generatedId == null && generatedValues != null ) {
				final Object compositeId =
						compositeGeneratedId( persister, instance, generatedValues, session );
				if ( compositeId != null ) {
					generatedId = compositeId;
				}
			}
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( generatedId, instance, state, generatedValues, session );
			}
			if ( generatedId == null ) {
				generatedId = persister.getIdentifier( instance, session );
			}
			assert generatedId != null;
			//need to do that here rather than in the save event listener to let
			//the post insert events to have an id-filled entity when IDENTITY is used (EJB3)
			persister.setIdentifier( instance, generatedId, session );
			persistenceContext.registerInsertedKey( persister, generatedId );
			entityKey = session.generateEntityKey( generatedId, persister );
			persistenceContext.checkUniqueness( entityKey, getInstance() );
		}

		//TODO: this bit actually has to be called after all cascades!
		//      but since identity insert is called *synchronously*,
		//      instead of asynchronously as other actions, it isn't
		/*if ( persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			cacheEntry = new CacheEntry(object, persister, session);
			persister.getCache().insert(generatedId, cacheEntry);
		}*/

		postInsert();

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !isVeto() ) {
			statistics.insertEntity( persister.getEntityName() );
		}

		markExecuted();
	}

	@Nonnull
	private static PersistenceException convertException(
			@Nonnull ConstraintViolationException cve,
			@Nonnull EventSource session) {
		return session.getFactory().getSessionFactoryOptions().isJpaBootstrap()
			&& cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE
				? new EntityExistsException( cve )
				: cve;
	}

	@Override
	public boolean needsAfterTransactionCompletion() {
		//TODO: simply remove this override if we fix the above todos
		return hasPostCommitEventListeners();
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final var group = getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT;
		for ( var listener : group.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, @Nonnull SharedSessionContractImplementor session) {
		//TODO: re-enable if we also fix the above todo
		/*EntityPersister persister = getEntityPersister();
		if ( success && persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			persister.getCache().afterInsert( getGeneratedId(), cacheEntry );
		}*/
		postCommitInsert( success );
	}

	public void postInsert() {
		final var persistenceContext = getSession().getPersistenceContextInternal();
		if ( delayedEntityKey != null && persistenceContext.containsEntity( delayedEntityKey ) ) {
			persistenceContext.replaceDelayedEntityIdentityInsertKeys( delayedEntityKey, generatedId );
		}
		getEventListenerGroups().eventListenerGroup_POST_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, PostInsertEventListener::onPostInsert );
	}

	@Nullable
	private static Object compositeGeneratedId(
			@Nonnull EntityPersister persister,
			@Nonnull Object entity,
			@Nonnull GeneratedValues generatedValues,
			@Nonnull SharedSessionContractImplementor session) {
		if ( persister.getIdentifierMapping() instanceof CompositeIdentifierMapping compositeIdentifier ) {
			final var idMapping = compositeIdentifier.getMappedIdEmbeddableTypeDescriptor();
			final var generatedMapping = compositeIdentifier.getEmbeddableTypeDescriptor();
			final Object currentId = persister.getIdentifier( entity, session );
			if ( currentId == null ) {
				final var values = defaultedPrimitiveIds( generatedMapping, idMapping );
				if ( unpackGeneratedValues( generatedValues, generatedMapping, values ) ) {
					return idMapping.getRepresentationStrategy()
							.getInstantiator().instantiate( () -> values );
				}
				else {
					return null;
				}
			}
			else {
				final var values = idMapping.getValues( currentId );
				if ( unpackGeneratedValues( generatedValues, generatedMapping, values ) ) {
					idMapping.setValues( currentId, values );
					return currentId;
				}
				else {
					return null;
				}
			}
		}
		else {
			return null;
		}
	}

	/**
	 * To instantiate a composite id class with primitive fields,
	 * via an {@link EmbeddableInstantiator}, we need to assign
	 * their Java default values.
	 */
	@Nonnull
	public static Object[] defaultedPrimitiveIds(
			@Nonnull EmbeddableMappingType generatedMapping,
			@Nonnull EmbeddableMappingType idMapping) {
		final int attributeCount = generatedMapping.getNumberOfAttributeMappings();
		final var values = new Object[attributeCount];
		for ( int i = 0; i < attributeCount; i++ ) {
			final var attribute = idMapping.getAttributeMapping( i );
			if ( attribute.getPropertyAccess().getGetter().getReturnTypeClass().isPrimitive() ) {
				values[i] = attribute.getJavaType().getDefaultValue();
			}
		}
		return values;
	}

	private static boolean unpackGeneratedValues(
			@Nonnull GeneratedValues generatedValues,
			@Nonnull EmbeddableMappingType generatedMapping,
			@Nonnull Object[] values) {
		final int attributeCount =
				generatedMapping.getNumberOfAttributeMappings();
		boolean updated = false;
		for ( int i = 0; i < attributeCount; i++ ) {
			final var basicPart =
					generatedMapping.getAttributeMapping( i )
							.asBasicValuedModelPart();
			if ( basicPart != null ) {
				final Object generatedValue =
						generatedValues.getGeneratedValue( basicPart );
				if ( generatedValue != null ) {
					values[i] = generatedValue;
					updated = true;
				}
			}
		}
		return updated;
	}

	@Nonnull
	PostInsertEvent newPostInsertEvent() {
		// generatedId can be null if the insert was vetoed
		return new PostInsertEvent( getInstance(), generatedId, getState(), getPersister(), eventSource() );
	}

	protected void postCommitInsert(boolean success) {
		getEventListenerGroups().eventListenerGroup_POST_COMMIT_INSERT
			.fireLazyEventOnEachListener( this::newPostInsertEvent,
					success ? PostInsertEventListener::onPostInsert : this::postCommitInsertOnFailure );
	}

	private void postCommitInsertOnFailure(@Nonnull PostInsertEventListener listener, @Nonnull PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener postCommitInsertEventListener ) {
			postCommitInsertEventListener.onPostInsertCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostInsert( event );
		}
	}

	protected boolean preInsert() {
		executePreInsertCallbacks( eventSource() );

		final var listenerGroup = getEventListenerGroups().eventListenerGroup_PRE_INSERT;
		if ( listenerGroup.isEmpty() ) {
			// NO_VETO
			return false;
		}
		else {
			final var event = new PreInsertEvent( getInstance(), null, getState(), getPersister(), eventSource() );
			boolean veto = false;
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreInsert( event );
			}
			return veto;
		}
	}

	/**
	 * Access to the generated identifier
	 *
	 * @return The generated identifier
	 */
	@Nullable
	public final Object getGeneratedId() {
		return generatedId;
	}

	public void setGeneratedId(@Nullable Object generatedId) {
		this.generatedId = generatedId;
	}

	@Override
	public boolean isEarlyInsert() {
		return !isDelayed;
	}

	@Override
	@Nonnull
	protected EntityKey getEntityKey() {
		final var entityKey = this.entityKey == null ? delayedEntityKey : this.entityKey;
		assert entityKey != null;
		return entityKey;
	}

	@Override
	@Nullable
	public Object getRowId() {
		return rowId;
	}

	public void setRowId(@Nullable Object rowId) {
		this.rowId = rowId;
	}

	public void setEntityKey(@Nonnull EntityKey entityKey) {
		this.entityKey = entityKey;
	}

	@Nonnull
	private static DelayedPostInsertIdentifier generateDelayedPostInsertIdentifier() {
		return new DelayedPostInsertIdentifier();
	}

	@Nonnull
	protected EntityKey generateDelayedEntityKey() {
		return getSession().generateEntityKey( getDelayedId(), getPersister() );
	}
}
