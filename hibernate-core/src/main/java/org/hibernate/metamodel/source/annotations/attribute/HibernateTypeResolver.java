package org.hibernate.metamodel.source.annotations.attribute;

import java.util.Map;

/**
 * @author Strong Liu
 */
public interface HibernateTypeResolver {
    String getExplicitHibernateTypeName();
    Map<String, String> getExplicitHibernateTypeParameters();
}
