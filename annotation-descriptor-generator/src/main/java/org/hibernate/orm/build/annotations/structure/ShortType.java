/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.build.annotations.structure;


/**
 * @author Steve Ebersole
 */
public class ShortType implements Type {
	public static final ShortType SHORT_TYPE = new ShortType();

	@Override
	public String getTypeDeclarationString() {
		return "short";
	}
}
