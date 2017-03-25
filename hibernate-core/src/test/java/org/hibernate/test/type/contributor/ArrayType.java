package org.hibernate.test.type.contributor;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType
        extends AbstractSingleColumnStandardBasicType<Array>
        implements DiscriminatorType<Array> {

    public static final ArrayType INSTANCE = new ArrayType();

    public ArrayType() {
        super( VarcharTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
    }

    @Override
    public Array stringToObject(String xml) throws Exception {
        return fromString( xml );
    }

    @Override
    public String objectToSQLString(Array value, Dialect dialect) throws Exception {
        return toString( value );
    }

    @Override
    public String getName() {
        return "comma-separated-array";
    }

}
