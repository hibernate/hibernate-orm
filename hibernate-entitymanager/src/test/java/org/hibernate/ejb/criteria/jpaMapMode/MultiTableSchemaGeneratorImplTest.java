package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.*;

@SuppressWarnings("unchecked")
public class MultiTableSchemaGeneratorImplTest  {


    public static MetaModel createSampleMetaModel() {
        Set<Document> documents = new HashSet<Document>();
        Set<Property> props = new HashSet<Property>();
        props.add(new Property("textProperty", PropertyType.TEXT, usLabel("Text Property")));
        props.add(new Property("dateProperty", PropertyType.DATE, usLabel("Date Property")));
        props.add(new Property("dateTimeProperty", PropertyType.DATETIME, usLabel("Date Time Property")));
        props.add(new Property("doubleProperty", PropertyType.DOUBLE, usLabel("Double Property")));
        props.add(new Property("longProperty", PropertyType.LONG, usLabel("Long Property")));
        props.add(new Property("multiLineProperty", PropertyType.MULTILINE_TEXT, usLabel("Multi-Line Text Property")));
        documents.add(new Document("aDocument", props, usLabel("A document")));

        MetaModel metaModel = new MetaModel("test", 0, documents, null);
        return metaModel;
    }

    public static Label usLabel(String string) {
        Map<Locale, String> map = new HashMap<Locale, String>(1);
        map.put(Locale.US, string);
        return new Label(map);
    }

}
