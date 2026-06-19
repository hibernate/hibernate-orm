/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.tree.spi.SqmStatement;

import java.util.Map;

public interface NamedSqmQueryMemento<E> extends NamedQueryMemento<E> {
	@Nonnull
	String getHqlString();

	@Nullable
	SqmStatement<E> getSqmStatement();

	@Nullable
	Map<String, String> getAnticipatedParameterTypes();

	@Nonnull
	@Override
	NamedSqmQueryMemento<E> makeCopy(@Nonnull String name);
}
