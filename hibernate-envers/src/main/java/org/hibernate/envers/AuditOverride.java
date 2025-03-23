/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@code AuditingOverride} annotation is used to override the auditing
 * behavior of a superclass or single property inherited from {@link jakarta.persistence.MappedSuperclass}
 * type, or attribute inside an embedded component.
 *
 * @author Erik-Berndt Scheper
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 *
 * @see jakarta.persistence.Embedded
 * @see jakarta.persistence.Embeddable
 * @see jakarta.persistence.MappedSuperclass
 * @see jakarta.persistence.AssociationOverride
 * @see AuditJoinTable
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(AuditOverrides.class)
public @interface AuditOverride {

	/**
	 * Name of the field (or property) whose mapping is being overridden. Allows empty value if
	 * {@link AuditOverride} is used to change auditing behavior of all attributes inherited from
	 * {@link jakarta.persistence.MappedSuperclass} type.
	 */
	String name() default "";

	/**
	 * Indicates if the field (or property) is audited; defaults to {@code true}.
	 */
	boolean isAudited() default true;

	/**
	 * New {@link AuditJoinTable} used for this field (or property). Its value
	 * is ignored if {@link #isAudited()} equals to {@code false}.
	 */
	AuditJoinTable auditJoinTable() default @AuditJoinTable;

	/**
	 * Specifies class which field (or property) mapping is being overridden. <strong>Required</strong> if
	 * {@link AuditOverride} is used to change auditing behavior of attributes inherited from
	 * {@link jakarta.persistence.MappedSuperclass} type.
	 */
	Class forClass() default void.class;
}
