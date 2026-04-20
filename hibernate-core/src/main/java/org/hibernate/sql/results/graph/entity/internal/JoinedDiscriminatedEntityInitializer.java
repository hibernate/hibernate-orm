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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
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
	private final DomainResultAssembler<?> discriminatorValueAssembler;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final Map<String, EntityInitializer<?>> concreteInitializersByEntityName;
	private final EntityInitializer<?>[] concreteInitializers;
	private final boolean keyIsEager;
	private final boolean hasLazySubInitializer;

	public static class JoinedDiscriminatedEntityInitializerData extends InitializerData {
		protected EntityPersister concreteDescriptor;
		protected Object entityIdentifier;
		protected EntityInitializer<?> concreteInitializer;

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
		discriminatorValueAssembler = discriminatorFetch.createAssembler( this, creationState );
		keyValueAssembler = keyFetch.createAssembler( this, creationState );

		keyIsEager = keyValueAssembler.isEager();

		final Map<String, EntityInitializer<?>> initializersByEntityName = new LinkedHashMap<>( concreteEntityResults.size() );
		final EntityInitializer<?>[] initializers = new EntityInitializer<?>[concreteEntityResults.size()];
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
		this.concreteInitializers = initializers;
		this.concreteInitializersByEntityName = initializersByEntityName;
		this.hasLazySubInitializer = lazySubInitializer;
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
			final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState );
			if ( discriminatorValue == null ) {
				data.setState( State.MISSING );
				data.concreteDescriptor = null;
				data.entityIdentifier = null;
				data.concreteInitializer = null;
				data.setInstance( null );
				assert keyValueAssembler.assemble( rowProcessingState ) == null;
				return;
			}

			data.concreteDescriptor =
					fetchedPart.resolveDiscriminatorValue( discriminatorValue )
							.getEntityPersister();
			data.entityIdentifier = keyValueAssembler.assemble( rowProcessingState );
			data.concreteInitializer =
					concreteInitializersByEntityName.get( data.concreteDescriptor.getEntityName() );
			data.setState( State.KEY_RESOLVED );

			if ( data.concreteInitializer != null ) {
				data.concreteInitializer.resolveKey( rowProcessingState );
			}
		}
	}

	@Override
	public void resolveState(JoinedDiscriminatedEntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		discriminatorValueAssembler.resolveState( rowProcessingState );
		keyValueAssembler.resolveState( rowProcessingState );
		if ( data.concreteInitializer != null ) {
			data.concreteInitializer.resolveState( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			if ( data.concreteInitializer != null ) {
				data.concreteInitializer.resolveInstance( data.getRowProcessingState() );
				data.setInstance( data.concreteInitializer.getResolvedInstance( data.getRowProcessingState() ) );
				final var concreteState = data.concreteInitializer.getData( data.getRowProcessingState() ).getState();
				if ( concreteState == State.MISSING ) {
					data.setState( State.MISSING );
					data.setInstance( null );
				}
				else if ( concreteState == State.RESOLVED ) {
					data.setState( State.RESOLVED );
				}
				else {
					data.setState( State.INITIALIZED );
				}
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
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
		}
	}

	@Override
	public void initializeInstance(JoinedDiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.RESOLVED ) {
			if ( data.concreteInitializer != null ) {
				data.concreteInitializer.initializeInstance( data.getRowProcessingState() );
				data.setInstance( data.concreteInitializer.getResolvedInstance( data.getRowProcessingState() ) );
			}
			else {
				data.setInstance( data.getRowProcessingState().getSession()
						.internalLoad(
								data.concreteDescriptor.getEntityName(),
								data.entityIdentifier,
								eager,
								false
						) );
			}
			data.setState( State.INITIALIZED );
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, JoinedDiscriminatedEntityInitializerData data) {
		final var attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance =
				attributeMapping != null
						? attributeMapping.getValue( parentInstance )
						: parentInstance;
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
			data.concreteInitializer = null;
		}
		else {
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
			data.concreteInitializer = null;
			if ( eager ) {
				Hibernate.initialize( instance );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final var keyInitializer = keyValueAssembler.getInitializer();
		if ( keyInitializer != null ) {
			consumer.accept( keyInitializer, data.getRowProcessingState() );
		}
		for ( EntityInitializer<?> concreteInitializer : concreteInitializers ) {
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
