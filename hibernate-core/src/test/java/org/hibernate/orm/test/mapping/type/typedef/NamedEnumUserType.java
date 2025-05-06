/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.typedef;

import java.util.Properties;

import org.hibernate.orm.test.EnumType;

/**
 * A simple user type where we force enums to be saved by name, not ordinal
 *
 * @author gtoison
 */
public class NamedEnumUserType<T extends Enum<T>> extends EnumType<T> {
	private static final long serialVersionUID = -4176945793071035928L;

	@Override
	public void setParameterValues(Properties parameters) {
		parameters.setProperty(EnumType.NAMED, "true");

		super.setParameterValues(parameters);
	}
}
