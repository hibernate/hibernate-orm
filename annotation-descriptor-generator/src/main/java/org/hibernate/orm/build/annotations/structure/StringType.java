/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;


/**
 * Type implementation Strings
 *
 * @author Steve Ebersole
 */
public class StringType implements Type {
	public static final StringType STRING_TYPE = new StringType();

	public StringType() {
	}

	@Override
	public String getTypeDeclarationString() {
		return "String";
	}
}
