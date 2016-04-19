/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Extends {@link javax.persistence.Entity} with Hibernate features.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated See individual attributes for intended replacements.  To be removed in 4.1
 */
@Target(TYPE)
@Retention(RUNTIME)
@Deprecated
public @interface Entity {
	/**
	 * Is this entity mutable (read only) or not.
	 *
	 * @deprecated use {@link org.hibernate.annotations.Immutable} 
	 */
	@Deprecated
	boolean mutable() default true;
	/**
	 * Needed column only in SQL on insert.
	 * @deprecated use {@link DynamicInsert} instead
	 */
	@Deprecated
	boolean dynamicInsert() default false;
	/**
	 * Needed column only in SQL on update.
	 * @deprecated Use {@link DynamicUpdate} instead
	 */
	@Deprecated
	boolean dynamicUpdate() default false;
	/**
	 *  Do a select to retrieve the entity beforeQuery any potential update.
	 *  @deprecated Use {@link SelectBeforeUpdate} instead
	 */
	@Deprecated
	boolean selectBeforeUpdate() default false;
	/**
	 * polymorphism strategy for this entity.
	 * @deprecated use {@link Polymorphism} instead
	 */
	@Deprecated
	PolymorphismType polymorphism() default PolymorphismType.IMPLICIT;
	/**
	 * optimistic locking strategy.
	 * @deprecated use {@link OptimisticLocking} instead.
	 */
	@Deprecated
	OptimisticLockType optimisticLock() default OptimisticLockType.VERSION;
	/**
	 * persister of this entity, default is hibernate internal one.
	 * @deprecated  use {@link Persister} instead
	 */
	@Deprecated
	String persister() default "";
}
