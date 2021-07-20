package org.hibernate.test.type.contributor;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
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

    private Map<String, Object> settings;

    public ArrayType() {
        super( VarcharTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
    }

    public ArrayType(ServiceRegistry serviceRegistry) {
        super( VarcharTypeDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
        ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
        this.settings = configurationService.getSettings();
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
