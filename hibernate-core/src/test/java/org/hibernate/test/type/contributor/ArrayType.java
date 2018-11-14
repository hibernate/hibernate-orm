package org.hibernate.test.type.contributor;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.hibernate.type.spi.TypeBootstrapContext;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType
        extends AbstractSingleColumnStandardBasicType<Array>
        implements DiscriminatorType<Array> {

    public static final ArrayType INSTANCE = new ArrayType();

    private Map<String, Object> settings;

    public ArrayType() {
        super( VarcharTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
    }

    public ArrayType(TypeBootstrapContext typeBootstrapContext) {
        super( VarcharTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
        this.settings = typeBootstrapContext.getConfigurationSettings();
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

    public Map<String, Object> getSettings() {
        return settings;
    }
}
