package org.hibernate.envers;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The {@code AuditingOverride} annotation is used to override the auditing
 * behavior of a field (or property) inside an embedded component.
 *
 * @author Erik-Berndt Scheper
 * @see javax.persistence.Embedded
 * @see javax.persistence.Embeddable
 * @see javax.persistence.MappedSuperclass  
 * @see javax.persistence.AssociationOverride
 * @see AuditJoinTable
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface AuditOverride {

	/**
	 * @return <strong>Required</strong> Name of the field (or property) whose mapping
	 * is being overridden.
	 */
	String name();

	/**
	 * @return Indicates if the field (or property) is audited; defaults to {@code true}.
	 */
	boolean isAudited() default true;

	/**
	 * @return New {@link AuditJoinTable} used for this field (or property). Its value
	 * is ignored if {@link #isAudited()} equals to {@code false}.
	 */
	AuditJoinTable auditJoinTable() default @AuditJoinTable;
}
