/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import jakarta.persistence.TupleElement;

/**
 * Implementation of the JPA TupleElement contract
 *
 * @author Steve Ebersole
 */
public class TupleElementImpl<E> implements TupleElement<E> {
	private final Class<? extends E> javaType;
	private final String alias;

	public TupleElementImpl(Class<? extends E> javaType, String alias) {
		this.javaType = javaType;
		this.alias = alias;
	}

	@Override
	public Class<? extends E> getJavaType() {
		return javaType;
	}

	@Override
	public String getAlias() {
		return alias;
	}
}
