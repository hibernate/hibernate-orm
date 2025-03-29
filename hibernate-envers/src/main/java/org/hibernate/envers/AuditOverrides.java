/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@code AuditingOverrides} annotation is used to override the auditing
 * behavior for one ore more fields (or properties) inside an embedded
 * component.
 *
 * @author Erik-Berndt Scheper
 * @see jakarta.persistence.Embedded
 * @see jakarta.persistence.Embeddable
 * @see jakarta.persistence.MappedSuperclass
 * @see jakarta.persistence.AssociationOverride
 * @see jakarta.persistence.AssociationOverrides
 * @see AuditJoinTable
 * @see AuditOverride
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface AuditOverrides {
	/**
	 * An array of {@link AuditOverride} values, to define the new auditing
	 * behavior.
	 */
	AuditOverride[] value();
}
