package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetType-example[]
public class BitSetType
        extends AbstractSingleColumnStandardBasicType<BitSet>
        implements DiscriminatorType<BitSet> {

    public static final BitSetType INSTANCE = new BitSetType();

    public BitSetType() {
        super( VarcharTypeDescriptor.INSTANCE, BitSetJavaType.INSTANCE );
    }

    @Override
    public BitSet stringToObject(CharSequence sequence) throws Exception {
        return fromString( sequence );
    }

	@Override
    public String getName() {
        return "bitset";
    }

}
//end::basic-custom-type-BitSetType-example[]
