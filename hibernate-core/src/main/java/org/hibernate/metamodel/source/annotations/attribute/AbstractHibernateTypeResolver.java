package org.hibernate.metamodel.source.annotations.attribute;

import java.util.Collections;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Strong Liu
 */
public abstract class AbstractHibernateTypeResolver implements HibernateTypeResolver {
    protected abstract AnnotationInstance getAnnotationInstance();

    protected abstract String resolveHibernateTypeName(AnnotationInstance annotationInstance);

    protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance annotationInstance) {
        return Collections.emptyMap();
    }
//    private String explicitHibernateTypeName;
//    private Map<String,String> explicitHibernateTypeParameters;
    /**
     * An optional  explicit hibernate type name specified via {@link org.hibernate.annotations.Type}.
     */
    @Override
    final public String getExplicitHibernateTypeName() {
        return resolveHibernateTypeName( getAnnotationInstance() );
    }

    /**
     * Optional type parameters. See {@link #getExplicitHibernateTypeName()}.
     */
    @Override
    final public Map<String, String> getExplicitHibernateTypeParameters() {
        if ( StringHelper.isNotEmpty( getExplicitHibernateTypeName() ) ) {
            return resolveHibernateTypeParameters( getAnnotationInstance() );
        }
        else {
            return Collections.emptyMap();
        }
    }
}
