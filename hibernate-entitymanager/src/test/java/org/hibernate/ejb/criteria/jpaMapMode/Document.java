package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Sample Document metamodel object. To be replaced by the "real" one.
 * <p/>
 * One notable difference is that the "real" metamodel objects have synthetic properties that represent
 * relationships. This will ultimately map to Hibernate better than the model below, where the Hibernate
 * PersistentClass (PersistentDocumentClass) synthesizes properties for the relationships which do not
 * correspond to the metamodel very well at all.
 */
public final class Document {
    private final String name;
    private final Set<Property> properties = new HashSet<Property>();
    private final Label label;
    private final Set<Relationship> relationships = new HashSet<Relationship>();

    public Document(String name, Set<Property> properties, Label label) {
        this.name = name;
        if (properties != null) {
            this.properties.addAll(properties);
            for (Property property : properties) {
                property.setDocument(this);
            }
        }
        this.label = label;
    }

    public void addRelationship(Relationship relationship) {
        relationships.add(relationship);
    }

    public Set<Relationship> getRelationships() {
        return Collections.unmodifiableSet(relationships);
    }

    public String getName() {
        return name;
    }

    public Set<Property> getProperties() {
        return Collections.unmodifiableSet(properties);
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        return name == null ? -1 : name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Document)) {
            return false;
        }

        Document that = (Document) o;

        return this.name.equals(that.name);
    }
}