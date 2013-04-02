package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DocumentTableMapping {
    private final Document document;
    private Map<Property, PropertyColumnMapping> propertyColumnMappings;
    private final Table table;

    public DocumentTableMapping(Document document,
                                Set<PropertyColumnMapping> propertyColumnMappings, Table table) {
        super();
        this.document = document;
        setPropertyMappings(propertyColumnMappings);
        this.table = table;
        assert table.getDocumentMapping() == null;
        table.setDocumentMapping(this);
    }

    public Map<Property, PropertyColumnMapping> getPropertyMappings() {
        return Collections.unmodifiableMap(propertyColumnMappings);
    }

    public void setPropertyMappings(Set<PropertyColumnMapping> propertyMappings) {
        propertyColumnMappings = new HashMap<Property, PropertyColumnMapping>();
        for (PropertyColumnMapping propertyColumnMapping : propertyMappings) {
            propertyColumnMappings.put(propertyColumnMapping.getProperty(),
                    propertyColumnMapping);
        }
    }

    public Document getDocument() {
        return document;
    }

    public Table getTable() {
        return table;
    }

    public PropertyColumnMapping getPropertyMapping(Property property) {
        return propertyColumnMappings.get(property);
    }
}
