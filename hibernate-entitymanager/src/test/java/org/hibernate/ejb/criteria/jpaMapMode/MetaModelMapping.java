package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.*;

public class MetaModelMapping {
    private final MetaModel metaModel;
    private final Set<Table> tables = new HashSet<Table>();
    private final Map<Document, DocumentTableMapping> documentTableMappings = new HashMap<Document, DocumentTableMapping>();
    private final Map<Relationship, RelationshipColumnMapping> relationshipColumnMappings = new HashMap<Relationship, RelationshipColumnMapping>();

    public MetaModelMapping(MetaModel metaModel) {
        super();
        this.metaModel = metaModel;
    }

    public MetaModel getMetaModel() {
        return metaModel;
    }

    public void addTable(Table table) {
        tables.add(table);
    }

    public Set<Table> getTables() {
        return Collections.unmodifiableSet(tables);
    }


    public void addDocumentTableMapping(
            DocumentTableMapping documentTableMapping) {
        documentTableMappings.put(documentTableMapping.getDocument(),
                documentTableMapping);
    }

    public void addRelationshipColumnMapping(RelationshipColumnMapping relationshipColumnMapping) {
        relationshipColumnMappings.put(relationshipColumnMapping.getRelationship(), relationshipColumnMapping);
    }

    public Map<Relationship, RelationshipColumnMapping> getRelationshipColumnMappings() {
        return Collections.unmodifiableMap(relationshipColumnMappings);
    }

    public Map<Document, DocumentTableMapping> getDocumentTableMappings() {
        return Collections.unmodifiableMap(documentTableMappings);
    }
}
