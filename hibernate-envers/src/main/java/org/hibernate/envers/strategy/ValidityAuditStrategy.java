package org.hibernate.envers.strategy;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.synchronization.SessionCacheCleaner;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.envers.tools.query.QueryBuilder;
import org.hibernate.property.Getter;

/**
 *  Audit strategy which persists and retrieves audit information using a validity algorithm, based on the 
 *  start-revision and end-revision of a row in the audit tables. 
 *  <p>This algorithm works as follows:
 *  <ul>
 *  <li>For a <strong>new row</strong> that is persisted in an audit table, only the <strong>start-revision</strong> column of that row is set</li>
 *  <li>At the same time the <strong>end-revision</strong> field of the <strong>previous</strong> audit row is set to this revision</li>
 *  <li>Queries are retrieved using 'between start and end revision', instead of a subquery.</li>
 *  </ul>
 *  </p>
 *  
 *  <p>
 *  This has a few important consequences that need to be judged against against each other:
 *  <ul>
 *  <li>Persisting audit information is a bit slower, because an extra row is updated</li>
 *  <li>Retrieving audit information is a lot faster</li>
 *  </ul>
 *  </p>
 * 
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 */
public class ValidityAuditStrategy implements AuditStrategy {

    /** getter for the revision entity field annotated with @RevisionTimestamp */
    private Getter revisionTimestampGetter = null;

    private final SessionCacheCleaner sessionCacheCleaner;

    public ValidityAuditStrategy() {
        sessionCacheCleaner = new SessionCacheCleaner();
    }

    public void perform(Session session, String entityName, AuditConfiguration auditCfg, Serializable id, Object data,
                        Object revision) {
        AuditEntitiesConfiguration audEntCfg = auditCfg.getAuditEntCfg();
        String auditedEntityName = audEntCfg.getAuditEntityName(entityName);

        // Update the end date of the previous row if this operation is expected to have a previous row
        if (getRevisionType(auditCfg, data) != RevisionType.ADD) {
            /*
             Constructing a query:
             select e from audited_ent e where e.end_rev is null and e.id = :id
             */

            QueryBuilder qb = new QueryBuilder(auditedEntityName, "e");

            // e.id = :id
            IdMapper idMapper = auditCfg.getEntCfg().get(entityName).getIdMapper();
            idMapper.addIdEqualsToQuery(qb.getRootParameters(), id, auditCfg.getAuditEntCfg().getOriginalIdPropName(), true);

            addEndRevisionNullRestriction(auditCfg, qb);

            @SuppressWarnings({"unchecked"})
            List<Object> l = qb.toQuery(session).setLockOptions(LockOptions.UPGRADE).list();

            updateLastRevision(session, auditCfg, l, id, auditedEntityName, revision);
        }

        // Save the audit data
        session.save(auditedEntityName, data);
        sessionCacheCleaner.scheduleAuditDataRemoval(session, data);
    }

    @SuppressWarnings({"unchecked"})
    public void performCollectionChange(Session session, AuditConfiguration auditCfg,
                                        PersistentCollectionChangeData persistentCollectionChangeData, Object revision) {

        final QueryBuilder qb = new QueryBuilder(persistentCollectionChangeData.getEntityName(), "e");

        // Adding a parameter for each id component, except the rev number
        final String originalIdPropName = auditCfg.getAuditEntCfg().getOriginalIdPropName();
        final Map<String, Object> originalId = (Map<String, Object>) persistentCollectionChangeData.getData().get(
                originalIdPropName);
        for (Map.Entry<String, Object> originalIdEntry : originalId.entrySet()) {
            if (!auditCfg.getAuditEntCfg().getRevisionFieldName().equals(originalIdEntry.getKey())) {
                qb.getRootParameters().addWhereWithParam(originalIdPropName + "." + originalIdEntry.getKey(),
                        true, "=", originalIdEntry.getValue());
            }
        }

        addEndRevisionNullRestriction(auditCfg, qb);

        final List<Object> l = qb.toQuery(session).setLockOptions(LockOptions.UPGRADE).list();

        // Update the last revision if one exists.
        // HHH-5967: with collections, the same element can be added and removed multiple times. So even if it's an
        // ADD, we may need to update the last revision.
        if (l.size() > 0) {
            updateLastRevision(session, auditCfg, l, originalId, persistentCollectionChangeData.getEntityName(), revision);
        }

        // Save the audit data
        session.save(persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData());
        sessionCacheCleaner.scheduleAuditDataRemoval(session, persistentCollectionChangeData.getData());
    }

