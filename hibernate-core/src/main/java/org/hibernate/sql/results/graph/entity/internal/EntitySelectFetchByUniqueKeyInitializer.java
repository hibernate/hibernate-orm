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
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
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
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( state != State.UNINITIALIZED ) {
			return;
		}
		state = State.RESOLVED;

		// We can avoid processing further if the parent is already initialized or missing,
		// as the value produced by this initializer will never be used anyway.
		if ( parentShallowCached || shouldSkipInitializer( rowProcessingState ) ) {
			state = State.INITIALIZED;
			return;
		}

		entityIdentifier = keyAssembler.assemble( rowProcessingState );
		if ( entityIdentifier == null ) {
			state = State.INITIALIZED;
			return;
		}

		// Defer the select by default to the initialize phase
		// We only need to select in this phase if this is part of an identifier or foreign key
		NavigablePath np = getNavigablePath().getParent();
		while ( np != null ) {
			if ( np instanceof EntityIdentifierNavigablePath
					|| ForeignKeyDescriptor.PART_NAME.equals( np.getLocalName() )
					|| ForeignKeyDescriptor.TARGET_PART_NAME.equals( np.getLocalName() )) {

				initializeInstance( rowProcessingState );
				return;
			}
			np = np.getParent();
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( state != State.RESOLVED ) {
			return;
		}
		state = State.INITIALIZED;

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
		}
		if ( entityInstance != null ) {
			entityInstance = persistenceContext.proxyFor( entityInstance );
		}
	}

	@Override
	public String toString() {
		return "EntitySelectFetchByUniqueKeyInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
