package org.hibernate.envers.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.query.criteria.RevisionTypeAuditExpression;
import org.hibernate.envers.tools.Pair;
import org.hibernate.envers.tools.Tools;

import static org.hibernate.envers.tools.ArgumentsTools.checkNotNull;
import static org.hibernate.envers.tools.ArgumentsTools.checkPositive;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CrossTypeRevisionChangesReaderImpl implements CrossTypeRevisionChangesReader {
    private final AuditReaderImplementor auditReaderImplementor;
    private final AuditConfiguration verCfg;

    public CrossTypeRevisionChangesReaderImpl(AuditReaderImplementor auditReaderImplementor, AuditConfiguration verCfg) {
        this.auditReaderImplementor = auditReaderImplementor;
        this.verCfg = verCfg;
    }

    @SuppressWarnings({"unchecked"})
    public List<Object> findEntities(Number revision) throws IllegalStateException, IllegalArgumentException {
        Set<Pair<String, Class>> entityTypes = findEntityTypes(revision);
        List<Object> result = new ArrayList<Object>();
        for (Pair<String, Class> type : entityTypes) {
            result.addAll(auditReaderImplementor.createQuery().forEntitiesModifiedAtRevision(type.getSecond(), type.getFirst(), revision)
                                                              .getResultList());
        }
        return result;
    }

    @SuppressWarnings({"unchecked"})
    public List<Object> findEntities(Number revision, RevisionType revisionType) throws IllegalStateException,
                                                                                        IllegalArgumentException {
        Set<Pair<String, Class>> entityTypes = findEntityTypes(revision);
        List<Object> result = new ArrayList<Object>();
        for (Pair<String, Class> type : entityTypes) {
            result.addAll(auditReaderImplementor.createQuery().forEntitiesModifiedAtRevision(type.getSecond(), type.getFirst(), revision)
                                                              .add(new RevisionTypeAuditExpression(revisionType, "=")).getResultList());
        }
        return result;
    }

    @SuppressWarnings({"unchecked"})
    public Map<RevisionType, List<Object>> findEntitiesGroupByRevisionType(Number revision) throws IllegalStateException,
                                                                                                   IllegalArgumentException {
        Set<Pair<String, Class>> entityTypes = findEntityTypes(revision);
        Map<RevisionType, List<Object>> result = new HashMap<RevisionType, List<Object>>();
        for (RevisionType revisionType : RevisionType.values()) {
            result.put(revisionType, new ArrayList<Object>());
            for (Pair<String, Class> type : entityTypes) {
                List<Object> list = auditReaderImplementor.createQuery().forEntitiesModifiedAtRevision(type.getSecond(), type.getFirst(), revision)
                                                                        .add(new RevisionTypeAuditExpression(revisionType, "=")).getResultList();
                result.get(revisionType).addAll(list);
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked"})
    public Set<Pair<String, Class>> findEntityTypes(Number revision) throws IllegalStateException, IllegalArgumentException {
        checkNotNull(revision, "Entity revision");
        checkPositive(revision, "Entity revision");
        checkSession();

        Session session = auditReaderImplementor.getSession();
        SessionImplementor sessionImplementor = auditReaderImplementor.getSessionImplementor();

        Set<Number> revisions = new HashSet<Number>(1);
        revisions.add(revision);
        Criteria query = verCfg.getRevisionInfoQueryCreator().getRevisionsQuery(session, revisions);
        Object revisionInfo = query.uniqueResult();

        if (revisionInfo != null) {
            // If revision exists.
            Set<String> entityNames = verCfg.getModifiedEntityNamesReader().getModifiedEntityNames(revisionInfo);
            if (entityNames != null) {
                // Generate result that contains entity names and corresponding Java classes.
                Set<Pair<String, Class>> result = new HashSet<Pair<String, Class>>();
                for (String entityName : entityNames) {
                    result.add(Pair.make(entityName, Tools.getEntityClass(sessionImplementor, session, entityName)));
                }
                return result;
            }
        }

        return Collections.EMPTY_SET;
    }

    private void checkSession() {
        if (!auditReaderImplementor.getSession().isOpen()) {
            throw new IllegalStateException("The associated entity manager is closed!");
        }
    }
}
