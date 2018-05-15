package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;

import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetType-example[]
public class BitSetType extends BasicTypeImpl<BitSet> {

    public static final BitSetType INSTANCE = new BitSetType();

    public BitSetType() {
        super( BitSetTypeDescriptor.INSTANCE, VarcharSqlDescriptor.INSTANCE );
    }

}
//end::basic-custom-type-BitSetType-example[]
