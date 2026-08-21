/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
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
	private final BitSet affectedByFilter;
	private final boolean resultInitializer;
	private final DomainResultAssembler<?> discriminatorAssembler;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final Map<String, EntityInitializer<EntityInitializerImpl.EntityInitializerData>> concreteInitializersByEntityName;
	private final EntityInitializer<EntityInitializerImpl.EntityInitializerData>[] concreteInitializers;
	private final boolean keyIsEager;
	private final boolean hasLazySubInitializer;
	// workaround for the fact that implicit discriminator mappings are not available
	private final boolean allowMissingConcreteInitializer;

	public static class JoinedDiscriminatedEntityInitializerData extends InitializerData {
		protected EntityPersister concreteDescriptor;
		protected Object entityIdentifier;
		protected EntityInitializer<EntityInitializerImpl.EntityInitializerData> concreteInitializer;

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
			BitSet affectedByFilter,
			boolean resultInitializer,
			List<EntityResultImpl<?>> concreteEntityResults,
			AssemblerCreationState creationState) {
		super( creationState );
		this.parent = parent;
		this.fetchedPart = fetchedPart;
		this.navigablePath = fetchedNavigable;
		this.eager = eager;
		this.affectedByFilter = affectedByFilter;
		this.resultInitializer = resultInitializer;

		isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		discriminatorAssembler = discriminatorFetch.createAssembler( this, creationState );
		keyValueAssembler = keyFetch.createAssembler( this, creationState );
		keyIsEager = keyValueAssembler.isEager();

		final Map<String, EntityInitializer<EntityInitializerImpl.EntityInitializerData>> initializersByEntityName =
				new LinkedHashMap<>( concreteEntityResults.size() );
		@SuppressWarnings("unchecked")
		final var initializers = (EntityInitializer<EntityInitializerImpl.EntityInitializerData>[])
				new EntityInitializer[concreteEntityResults.size()];
		boolean lazySubInitializer = keyValueAssembler.hasLazySubInitializers();
		for ( int i = 0; i < concreteEntityResults.size(); i++ ) {
			final var entityResult = concreteEntityResults.get( i );
			final var initializer = resolveEntityInitializer( i, entityResult, creationState );
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

	private EntityInitializer<EntityInitializerImpl.EntityInitializerData> resolveEntityInitializer(
			int index,
			EntityResultImpl<?> entityResult,
			AssemblerCreationState creationState) {
		//noinspection unchecked
		return (EntityInitializer<EntityInitializerImpl.EntityInitializerData>) creationState.resolveInitializer(
				entityResult.getNavigablePath(),
				entityResult.getReferencedModePart(),
				() -> new EntityInitializerImpl(
						entityResult,
						entityResult.getSourceAlias(),
						entityResult.getIdentifierFetch(),
						entityResult.getDiscriminatorFetch(),
						null,
						entityResult.getRowIdResult(),
						entityResult.getAuditChangesetIdResult(),
						NotFoundAction.EXCEPTION,
						affectedByFilter.get( index ),
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
			if ( discriminatorValue == null ) {
				setMissing( data );
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
					if ( data.concreteInitializer.getData( rowProcessingState ).getState() == State.MISSING ) {
						setMissing( data );
					}
				}
			}
		}
	}

	private void setMissing(JoinedDiscriminatedEntityInitializerData data) {
		final String entityName = data.concreteDescriptor == null
				? "<unknown>"
				: data.concreteDescriptor.getEntityName();
		data.setState( State.MISSING );
		data.concreteDescriptor = null;
		data.entityIdentifier = null;
		data.concreteInitializer = null;
		data.setInstance( null );
		final Object foreignKeyValue = keyValueAssembler.assemble( data.getRowProcessingState() );
		if ( foreignKeyValue != null ) {
			final var concreteInitializer = concreteInitializersByEntityName.get( entityName );
			final boolean filteredOut;
			if ( concreteInitializer == null ) {
				// Discriminator is null, but foreign key is given. Let's just assume this was filtered out,
				// if any initializer was affected by a filter
				filteredOut = !affectedByFilter.isEmpty();
			}
			else {
				final var index = ArrayHelper.indexOf( concreteInitializers, concreteInitializer );
				assert index >= 0;
				filteredOut = affectedByFilter.get( index );
			}
			if ( filteredOut ) {
				throw new EntityFilterException( entityName, foreignKeyValue,
						fetchedPart.getNavigableRole().getFullPath() );
			}
			throw new FetchNotFoundException( entityName, foreignKeyValue );
		}
	}

	@Override
	public void resolveFromPreviousRow(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final var initializer = keyValueAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				if ( data.concreteInitializer != null ) {
					data.concreteInitializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveState(JoinedDiscriminatedEntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		final Object discriminatorValue = discriminatorAssembler.assemble( rowProcessingState );
		keyValueAssembler.resolveState( rowProcessingState );
		if ( discriminatorValue  != null ) {
			final var concreteDescriptor = fetchedPart.resolveDiscriminatorValue( discriminatorValue )
					.getEntityPersister();
			final var concreteInitializer =
					concreteInitializersByEntityName.get( concreteDescriptor.getEntityName() );
			if ( concreteInitializer == null ) {
				throw new IllegalStateException( "Initializer for " + concreteDescriptor.getEntityName() + " is unexpectedly missing!" );
			}
			concreteInitializer.resolveState( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			final var concreteInitializer = data.concreteInitializer;
			if ( concreteInitializer != null ) {
				final var initializerData = concreteInitializer.getData( data.getRowProcessingState() );
				concreteInitializer.resolveInstance( initializerData );
				transferConcreteResolution( concreteInitializer, initializerData, data );
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
			data.setInstance( null );
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final var session = rowProcessingState.getSession();
			final var concreteDescriptor = resolveConcreteDescriptor( instance, session );
			final var concreteInitializer = resolveConcreteInitializer( concreteDescriptor );
			if ( concreteInitializer != null ) {
				final var initializerData = concreteInitializer.getData( rowProcessingState );
				concreteInitializer.resolveInstance( instance, initializerData );
				data.concreteDescriptor = concreteInitializer.getConcreteDescriptor( initializerData );
				data.entityIdentifier = concreteInitializer.getEntityIdentifier( initializerData );
				data.concreteInitializer = concreteInitializer;
				transferConcreteResolution( concreteInitializer, initializerData, data );
			}
			else {
				// Match the behavior of EntitySelectFetchInitializer
				final var lazyInitializer = extractLazyInitializer( instance );
				if ( lazyInitializer == null ) {
					data.setState( State.INITIALIZED );
					data.entityIdentifier = concreteDescriptor.getIdentifier( instance, session );
				}
				else {
					data.setState( lazyInitializer.isUninitialized() ? State.RESOLVED : State.INITIALIZED );
					data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				}

				final var entityKey = data.getRowProcessingState().getSession().generateEntityKey( data.entityIdentifier, concreteDescriptor );
				final var entityHolder = session.getPersistenceContextInternal().getEntityHolder(
						entityKey
				);

				if ( entityHolder == null || entityHolder.getEntity() != instance && entityHolder.getProxy() != instance ) {
					// the existing entity instance is detached or transient
					if ( entityHolder != null ) {
						final var managed = entityHolder.getManagedObject();
						data.setInstance( managed );
						data.entityIdentifier = entityHolder.getEntityKey().getIdentifier();
						data.setState( entityHolder.isInitialized() ? State.INITIALIZED : State.RESOLVED );
					}
				}
				else {
					data.setInstance( instance );
				}
				if ( eager && data.getState() == State.RESOLVED ) {
					Hibernate.initialize( instance ); //TODO: don't love this
				}
				data.setState( State.INITIALIZED );
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

	@Override
	public void initializeInstance(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.RESOLVED ) {
			final var rowProcessingState = data.getRowProcessingState();
			data.concreteInitializer.initializeInstance( rowProcessingState );
			data.setInstance( data.concreteInitializer.getResolvedInstance( rowProcessingState ) );
			data.setState( State.INITIALIZED );
		}
	}

	private static boolean hasImplicitDiscriminatorStrategy(DiscriminatorMapping discriminatorMapping) {
		return discriminatorMapping.getValueConverter() instanceof UnifiedAnyDiscriminatorConverter<?, ?> converter
			&& converter.hasImplicitValueStrategy();
	}

	private EntityPersister resolveConcreteDescriptor(Object instance, SharedSessionContractImplementor session) {
		final var lazyInitializer = extractLazyInitializer( instance );
		if ( lazyInitializer == null ) {
			return session.getEntityPersister( null, instance );
		}
		else {
			return lazyInitializer.isUninitialized()
					? session.getFactory().getMappingMetamodel().getEntityDescriptor( lazyInitializer.getEntityName() )
					: session.getEntityPersister( null, lazyInitializer.getImplementation() );
		}
	}

	private @Nullable EntityInitializer<EntityInitializerImpl.EntityInitializerData> resolveConcreteInitializer(EntityPersister concreteDescriptor) {
		final var concreteInitializer = concreteInitializersByEntityName.get( concreteDescriptor.getEntityName() );
		if ( concreteInitializer == null && !allowMissingConcreteInitializer ) {
			throw new IllegalStateException(
					"Initializer for " + concreteDescriptor.getEntityName() + " is unexpectedly missing"
			);
		}
		return concreteInitializer;
	}

	private <X extends InitializerData> void transferConcreteResolution(
			EntityInitializer<X> concreteInitializer,
			X concreteInitializerData,
			JoinedDiscriminatedEntityInitializerData data) {
		final var state = concreteInitializerData.getState();
		data.setInstance( switch ( state ) {
			case MISSING -> null;
			case RESOLVED, INITIALIZED -> concreteInitializer.getResolvedInstance( concreteInitializerData );
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
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
		}
		else {
			data.setInstance( instance );
			final var rowProcessingState = data.getRowProcessingState();
			final var session = rowProcessingState.getSession();
			final var concreteDescriptor = resolveConcreteDescriptor( instance, session );
			final var concreteInitializer = resolveConcreteInitializer( concreteDescriptor );
			if ( concreteInitializer != null ) {
				final var initializerData = concreteInitializer.getData( rowProcessingState );
				concreteInitializer.initializeInstanceFromParent( instance, initializerData );
				data.concreteDescriptor = concreteInitializer.getConcreteDescriptor( initializerData );
				data.entityIdentifier = concreteInitializer.getEntityIdentifier( initializerData );
				data.concreteInitializer = concreteInitializer;
				transferConcreteResolution( concreteInitializer, initializerData, data );
			}
			else {
				data.setState( State.INITIALIZED );
				if ( eager ) {
					Hibernate.initialize( instance );
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
