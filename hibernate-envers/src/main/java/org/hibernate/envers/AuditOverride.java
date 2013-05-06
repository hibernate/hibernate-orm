/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
