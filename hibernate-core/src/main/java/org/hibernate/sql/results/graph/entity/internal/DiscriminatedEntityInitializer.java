/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Initializer for discriminated mappings.
 */
public class DiscriminatedEntityInitializer
		extends AbstractInitializer<DiscriminatedEntityInitializer.DiscriminatedEntityInitializerData>
		implements EntityInitializer<DiscriminatedEntityInitializer.DiscriminatedEntityInitializerData> {

	protected final InitializerParent<?> parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;

	private final DomainResultAssembler<?> discriminatorValueAssembler;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final DiscriminatedAssociationModelPart fetchedPart;
	private final boolean eager;
	private final boolean resultInitializer;
	private final boolean keyIsEager;
	private final boolean hasLazySubInitializer;

	public static class DiscriminatedEntityInitializerData extends InitializerData {
		protected EntityPersister concreteDescriptor;
		protected Object entityIdentifier;

		public DiscriminatedEntityInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}
	}

	public DiscriminatedEntityInitializer(
			InitializerParent<?> parent,
			DiscriminatedAssociationModelPart fetchedPart,
			NavigablePath fetchedNavigable,
			Fetch discriminatorFetch,
			Fetch keyFetch,
			boolean eager,
			boolean resultInitializer,
			AssemblerCreationState creationState) {
		super( creationState );
		this.parent = parent;
		this.fetchedPart = fetchedPart;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		this.discriminatorValueAssembler = discriminatorFetch.createAssembler( this, creationState );
		this.keyValueAssembler = keyFetch.createAssembler( this, creationState );
		this.eager = eager;
		this.resultInitializer = resultInitializer;
		final Initializer<?> initializer = keyValueAssembler.getInitializer();
		if ( initializer == null ) {
			this.keyIsEager = false;
			this.hasLazySubInitializer = false;
		}
		else {
			this.keyIsEager = initializer.isEager();
			this.hasLazySubInitializer = !initializer.isEager() || initializer.hasLazySubInitializers();
		}
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new DiscriminatedEntityInitializerData( rowProcessingState );
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	public ModelPart getInitializedPart(){
		return fetchedPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(DiscriminatedEntityInitializerData data) {
		if ( data.getState() != State.UNINITIALIZED ) {
			return;
		}

		// resolve the key and the discriminator, and then use those to load the indicated entity

		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState );

		if ( discriminatorValue == null ) {
			data.setState( State.MISSING );
			data.concreteDescriptor = null;
			data.entityIdentifier = null;
			data.setInstance( null );
			// null association
			assert keyValueAssembler.assemble( rowProcessingState ) == null;
		}
		else {
			data.setState( State.KEY_RESOLVED );
			data.concreteDescriptor = fetchedPart.resolveDiscriminatorValue( discriminatorValue ).getEntityPersister();
			data.entityIdentifier = keyValueAssembler.assemble( rowProcessingState );
		}
	}

	@Override
	public void resolveState(DiscriminatedEntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		discriminatorValueAssembler.resolveState( rowProcessingState );
		keyValueAssembler.resolveState( rowProcessingState );
	}

	@Override
	public void resolveFromPreviousRow(DiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final Initializer<?> initializer = keyValueAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(DiscriminatedEntityInitializerData data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}

		data.setState( State.INITIALIZED );

		final SharedSessionContractImplementor session = data.getRowProcessingState().getSession();
		final EntityKey entityKey = new EntityKey( data.entityIdentifier, data.concreteDescriptor );

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null ) {
			final Object instance = holder.getEntity();
			data.setInstance( instance );
			if ( holder.getEntityInitializer() == null ) {
				if ( instance != null && Hibernate.isInitialized( instance ) ) {
					return;
				}
			}
			else if ( holder.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				return;
			}
			else if ( instance == null ) {
				// todo: maybe mark this as resolved instead?
				assert holder.getProxy() == null : "How to handle this case?";
				return;
			}
		}

		data.setInstance( session.internalLoad(
				data.concreteDescriptor.getEntityName(),
				data.entityIdentifier,
				eager,
				// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
				false
		) );
	}

	@Override
	public void resolveInstance(Object instance, DiscriminatedEntityInitializerData data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
			data.setInstance( null );
		}
		else {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final LazyInitializer lazyInitializer = extractLazyInitializer( instance );
			if ( lazyInitializer == null ) {
				data.setState( State.INITIALIZED );
				if ( keyIsEager ) {
					final SharedSessionContractImplementor session = rowProcessingState.getSession();
					data.concreteDescriptor = session.getEntityPersister( null, instance );
					data.entityIdentifier = data.concreteDescriptor.getIdentifier( instance, session );
				}
			}
			else if ( lazyInitializer.isUninitialized() ) {
				data.setState( eager ? State.RESOLVED : State.INITIALIZED );
				if ( keyIsEager ) {
					// Read the discriminator from the result set if necessary
					final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState );
					data.concreteDescriptor = fetchedPart.resolveDiscriminatorValue( discriminatorValue ).getEntityPersister();
					data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				}
			}
			else {
				data.setState( State.INITIALIZED );
				if ( keyIsEager ) {
					data.concreteDescriptor = rowProcessingState.getSession().getEntityPersister( null, lazyInitializer.getImplementation() );
					data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				}
			}
			data.setInstance( instance );
			if ( keyIsEager ) {
				final Initializer<?> initializer = keyValueAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				discriminatorValueAssembler.resolveState( rowProcessingState );
				keyValueAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public void initializeInstance(DiscriminatedEntityInitializerData data) {
		if ( data.getState() != State.RESOLVED ) {
			return;
		}
		data.setState( State.INITIALIZED );
		data.setInstance( data.getRowProcessingState().getSession().internalLoad(
				data.concreteDescriptor.getEntityName(),
				data.entityIdentifier,
				eager,
				// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
				false
		) );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, DiscriminatedEntityInitializerData data) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
		}
		else {
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			// No need to initialize this
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
			if ( eager ) {
				Hibernate.initialize( instance );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final Initializer<?> initializer = keyValueAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		throw new UnsupportedOperationException("Discriminated association has no static entity type");
	}

	@Override
	public EntityPersister getConcreteDescriptor(DiscriminatedEntityInitializerData data) {
		return data.concreteDescriptor;
	}

	@Override
	public @Nullable Object getEntityIdentifier(DiscriminatedEntityInitializerData data) {
		return data.entityIdentifier;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		return eager || keyIsEager;
	}

	@Override
	public boolean hasLazySubInitializers() {
		return hasLazySubInitializer;
	}

	@Override
	public boolean isResultInitializer() {
		return resultInitializer;
	}

	@Override
	public String toString() {
		return "DiscriminatedEntityInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
