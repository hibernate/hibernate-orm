package org.hibernate.orm.test.type.contributor;

import java.util.Map;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;
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
    public Array stringToObject(CharSequence sequence) throws Exception {
        return fromString( sequence );
    }

    @Override
    public String getName() {
        return "comma-separated-array";
    }

    public Map<String, Object> getSettings() {
        return settings;
    }
}
