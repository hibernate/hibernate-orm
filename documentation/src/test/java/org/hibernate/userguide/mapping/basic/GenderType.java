package org.hibernate.userguide.mapping.basic;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.CharTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderType extends AbstractSingleColumnStandardBasicType<Gender> {

    public static final GenderType INSTANCE = new GenderType();

    public GenderType() {
        super(
            CharTypeDescriptor.INSTANCE,
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
