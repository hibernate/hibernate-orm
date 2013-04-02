package org.hibernate.ejb.criteria.jpaMapMode;

/**
 * An instance of a relationship between two documentInstances
 */
public class RelationshipInstance {
    private final Relationship relationship;
    private final Object value;

    public RelationshipInstance(Relationship relationship,
                                Object value) {
        this.relationship = relationship;
        this.value = value;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public Object getValue() {
        return value;
    }
}
