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
package org.hibernate.envers.reader;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQueryCreator;
import org.hibernate.envers.synchronization.AuditProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.proxy.HibernateProxy;

import static org.hibernate.envers.tools.ArgumentsTools.checkNotNull;
import static org.hibernate.envers.tools.ArgumentsTools.checkPositive;
import static org.hibernate.envers.tools.Tools.getTargetClassIfProxied;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AuditReaderImpl implements AuditReaderImplementor {
    private final AuditConfiguration verCfg;
    private final SessionImplementor sessionImplementor;
    private final Session session;
    private final FirstLevelCache firstLevelCache;
    private final CrossTypeRevisionChangesReader crossTypeRevisionChangesReader;

    public AuditReaderImpl(AuditConfiguration verCfg, Session session,
                              SessionImplementor sessionImplementor) {
        this.verCfg = verCfg;
        this.sessionImplementor = sessionImplementor;
        this.session = session;

        firstLevelCache = new FirstLevelCache();
        crossTypeRevisionChangesReader = new CrossTypeRevisionChangesReaderImpl(this, verCfg);
    }

    private void checkSession() {
        if (!session.isOpen()) {
            throw new IllegalStateException("The associated entity manager is closed!");
        }
    }

    public SessionImplementor getSessionImplementor() {
        return sessionImplementor;
    }

    public Session getSession() {
        return session;
    }

    public FirstLevelCache getFirstLevelCache() {
        return firstLevelCache;
    }

    public <T> T find(Class<T> cls, Object primaryKey, Number revision) throws
            IllegalArgumentException, NotAuditedException, IllegalStateException {
    	cls = getTargetClassIfProxied(cls);
    	return this.find(cls, cls.getName(), primaryKey, revision);
    }

    public <T> T find(Class<T> cls, String entityName, Object primaryKey, Number revision)
            throws IllegalArgumentException, NotAuditedException, IllegalStateException {
        return this.find(cls, entityName, primaryKey, revision, false);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T find(Class<T> cls, String entityName, Object primaryKey, Number revision,
        boolean includeDeletions) throws
            IllegalArgumentException, NotAuditedException, IllegalStateException {
        cls = getTargetClassIfProxied(cls);
        checkNotNull(cls, "Entity class");
        checkNotNull(entityName, "Entity name");
        checkNotNull(primaryKey, "Primary key");
        checkNotNull(revision, "Entity revision");
        checkPositive(revision, "Entity revision");
        checkSession();

        if (!verCfg.getEntCfg().isVersioned(entityName)) {
            throw new NotAuditedException(entityName, entityName + " is not versioned!");
        }

        if (firstLevelCache.contains(entityName, revision, primaryKey)) {
            return (T) firstLevelCache.get(entityName, revision, primaryKey);
        }

        Object result;
        try {
            // The result is put into the cache by the entity instantiator called from the query
            result = createQuery().forEntitiesAtRevision(cls, entityName, revision, includeDeletions)
                .add(AuditEntity.id().eq(primaryKey)).getSingleResult();
        } catch (NoResultException e) {
            result = null;
        } catch (NonUniqueResultException e) {
            throw new AuditException(e);
        }

        return (T) result;
    }    

    public List<Number> getRevisions(Class<?> cls, Object primaryKey)
            throws IllegalArgumentException, NotAuditedException, IllegalStateException {
    	cls = getTargetClassIfProxied(cls);
    	return this.getRevisions(cls, cls.getName(), primaryKey);
    }

    @SuppressWarnings({"unchecked"})
    public List<Number> getRevisions(Class<?> cls, String entityName, Object primaryKey)
            throws IllegalArgumentException, NotAuditedException, IllegalStateException {
        // todo: if a class is not versioned from the beginning, there's a missing ADD rev - what then?
        cls = getTargetClassIfProxied(cls);
        checkNotNull(cls, "Entity class");
        checkNotNull(entityName, "Entity name");
        checkNotNull(primaryKey, "Primary key");
        checkSession();

        if (!verCfg.getEntCfg().isVersioned(entityName)) {
            throw new NotAuditedException(entityName, entityName + " is not versioned!");
        }

        return createQuery().forRevisionsOfEntity(cls, entityName, false, true)
                .addProjection(AuditEntity.revisionNumber())
				.addOrder(AuditEntity.revisionNumber().asc())
                .add(AuditEntity.id().eq(primaryKey))
                .getResultList();
    }

    public Date getRevisionDate(Number revision) throws IllegalArgumentException, RevisionDoesNotExistException,
            IllegalStateException{
        checkNotNull(revision, "Entity revision");
        checkPositive(revision, "Entity revision");
        checkSession();

        Criteria query = verCfg.getRevisionInfoQueryCreator().getRevisionDateQuery(session, revision);

        try {
            Object timestampObject = query.uniqueResult();
            if (timestampObject == null) {
                throw new RevisionDoesNotExistException(revision);
            }

            // The timestamp object is either a date or a long
            return timestampObject instanceof Date ? (Date) timestampObject : new Date((Long) timestampObject);
        } catch (NonUniqueResultException e) {
            throw new AuditException(e);
        }
    }

    public Number getRevisionNumberForDate(Date date) {
        checkNotNull(date, "Date of revision");
        checkSession();

        Criteria query = verCfg.getRevisionInfoQueryCreator().getRevisionNumberForDateQuery(session, date);

        try {
            Number res = (Number) query.uniqueResult();
            if (res == null) {
                throw new RevisionDoesNotExistException(date);
            }

            return res;
        } catch (NonUniqueResultException e) {
            throw new AuditException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T> T findRevision(Class<T> revisionEntityClass, Number revision) throws IllegalArgumentException,
            RevisionDoesNotExistException, IllegalStateException {
        revisionEntityClass = getTargetClassIfProxied(revisionEntityClass);
        checkNotNull(revision, "Entity revision");
        checkPositive(revision, "Entity revision");
        checkSession();

        Set<Number> revisions = new HashSet<Number>(1);
        revisions.add(revision);
        Criteria query = verCfg.getRevisionInfoQueryCreator().getRevisionsQuery(session, revisions);

        try {
            T revisionData = (T) query.uniqueResult();

            if (revisionData == null) {
                throw new RevisionDoesNotExistException(revision);
            }

            return revisionData;
        } catch (NonUniqueResultException e) {
            throw new AuditException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T> Map<Number, T> findRevisions(Class<T> revisionEntityClass, Set<Number> revisions) throws IllegalArgumentException,
    IllegalStateException {
        revisionEntityClass = getTargetClassIfProxied(revisionEntityClass);
		Map<Number, T> result = new HashMap<Number, T>(revisions.size());

    	for (Number revision : revisions) {
            checkNotNull(revision, "Entity revision");
            checkPositive(revision, "Entity revision");
		}
        checkSession();

        Criteria query = verCfg.getRevisionInfoQueryCreator().getRevisionsQuery(session, revisions);

        try {
            List<T> revisionList = query.list();
            for (T revision : revisionList) {
            	Number revNo = verCfg.getRevisionInfoNumberReader().getRevisionNumber(revision);
       			result.put(revNo, revision);
			}

            return result;
        } catch (HibernateException e) {
            throw new AuditException(e);
        }
    }

    public CrossTypeRevisionChangesReader getCrossTypeRevisionChangesReader() throws AuditException {
        if (!verCfg.getGlobalCfg().isTrackEntitiesChangedInRevisionEnabled()) {
            throw new AuditException("This API is designed for Envers default mechanism of tracking entities modified in a given revision."
                                     + " Extend DefaultTrackingModifiedEntitiesRevisionEntity, utilize @ModifiedEntityNames annotation or set "
                                     + "'org.hibernate.envers.track_entities_changed_in_revision' parameter to true.");
        }
        return crossTypeRevisionChangesReader;
    }

	@SuppressWarnings({"unchecked"})
	public <T> T getCurrentRevision(Class<T> revisionEntityClass, boolean persist) {
        revisionEntityClass = getTargetClassIfProxied(revisionEntityClass);
		if (!(session instanceof EventSource)) {
			throw new IllegalArgumentException("The provided session is not an EventSource!");
		}

		// Obtaining the current audit sync
		AuditProcess auditProcess = verCfg.getSyncManager().get((EventSource) session);

		// And getting the current revision data
		return (T) auditProcess.getCurrentRevisionData(session, persist);
	}

	public AuditQueryCreator createQuery() {
        return new AuditQueryCreator(verCfg, this);
    }
	
    public boolean isEntityClassAudited(Class<?> entityClass) {
        entityClass = getTargetClassIfProxied(entityClass);
    	return this.isEntityNameAudited(entityClass.getName());
    }


	public boolean isEntityNameAudited(String entityName) {
        checkNotNull(entityName, "Entity name");
        checkSession();
        return (verCfg.getEntCfg().isVersioned(entityName));
    }	


	public String getEntityName(Object primaryKey, Number revision ,Object entity) throws HibernateException{
        checkNotNull(primaryKey, "Primary key");
        checkNotNull(revision, "Entity revision");
        checkPositive(revision, "Entity revision");
        checkNotNull(entity, "Entity");
        checkSession();

		// Unwrap if necessary
		if(entity instanceof HibernateProxy) {
			entity = ((HibernateProxy)entity).getHibernateLazyInitializer().getImplementation();
		}
		if(firstLevelCache.containsEntityName(primaryKey, revision, entity)) {
			// it's on envers FLC!
			return firstLevelCache.getFromEntityNameCache(primaryKey, revision, entity);
		} else {
			throw new HibernateException(
						"Envers can't resolve entityName for historic entity. The id, revision and entity is not on envers first level cache.");
    }	
}
}