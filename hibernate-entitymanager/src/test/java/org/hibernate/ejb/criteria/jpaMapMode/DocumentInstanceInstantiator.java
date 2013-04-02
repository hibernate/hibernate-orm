package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.tuple.Instantiator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("serial")
public class DocumentInstanceInstantiator implements Instantiator {

    private final DocumentTableMapping documentTableMapping;
    private final String entityName;
    private final Set<String> isInstanceEntityNames = new HashSet<String>();

    public DocumentInstanceInstantiator(
            DocumentTableMapping documentTableMapping) {
        this.documentTableMapping = documentTableMapping;
        this.entityName = null;
    }

    public DocumentInstanceInstantiator(
            DocumentTableMapping documentTableMapping,
            PersistentClass mappingInfo) {
        this.documentTableMapping = documentTableMapping;
        this.entityName = mappingInfo.getEntityName();
        isInstanceEntityNames.add(entityName);
        if (mappingInfo.hasSubclasses()) {
            Iterator<?> itr = mappingInfo.getSubclassClosureIterator();
            while (itr.hasNext()) {
                final PersistentClass subclassInfo = (PersistentClass) itr
                        .next();
                isInstanceEntityNames.add(subclassInfo.getEntityName());
            }
        }
    }

    public DocumentInstanceInstantiator(
            DocumentTableMapping documentTableMapping, EntityBinding mappingInfo) {
        this.documentTableMapping = documentTableMapping;
        this.entityName = mappingInfo.getEntity().getName();
        isInstanceEntityNames.add(entityName);
        for (EntityBinding subEntityBinding : mappingInfo
                .getPostOrderSubEntityBindingClosure()) {
            isInstanceEntityNames.add(subEntityBinding.getEntity().getName());
        }
    }

    @Override
    public final Object instantiate(Serializable id) {
        return instantiate();
    }

    @Override
    public final DocumentInstance instantiate() {
        return new DocumentInstance(
                documentTableMapping.getDocument());
    }

    @Override
    public final boolean isInstance(Object object) {
        if (object instanceof DocumentInstance) {
            if (entityName == null) {
                return true;
            }
            String type = ((DocumentInstance) object).getDocument().getName();
            return type == null || isInstanceEntityNames.contains(type);
        } else {
            return false;
        }
    }

}
