package org.hibernate;

import java.util.Locale;

import org.hibernate.annotations.FilterDef;

import jakarta.persistence.EntityNotFoundException;

/**
 * Thrown when a filter excludes an association target and its fetching strategy
 * cannot preserve the stored reference while representing the association as null.
 * For example, this can occur for an {@link org.hibernate.annotations.Any} association.
 * <p>
 * Ordinary {@link jakarta.persistence.ManyToOne} and {@link jakarta.persistence.OneToOne}
 * fetching retains the physical reference in the persistence context and represents
 * the filtered association as {@code null}.
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
