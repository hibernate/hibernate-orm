package org.hibernate.envers.synchronization.work;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.RelationDescription;
import org.hibernate.envers.RevisionType;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * A work unit that handles "fake" bidirectional one-to-many relations (mapped with {@code @OneToMany+@JoinColumn} and
 * {@code @ManyToOne+@Column(insertable=false, updatable=false)}.
 * @author Adam Warski (adam at warski dot org)
 */
public class FakeBidirectionalRelationWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
    private final Object owningEntity;
    private final RelationDescription rd;
    private final RevisionType revisionType;

    /*
     * The work unit responsible for generating the "raw" entity data to be saved.
     */
    private final AuditWorkUnit nestedWorkUnit;


    public FakeBidirectionalRelationWorkUnit(SessionImplementor sessionImplementor, String entityName,
                                             AuditConfiguration verCfg, Serializable id, Object owningEntity,
                                             RelationDescription rd, RevisionType revisionType,
                                             AuditWorkUnit nestedWorkUnit) {
        super(sessionImplementor, entityName, verCfg, id);


        this.owningEntity = owningEntity;
        this.rd = rd;
        this.revisionType = revisionType;
        this.nestedWorkUnit = nestedWorkUnit;
    }

    public FakeBidirectionalRelationWorkUnit(FakeBidirectionalRelationWorkUnit original, AuditWorkUnit nestedWorkUnit) {
        super(original.sessionImplementor, original.entityName, original.verCfg, original.id);

        this.owningEntity = original.owningEntity;
        this.rd = original.rd;
        this.revisionType = original.revisionType;
        this.nestedWorkUnit = nestedWorkUnit;
    }

    public AuditWorkUnit getNestedWorkUnit() {
        return nestedWorkUnit;
    }

    public RevisionType getRevisionType() {
        return revisionType;
    }

    public boolean containsWork() {
        return true;
    }

    public Map<String, Object> generateData(Object revisionData) {
        // Generating data with the nested work unit. This data contains all data except the fake relation.
        // Making a defensive copy not to modify the data held by the nested work unit.
        Map<String, Object> nestedData = new HashMap<String, Object>(nestedWorkUnit.generateData(revisionData));

        // Now adding data for the fake relation.
        // If the revision type is "DEL", it means that the object is removed from the collection. Then the
        // new owner will in fact be null.
        rd.getFakeBidirectionalRelationMapper().mapToMapFromEntity(sessionImplementor, nestedData,
                revisionType == RevisionType.DEL ? null : owningEntity, null);

        return nestedData;
    }

    public AuditWorkUnit merge(AddWorkUnit second) {
        return merge(this, nestedWorkUnit, second);
    }

    public AuditWorkUnit merge(ModWorkUnit second) {
        return merge(this, nestedWorkUnit, second);
    }

    public AuditWorkUnit merge(DelWorkUnit second) {
        return second;
    }

    public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
        return this;
    }

    public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
        /*
         * The merging rules are the following (revision types of the first and second work units):
         * - DEL, DEL - return any (the work units are the same)
         * - DEL, ADD - return ADD (points to new owner)
         * - ADD, DEL - return ADD (points to new owner)
         * - ADD, ADD - return second (points to newer owner)
         */

        if (revisionType == RevisionType.DEL || second.getRevisionType() == RevisionType.ADD) {
            return second;
        }

        return this;
    }

    public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
        return first.merge(this);
    }

    public static AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit frwu, AuditWorkUnit nestedFirst,
                                    AuditWorkUnit nestedSecond) {
        AuditWorkUnit nestedMerged = nestedSecond.dispatch(nestedFirst);

        // Creating a new fake relation work unit with the nested merged data
        return new FakeBidirectionalRelationWorkUnit(frwu, nestedMerged);
    }
}
