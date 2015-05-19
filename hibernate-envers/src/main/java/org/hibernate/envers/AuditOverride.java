/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@code AuditingOverride} annotation is used to override the auditing
 * behavior of a superclass or single property inherited from {@link javax.persistence.MappedSuperclass}
 * type, or attribute inside an embedded component.
 *
 * @author Erik-Berndt Scheper
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 *
 * @see javax.persistence.Embedded
 * @see javax.persistence.Embeddable
 * @see javax.persistence.MappedSuperclass
 * @see javax.persistence.AssociationOverride
 * @see AuditJoinTable
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface AuditOverride {

	/**
	 * Name of the field (or property) whose mapping is being overridden. Allows empty value if
	 * {@link AuditOverride} is used to change auditing behavior of all attributes inherited from
	 * {@link javax.persistence.MappedSuperclass} type.
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
	 * {@link javax.persistence.MappedSuperclass} type.
	 */
	Class forClass() default void.class;
}
