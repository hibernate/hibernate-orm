package org.hibernate.ejb.criteria.jpaMapMode;

/**
 * A primitive version of a document property. This will be significantly enhanced for the production system, but
 * proves out what we need to know to make properties queryable from Hibernate.
 */
public final class Property {
    private Document document;
    private final String name;
    private final PropertyType type;
    private final Label label;

    public Property(String name, PropertyType type, Label label) {
        this.name = name;
        this.type = type;
        this.label = label;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        assert document != null;
        if (this.document != null) {
            throw new IllegalStateException("Document attribute already set.");
        }

        this.document = document;
    }

    public String getName() {
        return name;
    }

    public PropertyType getType() {
        return type;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Property)) {
            return false;
        }

        Property that = (Property) o;

        return this.name.equals(that.name) && this.type.equals(that.type);
    }

    @Override
    public String toString() {
        return name + label.getDefaultLabel();
    }
}
