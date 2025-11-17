/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderJavaType extends AbstractClassJavaType<Gender> {

	public static final GenderJavaType INSTANCE =
		new GenderJavaType();

	protected GenderJavaType() {
		super(Gender.class);
	}

	public String toString(Gender value) {
		return value == null ? null : value.name();
	}

	public Gender fromString(CharSequence string) {
		return string == null ? null : Gender.valueOf(string.toString());
	}

	public <X> X unwrap(Gender value, Class<X> type, WrapperOptions options) {
		return CharacterJavaType.INSTANCE.unwrap(
			value == null ? null : value.getCode(),
			type,
			options
	);
	}

	public <X> Gender wrap(X value, WrapperOptions options) {
		return Gender.fromCode(
				CharacterJavaType.INSTANCE.wrap( value, options)
	);
	}
}
//end::basic-enums-custom-type-example[]
