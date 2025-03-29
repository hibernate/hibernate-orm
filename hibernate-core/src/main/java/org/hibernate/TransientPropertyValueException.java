/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * Thrown when a property cannot be persisted because it is an association
 * with a transient unsaved entity instance.
 *
 * @author Gail Badner
 */
public class TransientPropertyValueException extends TransientObjectException {
	private final String transientEntityName;
	private final String propertyOwnerEntityName;
	private final String propertyName;

	/**
	 * Constructs a {@code TransientPropertyValueException} instance.
	 *
	 * @param message - the exception message;
	 * @param transientEntityName - the entity name for the transient entity
	 * @param propertyOwnerEntityName - the entity name for entity that owns
	 * the association property.
	 * @param propertyName - the property name
	 */
	public TransientPropertyValueException(
			String message,
			String transientEntityName,
			String propertyOwnerEntityName,
			String propertyName) {
		super( message );
		this.transientEntityName = transientEntityName;
		this.propertyOwnerEntityName = propertyOwnerEntityName;
		this.propertyName = propertyName;
	}

	/**
	 * Returns the entity name for the transient entity.
	 * @return the entity name for the transient entity.
	 */
	public String getTransientEntityName() {
		return transientEntityName;
	}

	/**
	 * Returns the entity name for entity that owns the association
	 * property.
	 *
	 * @return the entity name for entity that owns the association
	 * property
	 */
	public String getPropertyOwnerEntityName() {
		return propertyOwnerEntityName;
	}

	/**
	 * Returns the property name.
	 *
	 * @return the property name.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " for entity "
				+ qualify( propertyOwnerEntityName, propertyName ) + " -> " + transientEntityName;
	}
}
