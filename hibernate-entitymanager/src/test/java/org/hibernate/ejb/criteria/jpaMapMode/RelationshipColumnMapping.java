package org.hibernate.ejb.criteria.jpaMapMode;

/**
 * Maps a relationship to a column. The 'side' field indicates which side of the relationship has the column.
 */
public class RelationshipColumnMapping {
    private final Relationship relationship;
    private final org.hibernate.ejb.criteria.jpaMapMode.DocumentTableMapping documentTableMapping;
    private final Column column;
    private final Relationship.Side side;

    public RelationshipColumnMapping(Relationship relationship, org.hibernate.ejb.criteria.jpaMapMode.DocumentTableMapping documentTableMapping, Column column, Relationship.Side side) {
        this.relationship = relationship;
        this.documentTableMapping = documentTableMapping;
        this.column = column;
        this.side = side;
    }

    public Relationship getRelationship() {
        return relationship;
    }

    public org.hibernate.ejb.criteria.jpaMapMode.DocumentTableMapping getDocumentTableMapping() {
        return documentTableMapping;
    }

    public Column getColumn() {
        return column;
    }

    public Relationship.Side getSide() {
        return side;
    }
}
