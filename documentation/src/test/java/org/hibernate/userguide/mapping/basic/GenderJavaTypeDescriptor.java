package org.hibernate.userguide.mapping.basic;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterJavaTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderJavaTypeDescriptor extends AbstractClassJavaTypeDescriptor<Gender> {

    public static final GenderJavaTypeDescriptor INSTANCE =
        new GenderJavaTypeDescriptor();

    protected GenderJavaTypeDescriptor() {
        super(Gender.class);
    }

    public String toString(Gender value) {
        return value == null ? null : value.name();
    }

    public Gender fromString(CharSequence string) {
        return string == null ? null : Gender.valueOf(string.toString());
    }

    public <X> X unwrap(Gender value, Class<X> type, WrapperOptions options) {
        return CharacterJavaTypeDescriptor.INSTANCE.unwrap(
            value == null ? null : value.getCode(),
            type,
            options
       );
    }

    public <X> Gender wrap(X value, WrapperOptions options) {
        return Gender.fromCode(
				CharacterJavaTypeDescriptor.INSTANCE.wrap(value, options)
       );
    }
}
//end::basic-enums-custom-type-example[]