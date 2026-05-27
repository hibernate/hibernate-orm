/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.DomainType;

/**
 * A CTE (common table expression) criteria type.
 */
@Incubating
public interface JpaCteCriteriaType<T> extends JpaCriteriaNode {

	/**
	 * The name under which this CTE is registered.
	 */
	@Nullable String getName();

	/**
	 * The domain type of the CTE.
	 */
	DomainType<T> getType();

	/**
	 * The attributes of the CTE type.
	 */
	@Nonnull
	List<JpaCteCriteriaAttribute> getAttributes();

	/**
	 * Returns the found attribute or null.
	 */
	@Nullable
	JpaCteCriteriaAttribute getAttribute(String name);
}
