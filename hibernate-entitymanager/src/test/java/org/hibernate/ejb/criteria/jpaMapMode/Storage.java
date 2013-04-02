package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.List;

public interface Storage {
    DocumentInstance find(Document type, List<Property> properties, String id);

    DocumentInstance store(DocumentInstance documentInstance);
}
