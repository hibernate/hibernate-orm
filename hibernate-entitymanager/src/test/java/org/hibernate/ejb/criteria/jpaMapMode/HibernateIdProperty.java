package org.hibernate.ejb.criteria.jpaMapMode;

public class HibernateIdProperty extends HibernateProperty {
    private final DocumentTableMapping documentTableMapping;

    public HibernateIdProperty(DocumentTableMapping documentTableMapping) {
        this.documentTableMapping = documentTableMapping;
    }

    public DocumentTableMapping getDocumentTableMapping() {
        return documentTableMapping;
    }
}
