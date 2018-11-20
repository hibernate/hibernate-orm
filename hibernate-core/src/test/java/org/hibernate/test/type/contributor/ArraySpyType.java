package org.hibernate.test.type.contributor;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class ArraySpyType
        extends AbstractSingleColumnStandardBasicType<Array>
        implements DiscriminatorType<Array> {

    private static int instanceCount;

    public ArraySpyType() {
        super( VarcharTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
        instanceCount++;
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

    public static int getInstanceCount() {
        return instanceCount;
    }

    public static void resetInstanceCount() {
        instanceCount = 0;
    }
}
