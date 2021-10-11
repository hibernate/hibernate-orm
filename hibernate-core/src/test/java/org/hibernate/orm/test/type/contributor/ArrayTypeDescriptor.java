package org.hibernate.orm.test.type.contributor;

import java.sql.Types;
import java.util.Arrays;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Vlad Mihalcea
 */
public class ArrayTypeDescriptor extends AbstractClassJavaTypeDescriptor<Array> {

    private static final String DELIMITER = ",";

    public static final ArrayTypeDescriptor INSTANCE = new ArrayTypeDescriptor();

    public ArrayTypeDescriptor() {
        super( Array.class );
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators indicators) {
        return indicators.getTypeConfiguration().getJdbcTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
    }

    @Override
    public String toString(Array value) {
        StringBuilder builder = new StringBuilder();
        for ( String token : value ) {
            if ( builder.length() > 0 ) {
                builder.append( DELIMITER );
            }
            builder.append( token );
        }
        return builder.toString();
    }

    public static String extractString(Array value) {
        StringBuilder builder = new StringBuilder();
        for ( String token : value ) {
            if ( builder.length() > 0 ) {
                builder.append( DELIMITER );
            }
            builder.append( token );
        }
        return builder.toString();
    }

    @Override
    public Array fromString(CharSequence string) {
        if ( string == null || string.length() == 0 ) {
            return null;
        }
        String[] tokens = string.toString().split( DELIMITER );
        Array array = new Array();
        array.addAll( Arrays.asList(tokens) );
        return array;
    }

    @SuppressWarnings({"unchecked"})
    public <X> X unwrap(Array value, Class<X> type, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( Array.class.isAssignableFrom( type ) ) {
            return (X) value;
        }
        if ( String.class.isAssignableFrom( type ) ) {
            return (X) toString( value);
        }
        throw unknownUnwrap( type );
    }

    public <X> Array wrap(X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( String.class.isInstance( value ) ) {
            return fromString( (String) value );
        }
        if ( Array.class.isInstance( value ) ) {
            return (Array) value;
        }
        throw unknownWrap( value.getClass() );
    }
}
