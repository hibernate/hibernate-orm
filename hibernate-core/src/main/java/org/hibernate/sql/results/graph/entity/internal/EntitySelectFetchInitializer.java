/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchInitializer<Data extends EntitySelectFetchInitializer.EntitySelectFetchInitializerData>
		extends AbstractInitializer<Data> implements EntityInitializer<Data> {
	protected final InitializerParent<?> parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;
	private final boolean isEnhancedForLazyLoading;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> keyAssembler;
	protected final ToOneAttributeMapping toOneMapping;
	protected final boolean affectedByFilter;
	protected final boolean keyIsEager;
	protected final boolean hasLazySubInitializer;

	public static class EntitySelectFetchInitializerData extends InitializerData {
		// per-row state
		protected @Nullable Object entityIdentifier;

		public EntitySelectFetchInitializerData(EntitySelectFetchInitializer<?> initializer, RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}

		/*
		 * Used by Hibernate Reactive
		 */
		public EntitySelectFetchInitializerData(EntitySelectFetchInitializerData original) {
			super( original );
			this.entityIdentifier = original.entityIdentifier;
		}
	}

	public EntitySelectFetchInitializer(
			InitializerParent<?> parent,
			ToOneAttributeMapping toOneMapping,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResult<?> keyResult,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		super( creationState );
		this.parent = parent;
		this.toOneMapping = toOneMapping;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		this.concreteDescriptor = concreteDescriptor;
		this.keyAssembler = keyResult.createResultAssembler( this, creationState );
		this.isEnhancedForLazyLoading = concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
		this.affectedByFilter = affectedByFilter;
		final Initializer<?> initializer = keyAssembler.getInitializer();
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
		return new EntitySelectFetchInitializerData( this, rowProcessingState );
	}

	public ModelPart getInitializedPart(){
		return toOneMapping;
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveFromPreviousRow(Data data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final Initializer<?> initializer = keyAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(Data data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}

		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		data.entityIdentifier = keyAssembler.assemble( rowProcessingState );

		if ( data.entityIdentifier == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
			return;
		}
		data.setState( State.INITIALIZED );
		initialize( data );
	}

	@Override
	public void resolveInstance(Object instance, Data data) {
		if ( instance == null ) {
			data.setState(  State.MISSING );
			data.entityIdentifier = null;
			data.setInstance( null );
		}
		else {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final LazyInitializer lazyInitializer = extractLazyInitializer( data.getInstance() );
			if ( lazyInitializer == null ) {
				data.setState( State.INITIALIZED );
				if ( keyIsEager ) {
					data.entityIdentifier = concreteDescriptor.getIdentifier( instance, rowProcessingState.getSession() );
				}
			}
			else if ( lazyInitializer.isUninitialized() ) {
				data.setState( State.RESOLVED );
				if ( keyIsEager ) {
					data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				}
			}
			else {
				data.setState( State.INITIALIZED );
				if ( keyIsEager ) {
					data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				}
			}
			data.setInstance( instance );
			if ( keyIsEager ) {
				final Initializer<?> initializer = keyAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				keyAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public void initializeInstance(Data data) {
		if ( data.getState() != State.RESOLVED ) {
			return;
		}
		data.setState( State.INITIALIZED );
		Hibernate.initialize( data.getInstance() );
	}

	protected void initialize(EntitySelectFetchInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final EntityKey entityKey = new EntityKey( data.entityIdentifier, concreteDescriptor );

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null ) {
			data.setInstance( persistenceContext.proxyFor( holder, concreteDescriptor ) );
			if ( holder.getEntityInitializer() == null ) {
				if ( data.getInstance() != null && Hibernate.isInitialized( data.getInstance() ) ) {
					data.setState( State.INITIALIZED );
					return;
				}
			}
			else if ( holder.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				data.setState( State.INITIALIZED );
				return;
			}
			else if ( data.getInstance() == null ) {
				// todo: maybe mark this as resolved instead?
				assert holder.getProxy() == null : "How to handle this case?";
				data.setState( State.INITIALIZED );
				return;
			}
		}
		data.setState( State.INITIALIZED );
		final String entityName = concreteDescriptor.getEntityName();

		final Object instance = session.internalLoad(
				entityName,
				data.entityIdentifier,
				true,
				toOneMapping.isInternalLoadNullable()
		);
		data.setInstance( instance );

		if ( instance == null ) {
			if ( toOneMapping.getNotFoundAction() != NotFoundAction.IGNORE ) {
				if ( affectedByFilter ) {
					throw new EntityFilterException(
							entityName,
							data.entityIdentifier,
							toOneMapping.getNavigableRole().getFullPath()
					);
				}
				if ( toOneMapping.getNotFoundAction() == NotFoundAction.EXCEPTION ) {
					throw new FetchNotFoundException( entityName, data.entityIdentifier );
				}
			}
			persistenceContext.claimEntityHolderIfPossible(
					new EntityKey( data.entityIdentifier, concreteDescriptor ),
					instance,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			);
		}

		final boolean unwrapProxy = toOneMapping.isUnwrapProxy() && isEnhancedForLazyLoading;
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( data.getInstance() );
		if ( lazyInitializer != null ) {
			lazyInitializer.setUnwrap( unwrapProxy );
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, Data data) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityIdentifier = null;
			data.setInstance( null );
		}
		else {
			data.setState( State.INITIALIZED );
			// No need to initialize this
			data.entityIdentifier = null;
			data.setInstance( instance );
			Hibernate.initialize( instance );
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final Initializer<?> initializer = keyAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public EntityPersister getConcreteDescriptor(Data data) {
		return concreteDescriptor;
	}

	@Override
	public @Nullable Object getEntityIdentifier(Data data) {
		return data.entityIdentifier;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		return true;
	}

	@Override
	public void resolveState(EntitySelectFetchInitializerData data) {
		keyAssembler.resolveState( data.getRowProcessingState() );
	}

	@Override
	public boolean hasLazySubInitializers() {
		return hasLazySubInitializer;
	}

	@Override
	public boolean isResultInitializer() {
		return false;
	}

	@Override
	public String toString() {
		return "EntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	public DomainResultAssembler<?> getKeyAssembler() {
		return keyAssembler;
	}
}