    private void addEndRevisionNullRestriction(AuditConfiguration auditCfg, QueryBuilder qb) {
        // e.end_rev is null
        qb.getRootParameters().addWhere(auditCfg.getAuditEntCfg().getRevisionEndFieldName(), true, "is", "null", false);
    }

    public void addEntityAtRevisionRestriction(GlobalConfiguration globalCfg, QueryBuilder rootQueryBuilder,
			String revisionProperty,String revisionEndProperty, boolean addAlias,
            MiddleIdData idData, String revisionPropertyPath, String originalIdPropertyName,
            String alias1, String alias2) {
		Parameters rootParameters = rootQueryBuilder.getRootParameters();
		addRevisionRestriction(rootParameters, revisionProperty, revisionEndProperty, addAlias);
	}
	
	public void addAssociationAtRevisionRestriction(QueryBuilder rootQueryBuilder,  String revisionProperty, 
		    String revisionEndProperty, boolean addAlias, MiddleIdData referencingIdData, 
		    String versionsMiddleEntityName, String eeOriginalIdPropertyPath, String revisionPropertyPath,
		    String originalIdPropertyName, MiddleComponentData... componentDatas) {
		Parameters rootParameters = rootQueryBuilder.getRootParameters();
		addRevisionRestriction(rootParameters, revisionProperty, revisionEndProperty, addAlias);
	}
    
	public void setRevisionTimestampGetter(Getter revisionTimestampGetter) {
		this.revisionTimestampGetter = revisionTimestampGetter;
	}

    private void addRevisionRestriction(Parameters rootParameters,  
			String revisionProperty, String revisionEndProperty, boolean addAlias) {
    	
		// e.revision <= _revision and (e.endRevision > _revision or e.endRevision is null)
		Parameters subParm = rootParameters.addSubParameters("or");
		rootParameters.addWhereWithNamedParam(revisionProperty, addAlias, "<=", "revision");
		subParm.addWhereWithNamedParam(revisionEndProperty + ".id", addAlias, ">", "revision");
		subParm.addWhere(revisionEndProperty, addAlias, "is", "null", false);
	}

    @SuppressWarnings({"unchecked"})
    private RevisionType getRevisionType(AuditConfiguration auditCfg, Object data) {
        return (RevisionType) ((Map<String, Object>) data).get(auditCfg.getAuditEntCfg().getRevisionTypePropName());
    }

    @SuppressWarnings({"unchecked"})
    private void updateLastRevision(Session session, AuditConfiguration auditCfg, List<Object> l,
                                    Object id, String auditedEntityName, Object revision) {

        // There should be one entry
        if (l.size() == 1) {
            // Setting the end revision to be the current rev
            Object previousData = l.get(0);
            String revisionEndFieldName = auditCfg.getAuditEntCfg().getRevisionEndFieldName();
            ((Map<String, Object>) previousData).put(revisionEndFieldName, revision);

            if (auditCfg.getAuditEntCfg().isRevisionEndTimestampEnabled()) {
                // Determine the value of the revision property annotated with @RevisionTimestamp
            	Date revisionEndTimestamp;
            	String revEndTimestampFieldName = auditCfg.getAuditEntCfg().getRevisionEndTimestampFieldName();
            	Object revEndTimestampObj = this.revisionTimestampGetter.get(revision);

            	// convert to a java.util.Date
            	if (revEndTimestampObj instanceof Date) {
            		revisionEndTimestamp = (Date) revEndTimestampObj;
            	} else {
            		revisionEndTimestamp = new Date((Long) revEndTimestampObj);
            	}

            	// Setting the end revision timestamp
            	((Map<String, Object>) previousData).put(revEndTimestampFieldName, revisionEndTimestamp);
            }
            
            // Saving the previous version
            session.save(auditedEntityName, previousData);
            sessionCacheCleaner.scheduleAuditDataRemoval(session, previousData);
        } else {
            throw new RuntimeException("Cannot find previous revision for entity " + auditedEntityName + " and id " + id);
        }
    }
}

