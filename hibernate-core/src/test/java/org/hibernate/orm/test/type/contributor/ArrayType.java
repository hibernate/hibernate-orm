package org.hibernate.orm.test.type.contributor;

import java.util.Map;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcTypeDescriptor;
import org.hibernate.type.spi.TypeBootstrapContext;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType
        extends AbstractSingleColumnStandardBasicType<Array> {

    public static final ArrayType INSTANCE = new ArrayType();

    private Map<String, Object> settings;

    public ArrayType() {
        super( VarcharJdbcTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
    }

    public ArrayType(TypeBootstrapContext typeBootstrapContext) {
        super( VarcharJdbcTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
        this.settings = typeBootstrapContext.getConfigurationSettings();
    }

    @Override
    public String getName() {
        return "comma-separated-array";
    }

    public Map<String, Object> getSettings() {
        return settings;
    }
}
