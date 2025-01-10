/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.FetchNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.internal.ManagedTypeHelper;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.internal.log.LoggingHelper;
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
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl.determineConcreteEntityDescriptor;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityDelayedFetchInitializer
		extends AbstractInitializer<EntityDelayedFetchInitializer.EntityDelayedFetchInitializerData>
		implements EntityInitializer<EntityDelayedFetchInitializer.EntityDelayedFetchInitializerData> {

	private final InitializerParent<?> parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;
	private final ToOneAttributeMapping referencedModelPart;
	private final boolean selectByUniqueKey;
	private final DomainResultAssembler<?> identifierAssembler;
	private final @Nullable BasicResultAssembler<?> discriminatorAssembler;
	private final boolean keyIsEager;
	private final boolean hasLazySubInitializer;

	public static class EntityDelayedFetchInitializerData extends InitializerData {
		// per-row state
		protected @Nullable Object entityIdentifier;

		public EntityDelayedFetchInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}
	}

	public EntityDelayedFetchInitializer(
			InitializerParent<?> parent,
			NavigablePath fetchedNavigable,
			ToOneAttributeMapping referencedModelPart,
			boolean selectByUniqueKey,
			DomainResult<?> keyResult,
			@Nullable BasicFetch<?> discriminatorResult,
			AssemblerCreationState creationState) {
		super( creationState );
		// associations marked with `@NotFound` are ALWAYS eagerly fetched, unless we're resolving the concrete type
		assert !referencedModelPart.hasNotFoundAction() || referencedModelPart.getEntityMappingType().isConcreteProxy();

		this.parent = parent;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		this.referencedModelPart = referencedModelPart;
		this.selectByUniqueKey = selectByUniqueKey;
		this.identifierAssembler = keyResult.createResultAssembler( this, creationState );
		this.discriminatorAssembler = discriminatorResult == null
				? null
				: (BasicResultAssembler<?>) discriminatorResult.createResultAssembler( this, creationState );
		final Initializer<?> initializer;
		if ( identifierAssembler == null || ( initializer = identifierAssembler.getInitializer() ) == null ) {
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
		return new EntityDelayedFetchInitializerData( rowProcessingState );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public void resolveFromPreviousRow(EntityDelayedFetchInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final Initializer<?> initializer = identifierAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(EntityDelayedFetchInitializerData data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}

		// This initializer is done initializing, since this is only invoked for delayed or select initializers
		data.setState( State.INITIALIZED );

		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		data.entityIdentifier = identifierAssembler.assemble( rowProcessingState );

		if ( data.entityIdentifier == null ) {
			data.setInstance( null );
			data.setState( State.MISSING );
		}
		else {
			final SharedSessionContractImplementor session = rowProcessingState.getSession();

			final EntityPersister entityPersister = getEntityDescriptor();
			final EntityPersister concreteDescriptor;
			if ( discriminatorAssembler != null ) {
				concreteDescriptor = determineConcreteEntityDescriptor(
						rowProcessingState,
						discriminatorAssembler,
						entityPersister
				);
				if ( concreteDescriptor == null ) {
					// If we find no discriminator it means there's no entity in the target table
					if ( !referencedModelPart.isOptional() ) {
						throw new FetchNotFoundException( entityPersister.getEntityName(), data.entityIdentifier );
					}
					data.setInstance( null );
					data.setState( State.MISSING );
					return;
				}
			}
			else {
				concreteDescriptor = entityPersister;
			}

			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			if ( selectByUniqueKey ) {
				final String uniqueKeyPropertyName = referencedModelPart.getReferencedPropertyName();
				final Type uniqueKeyPropertyType = ( referencedModelPart.getReferencedPropertyName() == null ) ?
						concreteDescriptor.getIdentifierType() :
						session.getFactory()
								.getReferencedPropertyType(
										concreteDescriptor.getEntityName(),
										uniqueKeyPropertyName
								);

				final EntityUniqueKey euk = new EntityUniqueKey(
						concreteDescriptor.getEntityName(),
						uniqueKeyPropertyName,
						data.entityIdentifier,
						uniqueKeyPropertyType,
						session.getFactory()
				);
				Object instance = persistenceContext.getEntity( euk );
				if ( instance == null ) {
					// For unique-key mappings, we always use bytecode-laziness if possible,
					// because we can't generate a proxy based on the unique key yet
					if ( referencedModelPart.isLazy() ) {
						instance = UNFETCHED_PROPERTY;
					}
					else {
						// Try to load a PersistentAttributeInterceptable. If we get one, we can add the lazy
						// field to the interceptor. If we don't get one, we load the entity by unique key.
						PersistentAttributeInterceptable persistentAttributeInterceptable = null;
						if ( getParent().isEntityInitializer() && isLazyByGraph( rowProcessingState ) ) {
							final Object resolvedInstance =
									getParent().asEntityInitializer().getResolvedInstance( rowProcessingState );
							persistentAttributeInterceptable =
									ManagedTypeHelper.asPersistentAttributeInterceptableOrNull( resolvedInstance );
						}

						if ( persistentAttributeInterceptable != null ) {
							final LazyAttributeLoadingInterceptor persistentAttributeInterceptor = (LazyAttributeLoadingInterceptor) persistentAttributeInterceptable.$$_hibernate_getInterceptor();
							persistentAttributeInterceptor.addLazyFieldByGraph( navigablePath.getLocalName() );
							instance = UNFETCHED_PROPERTY;
						}
						else {
							instance = concreteDescriptor.loadByUniqueKey(
									uniqueKeyPropertyName,
									data.entityIdentifier,
									session
							);

							// If the entity was not in the Persistence Context, but was found now,
							// add it to the Persistence Context
							if ( instance != null ) {
								persistenceContext.addEntity( euk, instance );
							}
						}
					}
				}
				if ( instance != null ) {
					instance = persistenceContext.proxyFor( instance );
				}
				data.setInstance( instance );
			}
			else {
				final EntityKey entityKey = new EntityKey( data.entityIdentifier, concreteDescriptor );
				final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
				final Object instance;
				if ( holder != null && holder.getEntity() != null ) {
					instance = persistenceContext.proxyFor( holder, concreteDescriptor );
				}
				// For primary key based mappings we only use bytecode-laziness if the attribute is optional,
				// because the non-optionality implies that it is safe to have a proxy
				else if ( referencedModelPart.isOptional() && referencedModelPart.isLazy() ) {
					instance = UNFETCHED_PROPERTY;
				}
				else {
					instance = session.internalLoad(
							concreteDescriptor.getEntityName(),
							data.entityIdentifier,
							false,
							false
					);

					final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( instance );
					if ( lazyInitializer != null ) {
						lazyInitializer.setUnwrap( referencedModelPart.isUnwrapProxy() && concreteDescriptor.isInstrumented() );
					}
				}
				data.setInstance( instance );
			}
		}
	}

	private boolean isLazyByGraph(RowProcessingState rowProcessingState) {
		final AppliedGraph appliedGraph = rowProcessingState.getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() == GraphSemantic.FETCH ) {
			final AttributeNodeImplementor<Object> attributeNode = appliedGraph.getGraph()
					.findAttributeNode( navigablePath.getLocalName() );
			if ( attributeNode != null && attributeNode.getAttributeDescriptor() == getInitializedPart().asAttributeMapping() ) {
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public void resolveInstance(Object instance, EntityDelayedFetchInitializerData data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityIdentifier = null;
			data.setInstance( null );
		}
		else {
			// This initializer is done initializing, since this is only invoked for delayed or select initializers
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			if ( keyIsEager ) {
				data.entityIdentifier = getEntityDescriptor().getIdentifier( instance, rowProcessingState.getSession() );
				final Initializer<?> initializer = identifierAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				identifierAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final Initializer<?> initializer = identifierAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return referencedModelPart.getEntityMappingType().getEntityPersister();
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		return keyIsEager;
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
	public EntityPersister getConcreteDescriptor(EntityDelayedFetchInitializerData data) {
		return getEntityDescriptor();
	}

	@Override
	public void resolveState(EntityDelayedFetchInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		identifierAssembler.resolveState( rowProcessingState );
		if ( discriminatorAssembler != null ) {
			discriminatorAssembler.resolveState( rowProcessingState );
		}
	}

	@Override
	public @Nullable Object getEntityIdentifier(EntityDelayedFetchInitializerData data) {
		return data.entityIdentifier;
	}

	@Override
	public String toString() {
		return "EntityDelayedFetchInitializer(" + LoggingHelper.toLoggableString( navigablePath ) + ")";
	}

	//#########################
	// For Hibernate Reactive
	//#########################

	protected boolean isSelectByUniqueKey() {
		return selectByUniqueKey;
	}

	protected DomainResultAssembler<?> getIdentifierAssembler() {
		return identifierAssembler;
	}

	protected @Nullable BasicResultAssembler<?> getDiscriminatorAssembler() {
		return discriminatorAssembler;
	}
}
