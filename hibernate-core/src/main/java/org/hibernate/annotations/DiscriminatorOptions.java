/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
// $Id$
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Optional annotation to express Hibernate specific discrimintor properties.
 *
 * @author Hardy Ferentschik
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface DiscriminatorOptions {
	/**
	 * "Forces" Hibernate to specify the allowed discriminator values, even when retrieving all instances of
	 * the root class.  {@code true} indicates that the discriminator value should be forced; Default is
	 * {@code false}.
	 */
	boolean force() default false;

	/**
	 * Set this to {@code false} if your discriminator column is also part of a mapped composite identifier.
	 * It tells Hibernate not to include the column in SQL INSERTs.  Default is {@code true}.
	 */
	boolean insert() default true;
}
