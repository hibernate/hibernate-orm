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
import java.util.List;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.RelationDescription;
import org.hibernate.envers.entities.RelationType;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.synchronization.AuditProcess;
import org.hibernate.envers.synchronization.work.*;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.RevisionType;

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

    private void generateBidirectionalCollectionChangeWorkUnits(AuditProcess auditProcess, EntityPersister entityPersister,
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
            if (relDesc != null && relDesc.isBidirectional() && relDesc.getRelationType() == RelationType.TO_ONE &&
                    relDesc.isInsertable()) {
                // Checking for changes
                Object oldValue = oldState == null ? null : oldState[i];
                Object newValue = newState == null ? null : newState[i];

                if (!Tools.entitiesEqual(session, oldValue, newValue)) {
                    // We have to generate changes both in the old collection (size decreses) and new collection
                    // (size increases).
                    if (newValue != null) {
                        // relDesc.getToEntityName() doesn't always return the entity name of the value - in case
                        // of subclasses, this will be root class, no the actual class. So it can't be used here.
                        String toEntityName;
						Serializable id;

                        if (newValue instanceof HibernateProxy) {
                    	    HibernateProxy hibernateProxy = (HibernateProxy) newValue;
                    	    toEntityName = session.bestGuessEntityName(newValue);
                    	    id = hibernateProxy.getHibernateLazyInitializer().getIdentifier();
							// We've got to initialize the object from the proxy to later read its state.   
							newValue = Tools.getTargetFromProxy(session.getFactory(), hibernateProxy);
                    	} else {
                    		toEntityName =  session.guessEntityName(newValue);

							IdMapper idMapper = verCfg.getEntCfg().get(toEntityName).getIdMapper();
                         	id = (Serializable) idMapper.mapToIdFromEntity(newValue);
                    	}

                        auditProcess.addWorkUnit(new CollectionChangeWorkUnit(session, toEntityName, verCfg, id, newValue));
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

							IdMapper idMapper = verCfg.getEntCfg().get(toEntityName).getIdMapper();
							id = (Serializable) idMapper.mapToIdFromEntity(oldValue);
                    	}
						
                        auditProcess.addWorkUnit(new CollectionChangeWorkUnit(session, toEntityName, verCfg, id, oldValue));
                    }
                }
            }
        }
    }

    public void onPostInsert(PostInsertEvent event) {
        String entityName = event.getPersister().getEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditProcess auditProcess = verCfg.getSyncManager().get(event.getSession());

            AuditWorkUnit workUnit = new AddWorkUnit(event.getSession(), event.getPersister().getEntityName(), verCfg,
                    event.getId(), event.getPersister(), event.getState());
            auditProcess.addWorkUnit(workUnit);

            if (workUnit.containsWork()) {
                generateBidirectionalCollectionChangeWorkUnits(auditProcess, event.getPersister(), entityName, event.getState(),
                        null, event.getSession());
            }
        }
    }

    public void onPostUpdate(PostUpdateEvent event) {
        String entityName = event.getPersister().getEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditProcess auditProcess = verCfg.getSyncManager().get(event.getSession());

            AuditWorkUnit workUnit = new ModWorkUnit(event.getSession(), event.getPersister().getEntityName(), verCfg,
                    event.getId(), event.getPersister(), event.getState(), event.getOldState());
            auditProcess.addWorkUnit(workUnit);

            if (workUnit.containsWork()) {
                generateBidirectionalCollectionChangeWorkUnits(auditProcess, event.getPersister(), entityName, event.getState(),
                        event.getOldState(), event.getSession());
            }
        }
    }

    public void onPostDelete(PostDeleteEvent event) {
        String entityName = event.getPersister().getEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditProcess auditProcess = verCfg.getSyncManager().get(event.getSession());

            AuditWorkUnit workUnit = new DelWorkUnit(event.getSession(), event.getPersister().getEntityName(), verCfg,
                    event.getId(), event.getPersister(), event.getDeletedState());
            auditProcess.addWorkUnit(workUnit);

            if (workUnit.containsWork()) {
                generateBidirectionalCollectionChangeWorkUnits(auditProcess, event.getPersister(), entityName, null,
                        event.getDeletedState(), event.getSession());
            }
        }
    }

    private void generateBidirectionalCollectionChangeWorkUnits(AuditProcess auditProcess, AbstractCollectionEvent event,
                                                                PersistentCollectionChangeWorkUnit workUnit,
                                                                RelationDescription rd) {
        // Checking if this is enabled in configuration ...
        if (!verCfg.getGlobalCfg().isGenerateRevisionsForCollections()) {
            return;
        }

        // Checking if this is not a bidirectional relation - then, a revision needs also be generated for
        // the other side of the relation.
        // relDesc can be null if this is a collection of simple values (not a relation).
        if (rd != null && rd.isBidirectional()) {
            String relatedEntityName = rd.getToEntityName();
            IdMapper relatedIdMapper = verCfg.getEntCfg().get(relatedEntityName).getIdMapper();
            
            for (PersistentCollectionChangeData changeData : workUnit.getCollectionChanges()) {
                Object relatedObj = changeData.getChangedElement();
                Serializable relatedId = (Serializable) relatedIdMapper.mapToIdFromEntity(relatedObj);

                auditProcess.addWorkUnit(new CollectionChangeWorkUnit(event.getSession(), relatedEntityName, verCfg,
						relatedId, relatedObj));
            }
        }
    }

    private void generateFakeBidirecationalRelationWorkUnits(AuditProcess auditProcess, PersistentCollection newColl, Serializable oldColl,
                                                             String collectionEntityName, String referencingPropertyName,
                                                             AbstractCollectionEvent event,
                                                             RelationDescription rd) {
        // First computing the relation changes
        List<PersistentCollectionChangeData> collectionChanges = verCfg.getEntCfg().get(collectionEntityName).getPropertyMapper()
                .mapCollectionChanges(referencingPropertyName, newColl, oldColl, event.getAffectedOwnerIdOrNull());

        // Getting the id mapper for the related entity, as the work units generated will corrspond to the related
        // entities.
        String relatedEntityName = rd.getToEntityName();
        IdMapper relatedIdMapper = verCfg.getEntCfg().get(relatedEntityName).getIdMapper();

        // For each collection change, generating the bidirectional work unit.
        for (PersistentCollectionChangeData changeData : collectionChanges) {
            Object relatedObj = changeData.getChangedElement();
            Serializable relatedId = (Serializable) relatedIdMapper.mapToIdFromEntity(relatedObj);
            RevisionType revType = (RevisionType) changeData.getData().get(verCfg.getAuditEntCfg().getRevisionTypePropName());

            // This can be different from relatedEntityName, in case of inheritance (the real entity may be a subclass
            // of relatedEntityName).
            String realRelatedEntityName = event.getSession().bestGuessEntityName(relatedObj);

            // By default, the nested work unit is a collection change work unit.
            AuditWorkUnit nestedWorkUnit = new CollectionChangeWorkUnit(event.getSession(), realRelatedEntityName, verCfg,
                    relatedId, relatedObj);

            auditProcess.addWorkUnit(new FakeBidirectionalRelationWorkUnit(event.getSession(), realRelatedEntityName, verCfg,
                    relatedId, referencingPropertyName, event.getAffectedOwnerOrNull(), rd, revType,
                    changeData.getChangedElementIndex(), nestedWorkUnit));
        }

        // We also have to generate a collection change work unit for the owning entity.
        auditProcess.addWorkUnit(new CollectionChangeWorkUnit(event.getSession(), collectionEntityName, verCfg,
                event.getAffectedOwnerIdOrNull(), event.getAffectedOwnerOrNull()));
    }

    private void onCollectionAction(AbstractCollectionEvent event, PersistentCollection newColl, Serializable oldColl,
                                    CollectionEntry collectionEntry) {
        String entityName = event.getAffectedOwnerEntityName();

        if (verCfg.getEntCfg().isVersioned(entityName)) {
            AuditProcess auditProcess = verCfg.getSyncManager().get(event.getSession());

            String ownerEntityName = ((AbstractCollectionPersister) collectionEntry.getLoadedPersister()).getOwnerEntityName();
            String referencingPropertyName = collectionEntry.getRole().substring(ownerEntityName.length() + 1);

            // Checking if this is not a "fake" many-to-one bidirectional relation. The relation description may be
            // null in case of collections of non-entities.
            RelationDescription rd = verCfg.getEntCfg().get(entityName).getRelationDescription(referencingPropertyName);
            if (rd != null && rd.getMappedByPropertyName() != null) {
                generateFakeBidirecationalRelationWorkUnits(auditProcess, newColl, oldColl, entityName,
                        referencingPropertyName, event, rd);
            } else {
                PersistentCollectionChangeWorkUnit workUnit = new PersistentCollectionChangeWorkUnit(event.getSession(),
                        entityName, verCfg, newColl, collectionEntry, oldColl, event.getAffectedOwnerIdOrNull(),
                        referencingPropertyName);
                auditProcess.addWorkUnit(workUnit);

                if (workUnit.containsWork()) {
                    // There are some changes: a revision needs also be generated for the collection owner
                    auditProcess.addWorkUnit(new CollectionChangeWorkUnit(event.getSession(), event.getAffectedOwnerEntityName(),
                            verCfg, event.getAffectedOwnerIdOrNull(), event.getAffectedOwnerOrNull()));

                    generateBidirectionalCollectionChangeWorkUnits(auditProcess, event, workUnit, rd);
                }
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
        if (collectionEntry != null && !collectionEntry.getLoadedPersister().isInverse()) {
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
