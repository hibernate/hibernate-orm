package org.hibernate.metamodel.source.annotations.attribute;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * @author Strong Liu
 */
public class ExplicitHibernateTypeResolver extends AbstractHibernateTypeResolver {
    private final MappedAttribute mappedAttribute;


    public ExplicitHibernateTypeResolver(MappedAttribute mappedAttribute) {
        this.mappedAttribute = mappedAttribute;
    }

    @Override
    protected String resolveHibernateTypeName(AnnotationInstance typeAnnotation) {
        String typeName = null;
        if ( typeAnnotation != null ) {
            typeName = JandexHelper.getValue( typeAnnotation, "type", String.class );
        }
        return typeName;
    }

    @Override
    protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance typeAnnotation) {
        HashMap<String, String> typeParameters = new HashMap<String, String>();
        AnnotationValue parameterAnnotationValue = typeAnnotation.value( "parameters" );
        if ( parameterAnnotationValue != null ) {
            AnnotationInstance[] parameterAnnotations = parameterAnnotationValue.asNestedArray();
            for ( AnnotationInstance parameterAnnotationInstance : parameterAnnotations ) {
                typeParameters.put(
                        JandexHelper.getValue( parameterAnnotationInstance, "name", String.class ),
                        JandexHelper.getValue(
                                parameterAnnotationInstance,
                                "value",
                                String.class
                        )
                );
            }
        }
        return typeParameters;
    }

    @Override
    protected AnnotationInstance getAnnotationInstance() {
        return JandexHelper.getSingleAnnotation(
                mappedAttribute.annotations(),
                HibernateDotNames.TYPE
        );
    }

}
