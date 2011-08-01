package org.hibernate.metamodel.source.annotations.attribute;

import java.util.Calendar;
import java.util.Date;
import javax.persistence.TemporalType;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * @author Strong Liu
 */
public class TemporalTypeResolver extends AbstractHibernateTypeResolver {
    private final MappedAttribute mappedAttribute;
    private final boolean isMapKey;

    public TemporalTypeResolver(MappedAttribute mappedAttribute) {
        if ( mappedAttribute == null ) {
            throw new AssertionFailure( "MappedAttribute is null" );
        }
        this.mappedAttribute = mappedAttribute;
        this.isMapKey = false;//todo
    }

    @Override
    public String resolveHibernateTypeName(AnnotationInstance temporalAnnotation) {

        if ( isTemporalType( mappedAttribute.getAttributeType() ) ) {
            if ( temporalAnnotation == null ) {
                //SPEC 11.1.47 The Temporal annotation must be specified for persistent fields or properties of type java.util.Date and java.util.Calendar.
                throw new AnnotationException( "Attribute " + mappedAttribute.getName() + " is a Temporal type, but no @Temporal annotation found." );
            }
            TemporalType temporalType = JandexHelper.getEnumValue( temporalAnnotation, "value", TemporalType.class );
            boolean isDate = Date.class.isAssignableFrom( mappedAttribute.getAttributeType() );
            String type = null;
            switch ( temporalType ) {
                case DATE:
                    type = isDate ? "date" : "calendar_date";
                    break;
                case TIME:
                    type = "time";
                    if ( !isDate ) {
                        throw new NotYetImplementedException( "Calendar cannot persist TIME only" );
                    }
                    break;
                case TIMESTAMP:
                    type = isDate ? "timestamp" : "calendar";
                    break;
                default:
                    throw new AssertionFailure( "Unknown temporal type: " + temporalType );
            }
            return type;
        }
        else {
            if ( temporalAnnotation != null ) {
                throw new AnnotationException(
                        "@Temporal should only be set on a java.util.Date or java.util.Calendar property: " + mappedAttribute
                                .getName()
                );
            }
        }
        return null;
    }

    @Override
    protected AnnotationInstance getAnnotationInstance() {
        return JandexHelper.getSingleAnnotation(
                mappedAttribute.annotations(),
                JPADotNames.TEMPORAL
        );
    }

    private static boolean isTemporalType(Class type) {
        return Date.class.isAssignableFrom( type ) || Calendar.class.isAssignableFrom( type );
    }

}
