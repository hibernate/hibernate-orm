/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
