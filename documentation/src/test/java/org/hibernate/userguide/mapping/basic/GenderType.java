package org.hibernate.userguide.mapping.basic;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.jdbc.CharJdbcTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderType extends AbstractSingleColumnStandardBasicType<Gender> {

    public static final GenderType INSTANCE = new GenderType();

    public GenderType() {
        super(
				CharJdbcTypeDescriptor.INSTANCE,
				GenderJavaTypeDescriptor.INSTANCE
        );
    }

    public String getName() {
        return "gender";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }
}
//end::basic-enums-custom-type-example[]
