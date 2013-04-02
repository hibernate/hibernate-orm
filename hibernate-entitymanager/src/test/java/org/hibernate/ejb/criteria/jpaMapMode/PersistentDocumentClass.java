package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.mapping.RootClass;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a DocumentTableMapping for Hibernate.
 */
@SuppressWarnings("serial")
public class PersistentDocumentClass extends RootClass {
    private final DocumentTableMapping documentTableMapping;
    private final Set<RelationshipColumnMapping> relationships = new HashSet<RelationshipColumnMapping>();

    public PersistentDocumentClass(DocumentTableMapping documentTableMapping) {
        super();
        this.documentTableMapping = documentTableMapping;
        String name = documentTableMapping.getDocument().getName();
        this.setEntityName(name);
        this.setJpaEntityName(name);
    }

    public DocumentTableMapping getDocumentTableMapping() {
        return documentTableMapping;
    }

    public void addRelationship(RelationshipColumnMapping relationship) {
        relationships.add(relationship);
    }

    public Set<RelationshipColumnMapping> getRelationships() {
        return relationships;
    }

}
