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
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
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
			DomainResultAssembler identifierAssembler) {
		super( parentAccess, fetchedAttribute, fetchedNavigable, concreteDescriptor, identifierAssembler );
		this.fetchedAttribute = fetchedAttribute;
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( entityInstance != null || isInitialized ) {
			return;
		}

		if ( !isAttributeAssignableToConcreteDescriptor() ) {
			isInitialized = true;
			return;
		}

		final Object entityIdentifier = identifierAssembler.assemble( rowProcessingState );
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
			entityInstance = ( (UniqueKeyLoadable) concreteDescriptor ).loadByUniqueKey(
					uniqueKeyPropertyName,
					entityIdentifier,
					session
			);

			// If the entity was not in the Persistence Context, but was found now,
			// add it to the Persistence Context
			if ( entityInstance != null ) {
				persistenceContext.addEntity( euk, entityInstance );
			}
		}
		if ( entityInstance != null ) {
			entityInstance = persistenceContext.proxyFor( entityInstance );
		}
		isInitialized = true;
	}
}
