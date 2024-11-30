/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.inet;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;

public class InetType extends AbstractSingleColumnStandardBasicType<Inet> {

	public static final InetType INSTANCE = new InetType();

	public InetType() {
		super( InetJdbcType.INSTANCE, InetJavaType.INSTANCE );
	}

	@Override
	public String getName() {
		return "inet";
	}
}
