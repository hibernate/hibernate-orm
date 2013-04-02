package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.ejb.criteria.jpaMapMode.Property;

/**
 * An instance of a property of a document.
 */
public class PropertyInstance {
    private DocumentInstance documentInstance;
    private final Property property;
    private final Object value;

    public PropertyInstance(Property property, Object value) {
        this.property = property;
        assert value == null || property.getType().getJavaTypeForPropertyType().isAssignableFrom(value.getClass());
        this.value = value;
    }

    public DocumentInstance getDocumentInstance() {
        return documentInstance;
    }

    void setDocumentInstance(DocumentInstance documentInstance) {
        this.documentInstance = documentInstance;
    }

    public Object getValue() {
        return value;
    }

    public Property getProperty() {
        return property;
    }
}
