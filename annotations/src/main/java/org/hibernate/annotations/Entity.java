//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Extends {@link javax.persistence.Entity} with Hibernate features
 *
 * @author Emmanuel Bernard
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Entity {
	/** Is this entity mutable (read only) or not */
	boolean mutable() default true;
	/** Needed column only in SQL on insert */
	boolean dynamicInsert() default false;
	/** Needed column only in SQL on update */
	boolean dynamicUpdate() default false;
	/** Do a select to retrieve the entity before any potential update */
	boolean selectBeforeUpdate() default false;
	/** polymorphism strategy for this entity */
	PolymorphismType polymorphism() default PolymorphismType.IMPLICIT;
	/** persister of this entity, default is hibernate internal one */
	String persister() default "";
	/** optimistic locking strategy */
	OptimisticLockType optimisticLock() default OptimisticLockType.VERSION;
}
