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
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;

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
			AssemblerCreationState creationState) {
		super( parent, fetchedAttribute, fetchedNavigable, concreteDescriptor, keyResult, creationState );
		this.fetchedAttribute = fetchedAttribute;
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
			data.setInstance( concreteDescriptor.loadByUniqueKey(
					uniqueKeyPropertyName,
					data.entityIdentifier,
					session
			) );

			// If the entity was not in the Persistence Context, but was found now,
			// add it to the Persistence Context
			if ( data.getInstance() != null ) {
				persistenceContext.addEntity( euk, data.getInstance() );
			}
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
