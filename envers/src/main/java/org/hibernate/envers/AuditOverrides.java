package org.hibernate.envers;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
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
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface AuditOverrides {
	/**
	 * @return An array of {@link AuditOverride} values, to define the new auditing
	 * behavior.
	 */
	AuditOverride[] value();
}
