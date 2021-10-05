package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import java.util.BitSet;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-bitset-example-java-type[]
public class BitSetJavaType extends AbstractClassJavaTypeDescriptor<BitSet> {
    public static final BitSetJavaType INSTANCE = new BitSetJavaType();

    public BitSetJavaType() {
        super( BitSet.class );
    }

    @Override
    public MutabilityPlan<BitSet> getMutabilityPlan() {
        return BitSetMutabilityPlan.INSTANCE;
    }

    @Override
    public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators indicators) {
        return indicators.getTypeConfiguration()
                .getJdbcTypeDescriptorRegistry()
                .getDescriptor( Types.VARCHAR );
    }

    @Override
    public String toString(BitSet value) {
        return BitSetHelper.bitSetToString( value );
    }

    @Override
    public BitSet fromString(CharSequence string) {
        return BitSetHelper.stringToBitSet( string.toString() );
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
        if ( type.isArray() ) {
            if ( type.getComponentType() == byte.class ) {
                return (X) value.toByteArray();
            }
        }
        throw unknownUnwrap( type );
    }

    public <X> BitSet wrap(X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( value instanceof CharSequence ) {
            return fromString( (CharSequence) value );
        }
        if ( value instanceof BitSet ) {
            return (BitSet) value;
        }
        throw unknownWrap( value.getClass() );
    }

}
//end::basic-bitset-example-java-type[]
