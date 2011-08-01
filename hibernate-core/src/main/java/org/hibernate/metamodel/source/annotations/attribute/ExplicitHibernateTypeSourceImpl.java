package org.hibernate.metamodel.source.annotations.attribute;

import java.util.Map;

import org.hibernate.metamodel.source.binder.ExplicitHibernateTypeSource;

/**
 * @author Strong Liu
 */
public class ExplicitHibernateTypeSourceImpl implements ExplicitHibernateTypeSource {
    private final HibernateTypeResolver typeResolver;

    public ExplicitHibernateTypeSourceImpl(HibernateTypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public String getName() {
        return typeResolver.getExplicitHibernateTypeName();
    }

    @Override
    public Map<String, String> getParameters() {
        return typeResolver.getExplicitHibernateTypeParameters();
    }
}
