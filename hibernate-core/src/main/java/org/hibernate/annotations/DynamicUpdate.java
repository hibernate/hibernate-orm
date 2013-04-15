/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * For updating, should this entity use dynamic sql generation where only changed columns get referenced in the
 * prepared sql statement?
 * <p/>
 * Note, for re-attachment of detached entities this is not possible without select-before-update being enabled.
 *
 * @author Steve Ebersole
 *
 * @see SelectBeforeUpdate
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface DynamicUpdate {
	/**
	 * Should dynamic update generation be used for this entity?  {@code true} says the update sql will be dynamic
	 * generated.  Default is {@code true} (since generally this annotation is not used unless the user wants dynamic
	 * generation).
	 */
	boolean value() default true;
}
