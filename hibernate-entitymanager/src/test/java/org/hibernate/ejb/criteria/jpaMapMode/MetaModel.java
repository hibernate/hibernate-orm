package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.*;

/**
 * A primitive wrapper for the entire metamodel. To be replaced by Jason's metamodel.
 */
public final class MetaModel {
    private final String name;
    private final int version;
    private final Map<String, Document> documents = new HashMap<String, Document>();
    private final Set<Relationship> relationships = new HashSet<Relationship>();

    public MetaModel(String name, int version, Set<Document> documents, Set<Relationship> relationships) {
        this.name = name;
        this.version = version;
        if (documents != null) {
            for (Document document : documents) {
                Document result = this.documents.put(document.getName(), document);
                if (result != null) {
                    throw new IllegalStateException("Document names must be unique: " + document.getName());
                }
            }
        }

        if (relationships != null) {
            this.relationships.addAll(relationships);
        }
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public Set<Document> getDocuments() {
        return Collections.unmodifiableSet(new HashSet<Document>(documents.values()));
    }

    public Document getDocument(String name) {
        return documents.get(name);
    }

    public Set<Relationship> getRelationships() {
        return Collections.unmodifiableSet(relationships);
    }
}
