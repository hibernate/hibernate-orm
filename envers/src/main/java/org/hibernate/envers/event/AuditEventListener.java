/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.RelationDescription;
import org.hibernate.envers.entities.RelationType;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.synchronization.AuditSync;
import org.hibernate.envers.synchronization.work.AddWorkUnit;
import org.hibernate.envers.synchronization.work.CollectionChangeWorkUnit;
import org.hibernate.envers.synchronization.work.DelWorkUnit;
import org.hibernate.envers.synchronization.work.ModWorkUnit;
import org.hibernate.envers.synchronization.work.PersistentCollectionChangeWorkUnit;
import org.hibernate.envers.tools.Tools;

import org.hibernate.cfg.Configuration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.CollectionEntry;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.AbstractCollectionEvent;
import org.hibernate.event.Initializable;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreCollectionRemoveEvent;
import org.hibernate.event.PreCollectionRemoveEventListener;
import org.hibernate.event.PreCollectionUpdateEvent;
import org.hibernate.event.PreCollectionUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditEventListener implements PostInsertEventListener, PostUpdateEventListener,
        PostDeleteEventListener, PreCollectionUpdateEventListener, PreCollectionRemoveEventListener,
        PostCollectionRecreateEventListener, Initializable {
	private static final long serialVersionUID = -2499904286323112715L;

    private AuditConfiguration verCfg;

    private void generateBidirectionalCollectionChangeWorkUnits(AuditSync verSync, EntityPersister entityPersister,
                                                                String entityName, Object[] newState, Object[] oldState,
                                                                SessionImplementor session) {
        // Checking if this is enabled in configuration ...
        if (!verCfg.getGlobalCfg().isGenerateRevisionsForCollections()) {
            return;
        }

        // Checks every property of the entity, if it is an "owned" to-one relation to another entity.
        // If the value of that property changed, and the relation is bi-directional, a new revision
        // for the related entity is generated.
        String[] propertyNames = entityPersister.getPropertyNames();

        for (int i=0; i<propertyNames.length; i++) {
            String propertyName = propertyNames[i];
            RelationDescription relDesc = verCfg.getEntCfg().getRelationDescription(entityName, propertyName);
            if (relDesc != null && relDesc.isBidirectional() && relDesc.getRelationType() == RelationType.TO_ONE) {
                // Checking for changes
                Object oldValue = oldState == null ? null : oldState[i];
                Object newValue = newState == null ? null : newState[i];

                if (!Tools.objectsEqual(oldValue, newValue)) {
                    // We have to generate changes both in the old collection (size decreses) and new collection
                    // (size increases).
                    if (newValue != null) {
                        // relDesc.getToEntityName() doesn't always return the entity name of the value - in case
                        // of subclasses, this will be root class, no the actual class. So it can't be used here.
                        String toEntityName;
                        if(newValue instanceof HibernateProxy) {
                    	    HibernateProxy hibernateProxy = (HibernateProxy) newValue;
                    	    toEntityName = session.bestGuessEntityName(newValue);
                    	    newValue = hibernateProxy.getHibernateLazyInitializer().getImplementation();
                    	} else {
                    		toEntityName =  session.guessEntityName(newValue);
                    	}

                        IdMapper idMapper = verCfg.getEntCfg().get(toEntityName).getIdMapper();

                        Serializable id = (Serializable) idMapper.mapToIdFromEntity(newValue);
                        verSync.addWorkUnit(new CollectionChangeWorkUnit(toEntityName, verCfg, id, newValue));
                    }

                    if (oldValue != null) {
                    	String toEntityName;
                    	if(oldValue instanceof HibernateProxy) {
                    	    HibernateProxy hibernateProxy = (HibernateProxy) oldValue;
                    	    toEntityName = session.bestGuessEntityName(oldValue);
                    	    oldValue = hibernateProxy.getHibernateLazyInitializer().getImplementation();
                    	} else {
                    		toEntityName =  session.guessEntityName(oldValue);
                    	}
                        
                        IdMapper idMapper = verCfg.getEntCfg().get(toEntityName).getIdMapper();

                        Serializable id = (Serializable) idMapper.mapToIdFromEntity(oldValue);
                        verSync.addWorkUnit(new CollectionChangeWorkUnit(toEntityName, verCfg, id, oldValue));
                    }
                }
            }
        }
    }

    public void onPostInsert(PostInsertEvent event) {
        String entityName = event.getPersister().getEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditSync verSync = verCfg.getSyncManager().get(event.getSession());

            verSync.addWorkUnit(new AddWorkUnit(event.getPersister().getEntityName(), verCfg, event.getId(),
                    event.getPersister(), event.getState()));

            generateBidirectionalCollectionChangeWorkUnits(verSync, event.getPersister(), entityName, event.getState(),
                    null, event.getSession());
        }
    }

    public void onPostUpdate(PostUpdateEvent event) {
        String entityName = event.getPersister().getEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditSync verSync = verCfg.getSyncManager().get(event.getSession());

            verSync.addWorkUnit(new ModWorkUnit(event.getPersister().getEntityName(), verCfg, event.getId(),
                    event.getPersister(), event.getState(), event.getOldState()));

            generateBidirectionalCollectionChangeWorkUnits(verSync, event.getPersister(), entityName, event.getState(),
                    event.getOldState(), event.getSession());
        }
    }

    public void onPostDelete(PostDeleteEvent event) {
        String entityName = event.getPersister().getEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditSync verSync = verCfg.getSyncManager().get(event.getSession());

            verSync.addWorkUnit(new DelWorkUnit(event.getPersister().getEntityName(), verCfg, event.getId()));

            generateBidirectionalCollectionChangeWorkUnits(verSync, event.getPersister(), entityName, null,
                    event.getDeletedState(), event.getSession());
        }
    }

    private void generateBidirectionalCollectionChangeWorkUnits(AuditSync verSync, AbstractCollectionEvent event,
                                                                PersistentCollectionChangeWorkUnit workUnit) {
        // Checking if this is enabled in configuration ...
        if (!verCfg.getGlobalCfg().isGenerateRevisionsForCollections()) {
            return;
        }

        // Checking if this is not a bidirectional relation - then, a revision needs also be generated for
        // the other side of the relation.
        RelationDescription relDesc = verCfg.getEntCfg().getRelationDescription(event.getAffectedOwnerEntityName(),
                workUnit.getReferencingPropertyName());

        // relDesc can be null if this is a collection of simple values (not a relation).
        if (relDesc != null && relDesc.isBidirectional()) {
            String relatedEntityName = relDesc.getToEntityName();
            IdMapper relatedIdMapper = verCfg.getEntCfg().get(relatedEntityName).getIdMapper();
            
            for (PersistentCollectionChangeData changeData : workUnit.getCollectionChanges()) {
                Object relatedObj = changeData.getChangedElement();
                Serializable relatedId = (Serializable) relatedIdMapper.mapToIdFromEntity(relatedObj);

                verSync.addWorkUnit(new CollectionChangeWorkUnit(relatedEntityName, verCfg, relatedId, relatedObj));
            }
        }
    }

    private void onCollectionAction(AbstractCollectionEvent event, PersistentCollection newColl, Serializable oldColl,
                                    CollectionEntry collectionEntry) {
        String entityName = event.getAffectedOwnerEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditSync verSync = verCfg.getSyncManager().get(event.getSession());

            PersistentCollectionChangeWorkUnit workUnit = new PersistentCollectionChangeWorkUnit(entityName, verCfg,
                    newColl, collectionEntry, oldColl, event.getAffectedOwnerIdOrNull());
            verSync.addWorkUnit(workUnit);

            if (workUnit.containsWork()) {
                // There are some changes: a revision needs also be generated for the collection owner
                verSync.addWorkUnit(new CollectionChangeWorkUnit(event.getAffectedOwnerEntityName(), verCfg,
                        event.getAffectedOwnerIdOrNull(), event.getAffectedOwnerOrNull()));

                generateBidirectionalCollectionChangeWorkUnits(verSync, event, workUnit);
            }
        }
    }

    private CollectionEntry getCollectionEntry(AbstractCollectionEvent event) {
        return event.getSession().getPersistenceContext().getCollectionEntry(event.getCollection());
    }

    public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
        CollectionEntry collectionEntry = getCollectionEntry(event);
        if (!collectionEntry.getLoadedPersister().isInverse()) {
            onCollectionAction(event, event.getCollection(), collectionEntry.getSnapshot(), collectionEntry);
        }
    }

    public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
        CollectionEntry collectionEntry = getCollectionEntry(event);
        if (!collectionEntry.getLoadedPersister().isInverse()) {
            onCollectionAction(event, null, collectionEntry.getSnapshot(), collectionEntry);
        }
    }

    public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
        CollectionEntry collectionEntry = getCollectionEntry(event);
        if (!collectionEntry.getLoadedPersister().isInverse()) {
            onCollectionAction(event, event.getCollection(), null, collectionEntry);
        }
    }

    @SuppressWarnings({"unchecked"})
    public void initialize(Configuration cfg) {
        verCfg = AuditConfiguration.getFor(cfg);
    }

    public AuditConfiguration getVerCfg() {
        return verCfg;
    }
}
