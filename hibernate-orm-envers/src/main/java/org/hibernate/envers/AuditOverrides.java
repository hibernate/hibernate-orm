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
 * The {@code AuditingOverrides} annotation is used to override the auditing
 * behavior for one ore more fields (or properties) inside an embedded
 * component.
 *
 * @author Erik-Berndt Scheper
 * @see javax.persistence.Embedded
 * @see javax.persistence.Embeddable
 * @see javax.persistence.MappedSuperclass
 * @see javax.persistence.AssociationOverride
 * @see javax.persistence.AssociationOverrides
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
