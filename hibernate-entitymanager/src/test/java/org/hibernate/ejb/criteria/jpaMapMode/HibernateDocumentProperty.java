package org.hibernate.ejb.criteria.jpaMapMode;

@SuppressWarnings("serial")
public class HibernateDocumentProperty extends HibernateProperty {
    private final PropertyColumnMapping propertyColumnMapping;

    public HibernateDocumentProperty(PropertyColumnMapping propertyColumnMapping) {
        this.propertyColumnMapping = propertyColumnMapping;
    }

    public PropertyColumnMapping getPropertyColumnMapping() {
        return propertyColumnMapping;
    }
}
