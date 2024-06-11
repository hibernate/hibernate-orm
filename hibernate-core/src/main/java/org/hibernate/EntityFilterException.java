/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate;

import java.util.Locale;

import org.hibernate.annotations.FilterDef;

import jakarta.persistence.EntityNotFoundException;

/**
 * Exception thrown if a filter would make a to-one association {@code null},
 * which could lead to data loss.
 * Even though filters are applied to load-by-key operations,
 * a to-one association should never refer to an entity that is filtered.
 *
 * @see FilterDef#applyToLoadByKey()
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
