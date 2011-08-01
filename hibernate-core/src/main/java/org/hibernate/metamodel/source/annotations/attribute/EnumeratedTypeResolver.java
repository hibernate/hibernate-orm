package org.hibernate.metamodel.source.annotations.attribute;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.type.EnumType;

/**
 * @author Strong Liu
 */
public class EnumeratedTypeResolver extends AbstractHibernateTypeResolver {
    private final MappedAttribute mappedAttribute;
    private final boolean isMapKey;

    public EnumeratedTypeResolver(MappedAttribute mappedAttribute) {
        if ( mappedAttribute == null ) {
            throw new AssertionFailure( "MappedAttribute is null" );
        }
        this.mappedAttribute = mappedAttribute;
        this.isMapKey = false;//todo
    }

    @Override
    protected AnnotationInstance getAnnotationInstance() {
        return JandexHelper.getSingleAnnotation(
                mappedAttribute.annotations(),
                JPADotNames.ENUMERATED
        );
    }

    @Override
    public String resolveHibernateTypeName(AnnotationInstance enumeratedAnnotation) {
        boolean isEnum = mappedAttribute.getAttributeType().isEnum();
        if ( !isEnum ) {
            if ( enumeratedAnnotation != null ) {
                throw new AnnotationException( "Attribute " + mappedAttribute.getName() + " is not a Enumerated type, but has a @Enumerated annotation." );
            }
            else {
                return null;
            }
        }
        return EnumType.class.getName();
    }

    @Override
    protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance annotationInstance) {
        HashMap<String, String> typeParameters = new HashMap<String, String>();
        typeParameters.put( EnumType.ENUM, mappedAttribute.getAttributeType().getName() );
        if ( annotationInstance != null ) {
            javax.persistence.EnumType enumType = JandexHelper.getEnumValue(
                    annotationInstance,
                    "value",
                    javax.persistence.EnumType.class
            );
            if ( javax.persistence.EnumType.ORDINAL.equals( enumType ) ) {
                typeParameters.put( EnumType.TYPE, String.valueOf( Types.INTEGER ) );
            }
            else if ( javax.persistence.EnumType.STRING.equals( enumType ) ) {
                typeParameters.put( EnumType.TYPE, String.valueOf( Types.VARCHAR ) );
            }
            else {
                throw new AssertionFailure( "Unknown EnumType: " + enumType );
            }
        }
        else {
            typeParameters.put( EnumType.TYPE, String.valueOf( Types.INTEGER ) );
        }
        //todo
//        Schema schema = mappedAttribute.getContext().getMetadataImplementor().getDatabase().getDefaultSchema();
//        Identifier schemaIdentifier = schema.getName().getSchema();
//        Identifier catalogIdentifier = schema.getName().getCatalog();
//        String schemaName = schemaIdentifier == null ? "" : schemaIdentifier.getName();
//        String catalogName = catalogIdentifier == null ? "" : catalogIdentifier.getName();
//        typeParameters.put( EnumType.SCHEMA, schemaName );
//        typeParameters.put( EnumType.CATALOG, catalogName );
        /**
         String schema = columns[0].getTable().getSchema();
         schema = schema == null ? "" : schema;
         String catalog = columns[0].getTable().getCatalog();
         catalog = catalog == null ? "" : catalog;
         typeParameters.setProperty( EnumType.SCHEMA, schema );
         typeParameters.setProperty( EnumType.CATALOG, catalog );
         typeParameters.setProperty( EnumType.TABLE, columns[0].getTable().getName() );
         typeParameters.setProperty( EnumType.COLUMN, columns[0].getName() );
         */
        return typeParameters;
    }

}
