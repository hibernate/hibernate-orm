/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.spi;

import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import jakarta.persistence.SqlResultSetMapping;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

/**
 * Descriptor for a named native query in the runtime environment
 *
 * @author Steve Ebersole
 */
public interface NamedNativeQueryMemento<E> extends NamedQueryMemento<E> {
	/**
	 * Informational access to the SQL query string
	 */
	@Nonnull
	String getSqlString();

	@Nonnull
	default String getOriginalSqlString(){
		return getSqlString();
	}

	/**
	 * The affected query spaces.
	 */
	@Nullable
	Set<String> getQuerySpaces();

	/**
	 * An explicit ResultSet mapping by name
	 *
	 * @see SqlResultSetMapping#name
	 */
	@Nullable
	String getResultMappingName();

	@Nullable
	Integer getFirstResult();

	@Nullable
	Integer getMaxResults();

	/**
	 * Convert the memento into an untyped executable query
	 */
	@Nonnull
	@Override
	NativeQueryImplementor<E> toQuery(@Nonnull SharedSessionContractImplementor session);

	/**
	 * Convert the memento into a typed executable query
	 */
	@Nonnull
	@Override
	<T> NativeQueryImplementor<T> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<T> resultType);

	/**
	 * Convert the memento into a typed executable query
	 */
	@Nonnull
	<T> NativeQueryImplementor<T> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable String resultSetMapping);

	@Nonnull
	@Override
	NamedNativeQueryMemento<E> makeCopy(@Nonnull String name);

}
