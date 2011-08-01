package org.hibernate.metamodel.source.annotations.attribute;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;

/**
 * @author Strong Liu
 */
public class LobTypeResolver extends AbstractHibernateTypeResolver {
    private final MappedAttribute mappedAttribute;


    public LobTypeResolver(MappedAttribute mappedAttribute) {
        if ( mappedAttribute == null ) {
            throw new AssertionFailure( "MappedAttribute is null" );
        }
        this.mappedAttribute = mappedAttribute;
    }

    @Override
    protected AnnotationInstance getAnnotationInstance() {
        return JandexHelper.getSingleAnnotation( mappedAttribute.annotations(), JPADotNames.LOB );
    }

    @Override
    public String resolveHibernateTypeName(AnnotationInstance annotationInstance) {
        if ( annotationInstance == null ) {
            return null;
        }
        String type = null;
        if ( Clob.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = StandardBasicTypes.CLOB.getName();
        }
        else if ( Blob.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = StandardBasicTypes.BLOB.getName();
        }
        else if ( String.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = StandardBasicTypes.MATERIALIZED_CLOB.getName();
        }
        else if ( Character[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = CharacterArrayClobType.class.getName();
        }
        else if ( char[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = PrimitiveCharacterArrayClobType.class.getName();
        }
        else if ( Byte[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = WrappedMaterializedBlobType.class.getName();
        }
        else if ( byte[].class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = StandardBasicTypes.MATERIALIZED_BLOB.getName();
        }
        else if ( Serializable.class.isAssignableFrom( mappedAttribute.getAttributeType() ) ) {
            type = SerializableToBlobType.class.getName();
        }
        else {
            type = "blob";
        }
        return type;
    }

    @Override
    protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance annotationInstance) {
        if ( getExplicitHibernateTypeName().equals( SerializableToBlobType.class.getName() ) ) {
            HashMap<String, String> typeParameters = new HashMap<String, String>();
            typeParameters.put(
                    SerializableToBlobType.CLASS_NAME,
                    mappedAttribute.getAttributeType().getName()
            );
            return typeParameters;
        }
        return null;
    }
}
