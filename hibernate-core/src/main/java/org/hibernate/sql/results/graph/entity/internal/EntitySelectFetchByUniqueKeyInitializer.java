/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchByUniqueKeyInitializer
		extends EntitySelectFetchInitializer<EntitySelectFetchInitializer.EntitySelectFetchInitializerData> {
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
	protected void initializeIfNecessary(EntitySelectFetchInitializerData data) {
		final var session = data.getRowProcessingState().getSession();
		final var persistenceContext = session.getPersistenceContextInternal();

		final String uniqueKeyPropertyName = fetchedAttribute.getReferencedPropertyName();
		final EntityUniqueKey euk = new EntityUniqueKey(
				concreteDescriptor.getEntityName(),
				uniqueKeyPropertyName,
				data.entityIdentifier,
				concreteDescriptor.getPropertyType( uniqueKeyPropertyName ),
				session.getFactory()
		);
		data.setInstance( persistenceContext.getEntity( euk ) );
		if ( data.getInstance() == null ) {
			final Object instance = concreteDescriptor.loadByUniqueKey(
					uniqueKeyPropertyName,
					data.entityIdentifier,
					session
			);
			data.setInstance( instance );

			if ( instance == null ) {
				handleNotFound( data );
			}
			// If the entity was not in the persistence context but
			// was found now, then add it to the persistence context
			persistenceContext.addEntity( euk, instance );
		}
		if ( data.getInstance() != null ) {
			data.setInstance( persistenceContext.proxyFor( data.getInstance() ) );
		}
	}

	@Override
	public String toString() {
		return "EntitySelectFetchByUniqueKeyInitializer(" + toLoggableString( getNavigablePath() ) + ")";
	}
}
