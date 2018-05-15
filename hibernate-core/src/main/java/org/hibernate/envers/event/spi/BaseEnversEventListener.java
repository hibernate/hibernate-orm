/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.event.spi;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.work.CollectionChangeWorkUnit;
import org.hibernate.envers.internal.tools.EntityTools;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.proxy.HibernateProxy;

/**
 * Base class for all Envers event listeners
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public abstract class BaseEnversEventListener implements EnversListener {
	private final AuditService auditService;

	protected BaseEnversEventListener(AuditService auditService) {
		this.auditService = auditService;
	}

	protected AuditService getAuditService() {
		return auditService;
	}

	protected final void generateBidirectionalCollectionChangeWorkUnits(
			AuditProcess auditProcess,
			EntityTypeDescriptor entityDescriptor,
			String entityName,
			Object[] newState,
			Object[] oldState,
			SessionImplementor session) {

		// Checking if this is enabled in configuration ...
		if ( !auditService.getOptions().isRevisionOnCollectionChangeEnabled() ) {
			return;
		}

		// Checks every property of the entity, if it is an "owned" to-one relation to another entity.
		// If the value of that property changed, and the relation is bi-directional, a new revision
		// for the related entity is generated.
		final String[] propertyNames = EntityTools.getPropertyNames( entityDescriptor );

		for ( int i = 0; i < propertyNames.length; i++ ) {
			final String propertyName = propertyNames[i];
			final RelationDescription relDesc = auditService.getEntityBindings().getRelationDescription(
					entityName,
					propertyName
			);
			if ( relDesc != null && relDesc.isBidirectional() && relDesc.getRelationType() == RelationType.TO_ONE &&
					relDesc.isInsertable() ) {
				// Checking for changes
				final Object oldValue = oldState == null ? null : oldState[i];
				final Object newValue = newState == null ? null : newState[i];

				if ( !EntityTools.entitiesEqual( session, relDesc.getToEntityName(), oldValue, newValue ) ) {
					// We have to generate changes both in the old collection (size decreses) and new collection
					// (size increases).
					if ( newValue != null ) {
						addCollectionChangeWorkUnit( auditProcess, session, entityName, relDesc, newValue );
					}

					if ( oldValue != null ) {
						addCollectionChangeWorkUnit( auditProcess, session, entityName, relDesc, oldValue );
					}
				}
			}
		}
	}

	private void addCollectionChangeWorkUnit(
			AuditProcess auditProcess,
			SessionImplementor session,
			String fromEntityName,
			RelationDescription relDesc,
			Object value) {

		// relDesc.getToEntityName() doesn't always return the entity name of the value - in case
		// of subclasses, this will be root class, no the actual class. So it can't be used here.
		String toEntityName;
		Object id;

		if ( value instanceof HibernateProxy ) {
			final HibernateProxy hibernateProxy = (HibernateProxy) value;
			id = hibernateProxy.getHibernateLazyInitializer().getIdentifier();
			// We've got to initialize the object from the proxy to later read its state.
			value = EntityTools.getTargetFromProxy( session.getFactory(), hibernateProxy );
			// HHH-7249
			// This call must occur after the proxy has been initialized or the returned name will
			// be to the base class which will impact the discriminator value chosen when using an
			// inheritance strategy with discriminators.
			toEntityName = session.bestGuessEntityName( value );
		}
		else {
			toEntityName = session.guessEntityName( value );

			final IdMapper idMapper = auditService.getEntityBindings().get( toEntityName ).getIdMapper();
			id = (Serializable) idMapper.mapToIdFromEntity( value );
		}

		final Set<String> toPropertyNames = auditService.getEntityBindings().getToPropertyNames(
				fromEntityName,
				relDesc.getFromPropertyName(),
				toEntityName
		);
		final String toPropertyName = toPropertyNames.iterator().next();

		auditProcess.addWorkUnit(
				new CollectionChangeWorkUnit(
						session,
						toEntityName,
						toPropertyName,
						auditService,
						id,
						value
				)
		);
	}

	protected void checkIfTransactionInProgress(SessionImplementor session) {
		if ( !session.isTransactionInProgress() ) {
			// Historical data would not be flushed to audit tables if outside of active transaction
			// (AuditProcess#doBeforeTransactionCompletion(SessionImplementor) not executed).
			throw new AuditException( "Unable to create revision because of non-active transaction" );
		}
	}
}
