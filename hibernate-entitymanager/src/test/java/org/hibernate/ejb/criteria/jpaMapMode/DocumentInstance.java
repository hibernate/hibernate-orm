package org.hibernate.ejb.criteria.jpaMapMode;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An instance of a Document type.
 */
public class DocumentInstance implements Serializable {
    private final Document document;
    private Map<Property, PropertyInstance> properties = new HashMap<Property, PropertyInstance>();
    private Map<Relationship, RelationshipInstance> relationshipInstances = new HashMap<Relationship, RelationshipInstance>();
    private UUID id;

    public DocumentInstance(Document document) {
        this.document = document;
    }

    public DocumentInstance(Document document, UUID id,
                            Map<Property, PropertyInstance> properties,
                            Map<Relationship, RelationshipInstance> relationshipInstances) {
        this.document = document;
        this.id = id;
        this.properties = properties;
        this.relationshipInstances = new HashMap<Relationship, RelationshipInstance>(relationshipInstances);
    }

    public Document getDocument() {
        return document;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        assert this.id == null;
        assert id != null;
        this.id = id;
    }

    public PropertyInstance getPropertyInstance(Property property) {
        return properties.get(property);
    }

    public void setPropertyInstance(Property property,
                                    PropertyInstance propertyInstance) {
        assert document.getProperties().contains(property);
        properties.put(property, propertyInstance);
    }

    public Map<Property, PropertyInstance> getPropertyInstances() {
        return Collections.unmodifiableMap(properties);
    }

    public RelationshipInstance getRelationshipInstance(Relationship relationship) {
        return relationshipInstances.get(relationship);
    }

    public void setRelationshipInstance(Relationship relationship, RelationshipInstance relationshipInstance) {
        relationshipInstances.put(relationship, relationshipInstance);
    }
}