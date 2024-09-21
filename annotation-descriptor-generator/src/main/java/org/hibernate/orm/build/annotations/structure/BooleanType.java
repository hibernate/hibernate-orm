/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;

/**
 * @author Steve Ebersole
 */
public class BooleanType implements Type {
	public static final BooleanType BOOLEAN_TYPE = new BooleanType();

	@Override
	public String getTypeDeclarationString() {
		return "boolean";
	}
}
