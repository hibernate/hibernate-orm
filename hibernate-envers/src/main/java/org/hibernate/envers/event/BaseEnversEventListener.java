/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.event;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.RelationDescription;
import org.hibernate.envers.entities.RelationType;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.synchronization.AuditProcess;
import org.hibernate.envers.synchronization.work.CollectionChangeWorkUnit;
import org.hibernate.envers.tools.Tools;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

/**
 * Base class for all Envers event listeners
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Hern�n Chanfreau
 * @author Steve Ebersole
 */
public abstract class BaseEnversEventListener implements EnversListener {
	private AuditConfiguration enversConfiguration;

	protected BaseEnversEventListener(AuditConfiguration enversConfiguration) {
		this.enversConfiguration = enversConfiguration;
	}

	@Override
	public AuditConfiguration getAuditConfiguration() {
		return enversConfiguration;
	}

	protected final void generateBidirectionalCollectionChangeWorkUnits(
			AuditProcess auditProcess,
			EntityPersister entityPersister,
			String entityName,
			Object[] newState,
			Object[] oldState,
			SessionImplementor session) {
		// Checking if this is enabled in configuration ...
		if ( ! enversConfiguration.getGlobalCfg().isGenerateRevisionsForCollections() ) {
			return;
		}

		// Checks every property of the entity, if it is an "owned" to-one relation to another entity.
		// If the value of that property changed, and the relation is bi-directional, a new revision
		// for the related entity is generated.
		String[] propertyNames = entityPersister.getPropertyNames();

		for ( int i=0; i<propertyNames.length; i++ ) {
			String propertyName = propertyNames[i];
			RelationDescription relDesc = enversConfiguration.getEntCfg().getRelationDescription(entityName, propertyName);
			if (relDesc != null && relDesc.isBidirectional() && relDesc.getRelationType() == RelationType.TO_ONE &&
					relDesc.isInsertable()) {
				// Checking for changes
				Object oldValue = oldState == null ? null : oldState[i];
				Object newValue = newState == null ? null : newState[i];

				if (!Tools.entitiesEqual( session, relDesc.getToEntityName(), oldValue, newValue )) {
					// We have to generate changes both in the old collection (size decreses) and new collection
					// (size increases).
					if (newValue != null) {
						// relDesc.getToEntityName() doesn't always return the entity name of the value - in case
						// of subclasses, this will be root class, no the actual class. So it can't be used here.
						String toEntityName;
						Serializable id;

						if (newValue instanceof HibernateProxy ) {
							HibernateProxy hibernateProxy = (HibernateProxy) newValue;
							toEntityName = session.bestGuessEntityName(newValue);
							id = hibernateProxy.getHibernateLazyInitializer().getIdentifier();
							// We've got to initialize the object from the proxy to later read its state.
							newValue = Tools.getTargetFromProxy(session.getFactory(), hibernateProxy);
						} else {
							toEntityName =  session.guessEntityName(newValue);

							IdMapper idMapper = enversConfiguration.getEntCfg().get(toEntityName).getIdMapper();
							 id = (Serializable) idMapper.mapToIdFromEntity(newValue);
						}

						auditProcess.addWorkUnit(new CollectionChangeWorkUnit(session, toEntityName, enversConfiguration, id, newValue));
					}

					if (oldValue != null) {
						String toEntityName;
						Serializable id;

						if(oldValue instanceof HibernateProxy) {
							HibernateProxy hibernateProxy = (HibernateProxy) oldValue;
							toEntityName = session.bestGuessEntityName(oldValue);
							id = hibernateProxy.getHibernateLazyInitializer().getIdentifier();
							// We've got to initialize the object as we'll read it's state anyway.
							oldValue = Tools.getTargetFromProxy(session.getFactory(), hibernateProxy);
						} else {
							toEntityName =  session.guessEntityName(oldValue);

							IdMapper idMapper = enversConfiguration.getEntCfg().get(toEntityName).getIdMapper();
							id = (Serializable) idMapper.mapToIdFromEntity(oldValue);
						}

						auditProcess.addWorkUnit(new CollectionChangeWorkUnit(session, toEntityName, enversConfiguration, id, oldValue));
					}
				}
			}
		}
	}

}
