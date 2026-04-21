/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.internal.UnifiedAnyDiscriminatorConverter;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Join fetching of {@link org.hibernate.annotations.Any} mappings.
 *
 * @author Gavin King
 */
public class JoinedDiscriminatedEntityInitializer
		extends AbstractInitializer<JoinedDiscriminatedEntityInitializer.JoinedDiscriminatedEntityInitializerData>
		implements EntityInitializer<JoinedDiscriminatedEntityInitializer.JoinedDiscriminatedEntityInitializerData> {

	protected final InitializerParent<?> parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;
	private final DiscriminatedAssociationModelPart fetchedPart;
	private final boolean eager;
	private final boolean resultInitializer;
	private final DomainResultAssembler<?> discriminatorAssembler;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final Map<String, EntityInitializer<?>> concreteInitializersByEntityName;
	private final EntityInitializer<?>[] concreteInitializers;
	private final boolean keyIsEager;
	private final boolean hasLazySubInitializer;
	// workaround for the fact that implicit discriminator mappings are not available
	private final boolean allowMissingConcreteInitializer;

	public static class JoinedDiscriminatedEntityInitializerData extends InitializerData {
		protected EntityPersister concreteDescriptor;
		protected Object entityIdentifier;
		protected EntityInitializer<?> concreteInitializer;
		protected boolean initializeExistingProxy;

		public JoinedDiscriminatedEntityInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}
	}

	public JoinedDiscriminatedEntityInitializer(
			InitializerParent<?> parent,
			DiscriminatedAssociationModelPart fetchedPart,
			NavigablePath fetchedNavigable,
			Fetch discriminatorFetch,
			Fetch keyFetch,
			boolean eager,
			boolean resultInitializer,
			List<EntityResultImpl<?>> concreteEntityResults,
			AssemblerCreationState creationState) {
		super( creationState );
		this.parent = parent;
		this.fetchedPart = fetchedPart;
		this.navigablePath = fetchedNavigable;
		this.eager = eager;
		this.resultInitializer = resultInitializer;

		isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		discriminatorAssembler = discriminatorFetch.createAssembler( this, creationState );
		keyValueAssembler = keyFetch.createAssembler( this, creationState );
		keyIsEager = keyValueAssembler.isEager();

		final Map<String, EntityInitializer<?>> initializersByEntityName =
				new LinkedHashMap<>( concreteEntityResults.size() );
		final var initializers = new EntityInitializer<?>[concreteEntityResults.size()];
		boolean lazySubInitializer = keyValueAssembler.hasLazySubInitializers();
		for ( int i = 0; i < concreteEntityResults.size(); i++ ) {
			final var entityResult = concreteEntityResults.get( i );
			final var initializer = resolveEntityInitializer( entityResult, creationState );
			initializers[i] = initializer;
			initializersByEntityName.put(
					entityResult.getEntityValuedModelPart().getEntityMappingType().getEntityName(),
					initializer
			);
			lazySubInitializer = lazySubInitializer || initializer.hasLazySubInitializers();
		}
		concreteInitializers = initializers;
		concreteInitializersByEntityName = initializersByEntityName;
		hasLazySubInitializer = lazySubInitializer;
		allowMissingConcreteInitializer =
				hasImplicitDiscriminatorStrategy( fetchedPart.getDiscriminatorMapping() );
	}

	private EntityInitializer<?> resolveEntityInitializer(
			EntityResultImpl<?> entityResult,
			AssemblerCreationState creationState) {
		return creationState.resolveInitializer(
				entityResult.getNavigablePath(),
				entityResult.getReferencedModePart(),
				() -> new EntityInitializerImpl(
						entityResult,
						entityResult.getSourceAlias(),
						entityResult.getIdentifierFetch(),
						entityResult.getDiscriminatorFetch(),
						null,
						entityResult.getRowIdResult(),
						NotFoundAction.EXCEPTION,
						false,
						this,
						false,
						creationState
				)
		).asEntityInitializer();
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new JoinedDiscriminatedEntityInitializerData( rowProcessingState );
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public DiscriminatedAssociationModelPart getInitializedPart() {
		return fetchedPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			final var rowProcessingState = data.getRowProcessingState();
			final Object discriminatorValue = discriminatorAssembler.assemble( rowProcessingState );
			data.initializeExistingProxy = false;
			if ( discriminatorValue == null ) {
				data.setState( State.MISSING );
				data.concreteDescriptor = null;
				data.entityIdentifier = null;
				data.concreteInitializer = null;
				data.setInstance( null );
				assert keyValueAssembler.assemble( rowProcessingState ) == null;
			}
			else {
				data.concreteDescriptor =
						fetchedPart.resolveDiscriminatorValue( discriminatorValue )
								.getEntityPersister();
				data.entityIdentifier = keyValueAssembler.assemble( rowProcessingState );
				data.concreteInitializer = resolveConcreteInitializer( data.concreteDescriptor );
				data.setState( State.KEY_RESOLVED );
				if ( data.concreteInitializer != null ) {
					data.concreteInitializer.resolveKey( rowProcessingState );
				}
			}
		}
	}

	@Override
	public void resolveState(JoinedDiscriminatedEntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		discriminatorAssembler.resolveState( rowProcessingState );
		keyValueAssembler.resolveState( rowProcessingState );
		if ( data.concreteInitializer != null ) {
			data.concreteInitializer.resolveState( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			final var concreteInitializer = data.concreteInitializer;
			if ( concreteInitializer != null ) {
				concreteInitializer.resolveInstance( data.getRowProcessingState() );
				transferConcreteResolution( concreteInitializer, data );
				if ( data.getInstance() != null || data.getState() == State.MISSING ) {
					return;
				}
			}

			data.setState( State.INITIALIZED );
			data.setInstance( data.getRowProcessingState().getSession()
					.internalLoad(
							data.concreteDescriptor.getEntityName(),
							data.entityIdentifier,
							eager,
							false
					) );
		}
	}

	@Override
	public void resolveInstance(Object instance, JoinedDiscriminatedEntityInitializerData data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
			data.concreteInitializer = null;
			data.initializeExistingProxy = false;
			data.setInstance( null );
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			resolveConcreteAssociation( instance, data, rowProcessingState.getSession() );

			final var concreteInitializer = data.concreteInitializer;
			if ( concreteInitializer != null ) {
				concreteInitializer.resolveInstance( instance, rowProcessingState );
				data.concreteDescriptor = concreteInitializer.getConcreteDescriptor( rowProcessingState );
				data.entityIdentifier = concreteInitializer.getEntityIdentifier( rowProcessingState );
				transferConcreteResolution( concreteInitializer, data );
			}
			else {
				data.setInstance( instance );
				if ( eager && !Hibernate.isInitialized( instance ) ) {  //TODO: don't like this
					data.initializeExistingProxy = true;
					data.setState( State.RESOLVED );
				}
				else {
					data.setState( State.INITIALIZED );
				}
			}

			if ( keyIsEager ) {
				final var initializer = keyValueAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() ) {
				discriminatorAssembler.resolveState( rowProcessingState );
				keyValueAssembler.resolveState( rowProcessingState );
			}
		}
	}

	private void resolveConcreteAssociation(
			Object instance,
			JoinedDiscriminatedEntityInitializerData data,
			SharedSessionContractImplementor session) {
		final var lazyInitializer = extractLazyInitializer( instance );
		data.initializeExistingProxy = false;
		if ( lazyInitializer == null ) {
			data.concreteDescriptor = session.getEntityPersister( null, instance );
			data.entityIdentifier = data.concreteDescriptor.getIdentifier( instance, session );
		}
		else {
			data.concreteDescriptor =
					lazyInitializer.isUninitialized()
							? session.getFactory().getMappingMetamodel().getEntityDescriptor( lazyInitializer.getEntityName() )
							: session.getEntityPersister( null, lazyInitializer.getImplementation() );
			data.entityIdentifier = lazyInitializer.getInternalIdentifier();
		}
		data.concreteInitializer = resolveConcreteInitializer( data.concreteDescriptor );
	}

	@Override
	public void initializeInstance(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.RESOLVED ) {
			if ( data.initializeExistingProxy ) {
				data.initializeExistingProxy = false;
				Hibernate.initialize( data.getInstance() );
			}
			else {
				final var concreteInitializer = data.concreteInitializer;
				final var rowProcessingState = data.getRowProcessingState();
				if ( concreteInitializer != null ) {
					concreteInitializer.initializeInstance( rowProcessingState );
					data.setInstance( concreteInitializer.getResolvedInstance( rowProcessingState ) );
				}
				else {
					data.setInstance( rowProcessingState.getSession()
							.internalLoad(
									data.concreteDescriptor.getEntityName(),
									data.entityIdentifier,
									eager,
									false
							) );
				}
			}
			data.setState( State.INITIALIZED );
		}
	}

	private static boolean hasImplicitDiscriminatorStrategy(DiscriminatorMapping discriminatorMapping) {
		return discriminatorMapping.getValueConverter() instanceof UnifiedAnyDiscriminatorConverter<?, ?> converter
			&& converter.hasImplicitValueStrategy();
	}

	private @Nullable EntityInitializer<?> resolveConcreteInitializer(EntityPersister concreteDescriptor) {
		final var concreteInitializer = concreteInitializersByEntityName.get( concreteDescriptor.getEntityName() );
		if ( concreteInitializer == null && !allowMissingConcreteInitializer ) {
			throw new IllegalStateException(
					"Initializer for " + concreteDescriptor.getEntityName() + " is unexpectedly missing"
			);
		}
		return concreteInitializer;
	}

	private void transferConcreteResolution(
			EntityInitializer<?> concreteInitializer,
			JoinedDiscriminatedEntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		final var state = concreteInitializer.getData( rowProcessingState ).getState();
		data.setInstance( switch ( state ) {
			case MISSING -> null;
			case RESOLVED, INITIALIZED -> concreteInitializer.getResolvedInstance( rowProcessingState );
			default -> throw new IllegalStateException( "Unexpected concrete initializer state: " + state );
		} );
		data.setState( state );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, JoinedDiscriminatedEntityInitializerData data) {
		final var attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance =
				attributeMapping != null
						? attributeMapping.getValue( parentInstance )
						: parentInstance;
		data.entityIdentifier = null;
		data.concreteDescriptor = null;
		data.concreteInitializer = null;
		data.initializeExistingProxy = false;
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
		}
		else {
			data.setInstance( instance );
			final var rowProcessingState = data.getRowProcessingState();
			resolveConcreteAssociation( instance, data, rowProcessingState.getSession() );
			final var concreteInitializer = data.concreteInitializer;
			if ( concreteInitializer != null ) {
				concreteInitializer.initializeInstanceFromParent( instance, rowProcessingState );
				data.concreteDescriptor = concreteInitializer.getConcreteDescriptor( rowProcessingState );
				data.entityIdentifier = concreteInitializer.getEntityIdentifier( rowProcessingState );
				transferConcreteResolution( concreteInitializer, data );
			}
			else {
				data.setState( State.INITIALIZED );
				if ( eager ) {
					Hibernate.initialize( instance ); //TODO: don't like this
				}
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final var keyInitializer = keyValueAssembler.getInitializer();
		if ( keyInitializer != null ) {
			consumer.accept( keyInitializer, data.getRowProcessingState() );
		}
		for ( var concreteInitializer : concreteInitializers ) {
			consumer.accept( concreteInitializer, data.getRowProcessingState() );
		}
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		throw new UnsupportedOperationException( "Discriminated association has no static entity type" );
	}

	@Override
	public EntityPersister getConcreteDescriptor(JoinedDiscriminatedEntityInitializerData data) {
		return data.concreteDescriptor;
	}

	@Override
	public @Nullable Object getEntityIdentifier(JoinedDiscriminatedEntityInitializerData data) {
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
		return "JoinedDiscriminatedEntityInitializer("
				+ toLoggableString( getNavigablePath() ) + ")";
	}
}
