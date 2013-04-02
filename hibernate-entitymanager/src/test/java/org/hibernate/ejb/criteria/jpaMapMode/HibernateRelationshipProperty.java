package org.hibernate.ejb.criteria.jpaMapMode;

public class HibernateRelationshipProperty extends HibernateProperty {
    private final RelationshipColumnMapping relationshipColumnMapping;

    public HibernateRelationshipProperty(RelationshipColumnMapping relationshipColumnMapping) {
        this.relationshipColumnMapping = relationshipColumnMapping;
    }

    public RelationshipColumnMapping getRelationshipColumnMapping() {
        return relationshipColumnMapping;
    }
}
