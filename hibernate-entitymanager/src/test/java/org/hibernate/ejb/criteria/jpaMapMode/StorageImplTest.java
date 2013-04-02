package org.hibernate.ejb.criteria.jpaMapMode;


import java.util.HashSet;
import java.util.Set;

public class StorageImplTest {


    @SuppressWarnings("unchecked")
    public static MetaModel createSampleMetaModel() {
        Set<Document> documents = new HashSet<Document>();
        Set<Property> props = new HashSet<Property>();
        props.add(new Property("doubleProperty", PropertyType.DOUBLE, MultiTableSchemaGeneratorImplTest
            .usLabel("Double Property")));
        props.add(new Property("doubleProperty2", PropertyType.DOUBLE,
            MultiTableSchemaGeneratorImplTest
            .usLabel("Double Property2")));
        final Document document1 = new Document("aDocument", props,
            MultiTableSchemaGeneratorImplTest.usLabel("A document"));
        documents.add(document1);

        props = new HashSet<Property>();
        props.add(new Property("startDate", PropertyType.DATE,
            MultiTableSchemaGeneratorImplTest
            .usLabel("Start Date")));

        final Document document2 = new Document("anotherDocument", props,
            MultiTableSchemaGeneratorImplTest.usLabel("Another Document"));
        documents.add(document2);

        Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(new Relationship("aRelationship", document1, Multiplicity.ZERO_OR_ONE,
            "anotherDocument", document2, Multiplicity.ZERO_OR_MORE, "aDocuments"));

        MetaModel metaModel = new MetaModel("test", 0, documents, relationships);
        return metaModel;
    }


}