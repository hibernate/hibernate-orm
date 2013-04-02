package org.hibernate.ejb.criteria.jpaMapMode;

public class PropertyColumnMapping {
    private final Property property;
    private final Column column;

    public PropertyColumnMapping(Property property, Column column) {
        super();
        this.property = property;
        this.column = column;
    }

    public Property getProperty() {
        return property;
    }

    public Column getColumn() {
        return column;
    }

}
