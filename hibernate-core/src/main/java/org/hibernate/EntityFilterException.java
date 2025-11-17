/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.Locale;

import org.hibernate.annotations.FilterDef;

import jakarta.persistence.EntityNotFoundException;

/**
 * Thrown if an enabled {@linkplain FilterDef filter} would filter out
 * the target of a {@link jakarta.persistence.ManyToOne @ManyToOne} or
 * {@link jakarta.persistence.OneToOne @OneToOne} association.
 * <p>
 * By default, a filter does not apply to to-one association fetching,
 * and this exception does not occur. However, if a filter is explicitly
 * declared {@link FilterDef#applyToLoadByKey applyToLoadByKey = true},
 * then the filter is applied, and it's possible that a filtered entity
 * is the target of a to-one association belonging to an unfiltered entity.
 * Replacing such a filtered object with {@code null} would lead to data
 * loss, and so filtering never results in such replacement. Instead,
 * this exception is thrown to indicate the inconsistency of the data
 * with the filter definition.
 *
 * @see FilterDef#applyToLoadByKey
 */
public class EntityFilterException extends EntityNotFoundException {
	private final String entityName;
	private final Object identifier;
	private final String role;

	public EntityFilterException(String entityName, Object identifier, String role) {
		super(
				String.format(
						Locale.ROOT,
						"Entity `%s` with identifier value `%s` is filtered for association `%s`",
						entityName,
						identifier,
						role
				)
		);
		this.entityName = entityName;
		this.identifier = identifier;
		this.role = role;
	}

	public String getEntityName() {
		return entityName;
	}

	public Object getIdentifier() {
		return identifier;
	}

	public String getRole() {
		return role;
	}
}
