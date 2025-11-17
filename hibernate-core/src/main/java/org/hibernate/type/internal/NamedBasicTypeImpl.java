/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class NamedBasicTypeImpl<J> extends BasicTypeImpl<J> {

	private final String name;

	public NamedBasicTypeImpl(JavaType<J> jtd, JdbcType std, String name) {
		super( jtd, std );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
