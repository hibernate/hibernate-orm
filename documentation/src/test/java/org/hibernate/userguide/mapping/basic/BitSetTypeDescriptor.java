package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetTypeDescriptor-example[]
public class BitSetTypeDescriptor extends AbstractTypeDescriptor<BitSet> {

    private static final String DELIMITER = ",";

    public static final BitSetTypeDescriptor INSTANCE = new BitSetTypeDescriptor();

    public BitSetTypeDescriptor() {
        super( BitSet.class );
    }

    @Override
    public String toString(BitSet value) {
        StringBuilder builder = new StringBuilder();
        for ( long token : value.toLongArray() ) {
            if ( builder.length() > 0 ) {
                builder.append( DELIMITER );
            }
            builder.append( Long.toString( token, 2 ) );
        }
        return builder.toString();
    }

    @Override
    public BitSet fromString(String string) {
        if ( string == null || string.isEmpty() ) {
            return null;
        }
        String[] tokens = string.split( DELIMITER );
        long[] values = new long[tokens.length];

        for ( int i = 0; i < tokens.length; i++ ) {
            values[i] = Long.valueOf( tokens[i], 2 );
        }
        return BitSet.valueOf( values );
    }

    @SuppressWarnings({"unchecked"})
    public <X> X unwrap(BitSet value, Class<X> type, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( BitSet.class.isAssignableFrom( type ) ) {
            return (X) value;
        }
        if ( String.class.isAssignableFrom( type ) ) {
            return (X) toString( value);
        }
        throw unknownUnwrap( type );
    }

    public <X> BitSet wrap(X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( String.class.isInstance( value ) ) {
            return fromString( (String) value );
        }
        if ( BitSet.class.isInstance( value ) ) {
            return (BitSet) value;
        }
        throw unknownWrap( value.getClass() );
    }
}
//end::basic-custom-type-BitSetTypeDescriptor-example[]
