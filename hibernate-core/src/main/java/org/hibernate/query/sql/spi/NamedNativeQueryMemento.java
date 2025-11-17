/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.named.NamedQueryMemento;

import jakarta.persistence.SqlResultSetMapping;

/**
 * Descriptor for a named native query in the runtime environment
 *
 * @author Steve Ebersole
 */
public interface NamedNativeQueryMemento<E> extends NamedQueryMemento<E> {
	/**
	 * Informational access to the SQL query string
	 */
	String getSqlString();

	default String getOriginalSqlString(){
		return getSqlString();
	}

	/**
	 * The affected query spaces.
	 */
	Set<String> getQuerySpaces();

	/**
	 * An explicit ResultSet mapping by name
	 *
	 * @see SqlResultSetMapping#name
	 */
	String getResultMappingName();

	/**
	 * An implicit entity mapping by entity class
	 */
	default Class<?> getResultMappingClass() {
		return getResultType();
	}

	Integer getFirstResult();

	Integer getMaxResults();

	/**
	 * Convert the memento into an untyped executable query
	 */
	@Override
	NativeQueryImplementor<E> toQuery(SharedSessionContractImplementor session);

	/**
	 * Convert the memento into a typed executable query
	 */
	@Override
	<T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);

	/**
	 * Convert the memento into a typed executable query
	 */
	<T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, String resultSetMapping);

	@Override
	NamedNativeQueryMemento<E> makeCopy(String name);

}
