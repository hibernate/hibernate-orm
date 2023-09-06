/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchByUniqueKeyInitializer extends EntitySelectFetchInitializer {
	private final ToOneAttributeMapping fetchedAttribute;


	public EntitySelectFetchByUniqueKeyInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping fetchedAttribute,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> keyAssembler) {
		super( parentAccess, fetchedAttribute, fetchedNavigable, concreteDescriptor, keyAssembler );
		this.fetchedAttribute = fetchedAttribute;
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( entityInstance != null || isInitialized ) {
			return;
		}

		final EntityInitializer parentEntityInitializer = getParentEntityInitializer( parentAccess );
		if ( parentEntityInitializer != null && parentEntityInitializer.getEntityKey() != null ) {
			// make sure parentEntityInitializer.resolveInstance has been called before
			parentEntityInitializer.resolveInstance( rowProcessingState );
			if ( parentEntityInitializer.isEntityInitialized() ) {
				isInitialized = true;
				return;
			}
		}

		if ( !isAttributeAssignableToConcreteDescriptor() ) {
			isInitialized = true;
			return;
		}

		final Object entityIdentifier = keyAssembler.assemble( rowProcessingState );
		if ( entityIdentifier == null ) {
			isInitialized = true;
			return;
		}
		final String entityName = concreteDescriptor.getEntityName();
		final String uniqueKeyPropertyName = fetchedAttribute.getReferencedPropertyName();

		final SharedSessionContractImplementor session = rowProcessingState.getSession();

		final EntityUniqueKey euk = new EntityUniqueKey(
				entityName,
				uniqueKeyPropertyName,
				entityIdentifier,
				concreteDescriptor.getPropertyType( uniqueKeyPropertyName ),
				session.getFactory()
		);
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		entityInstance = persistenceContext.getEntity( euk );
		if ( entityInstance == null ) {
			final EntitySelectFetchByUniqueKeyInitializer initializer = (EntitySelectFetchByUniqueKeyInitializer) persistenceContext.getLoadContexts()
					.findInitializer( euk );
			if ( initializer == null ) {
				final JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState = rowProcessingState.getJdbcValuesSourceProcessingState();
				jdbcValuesSourceProcessingState.registerInitializer( euk, this );

				entityInstance = concreteDescriptor.loadByUniqueKey(
						uniqueKeyPropertyName,
						entityIdentifier,
						session
				);

				// If the entity was not in the Persistence Context, but was found now,
				// add it to the Persistence Context
				if ( entityInstance != null ) {
					persistenceContext.addEntity( euk, entityInstance );
				}
				notifyResolutionListeners(entityInstance);
			}
			else {
				registerResolutionListener( instance -> entityInstance = instance );
			}
		}
		if ( entityInstance != null ) {
			entityInstance = persistenceContext.proxyFor( entityInstance );
		}
		isInitialized = true;
	}

	private EntityInitializer getParentEntityInitializer(FetchParentAccess parentAccess) {
		if ( parentAccess != null ) {
			return parentAccess.findFirstEntityInitializer();
		}
		return null;
	}

	@Override
	public String toString() {
		return "EntitySelectFetchByUniqueKeyInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
