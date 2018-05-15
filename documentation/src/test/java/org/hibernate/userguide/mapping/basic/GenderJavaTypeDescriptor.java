package org.hibernate.userguide.mapping.basic;

import org.hibernate.type.descriptor.java.internal.CharacterJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderJavaTypeDescriptor extends AbstractBasicJavaDescriptor<Gender> {

    public static final GenderJavaTypeDescriptor INSTANCE =
        new GenderJavaTypeDescriptor();

    protected GenderJavaTypeDescriptor() {
        super( Gender.class );
    }

    public String toString(Gender value) {
        return value == null ? null : value.name();
    }

    public Gender fromString(String string) {
        return string == null ? null : Gender.valueOf( string );
    }

    @Override
    public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
        // none
        return null;
    }

    public <X> X unwrap(Gender value, Class<X> type, WrapperOptions options) {
        return CharacterJavaDescriptor.INSTANCE.unwrap(
            value == null ? null : value.getCode(),
            type,
            options
        );
    }

    public <X> Gender wrap(X value, WrapperOptions options) {
        return Gender.fromCode(
                CharacterJavaDescriptor.INSTANCE.wrap( value, options )
        );
    }
}
//end::basic-enums-custom-type-example[]