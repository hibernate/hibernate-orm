/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchByUniqueKeyInitializer extends EntitySelectFetchInitializer<EntitySelectFetchInitializer.EntitySelectFetchInitializerData> {
	private final ToOneAttributeMapping fetchedAttribute;

	public EntitySelectFetchByUniqueKeyInitializer(
			InitializerParent<?> parent,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResult<?> keyResult,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		super( parent, fetchedAttribute, fetchedNavigable, concreteDescriptor, keyResult, affectedByFilter, creationState );
		this.fetchedAttribute = fetchedAttribute;
	}

	@Override
	public void resolveInstance(Object instance, EntitySelectFetchInitializerData data) {
		if ( instance == null && !isReadOnly ) {
			data.entityIdentifier = null;
			data.setState( State.MISSING );
			data.setInstance( null );
		}
		else if ( isReadOnly ) {
			// When the mapping is read-only, we can't trust the state of the persistence context
			resolveKey( data );
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			data.entityIdentifier = keyAssembler.assemble( rowProcessingState );
			if ( data.entityIdentifier == null ) {
				data.setState( State.MISSING );
				data.setInstance( null );
			}
			else {
				initialize( data );
			}
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final var session = rowProcessingState.getSession();
			final var entityDescriptor = getEntityDescriptor();
			final LazyInitializer lazyInitializer = extractLazyInitializer( instance );
			final boolean identifierResolved;
			final Object primaryKey;
			if ( lazyInitializer == null ) {
				data.setState( State.INITIALIZED );
				data.entityIdentifier =
						entityDescriptor.getPropertyValue( instance, fetchedAttribute.getReferencedPropertyName() );
				primaryKey = entityDescriptor.getIdentifier( instance, session );
				identifierResolved = false;
			}
			else if ( lazyInitializer.isUninitialized() ) {
				data.setState( State.RESOLVED );
				data.entityIdentifier = keyAssembler.assemble( rowProcessingState );
				primaryKey = lazyInitializer.getIdentifier();
				identifierResolved = true;
			}
			else {
				data.setState( State.INITIALIZED );
				data.entityIdentifier =
						entityDescriptor.getPropertyValue( instance, fetchedAttribute.getReferencedPropertyName() );
				primaryKey = lazyInitializer.getIdentifier();
				identifierResolved = false;
			}
			assert data.entityIdentifier != null;

			final var persistenceContext = session.getPersistenceContextInternal();
			final var entityKey = new EntityKey( primaryKey, concreteDescriptor );
			final var entityHolder = persistenceContext.getEntityHolder( entityKey );

			if ( entityHolder == null
					|| entityHolder.getEntity() != instance && entityHolder.getProxy() != instance ) {
				// the existing entity instance is detached or transient
				if ( entityHolder != null ) {
					final var managed = entityHolder.getManagedObject();
					data.setInstance( managed );
					data.entityIdentifier = entityHolder.getEntityKey().getIdentifier();
					data.setState( entityHolder.isInitialized() ? State.INITIALIZED : State.RESOLVED );
				}
				else {
					initialize( data, null, session, persistenceContext );
				}
			}
			else {
				data.setInstance( instance );
			}

			if ( keyIsEager && !identifierResolved ) {
				final Initializer<?> initializer = keyAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() && !identifierResolved ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				keyAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	protected void initialize(EntitySelectFetchInitializerData data) {
		final String entityName = concreteDescriptor.getEntityName();
		final String uniqueKeyPropertyName = fetchedAttribute.getReferencedPropertyName();

		final SharedSessionContractImplementor session = data.getRowProcessingState().getSession();

		final EntityUniqueKey euk = new EntityUniqueKey(
				entityName,
				uniqueKeyPropertyName,
				data.entityIdentifier,
				concreteDescriptor.getPropertyType( uniqueKeyPropertyName ),
				session.getFactory()
		);
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		data.setInstance( persistenceContext.getEntity( euk ) );
		if ( data.getInstance() == null ) {
			final Object instance = concreteDescriptor.loadByUniqueKey(
					uniqueKeyPropertyName,
					data.entityIdentifier,
					session
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
			}
			// If the entity was not in the Persistence Context, but was found now,
			// add it to the Persistence Context
			persistenceContext.addEntity( euk, instance );
		}
		if ( data.getInstance() != null ) {
			data.setInstance( persistenceContext.proxyFor( data.getInstance() ) );
		}
	}

	@Override
	public String toString() {
		return "EntitySelectFetchByUniqueKeyInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
