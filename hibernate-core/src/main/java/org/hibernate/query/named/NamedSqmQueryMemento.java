/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import org.hibernate.query.sqm.tree.SqmStatement;

import java.util.Map;

public interface NamedSqmQueryMemento<E> extends NamedQueryMemento<E> {
	String getHqlString();

	SqmStatement<E> getSqmStatement();

	Map<String, String> getAnticipatedParameterTypes();

	@Override
	NamedSqmQueryMemento<E> makeCopy(String name);
}
