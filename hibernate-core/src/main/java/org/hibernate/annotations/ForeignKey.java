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
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Define the foreign key name.
 *
 * @deprecated Prefer the JPA 2.1 introduced {@link javax.persistence.ForeignKey} instead.
 */
@Target({FIELD, METHOD, TYPE})
@Retention(RUNTIME)
@Deprecated
public @interface ForeignKey {
	/**
	 * Name of the foreign key.  Used in OneToMany, ManyToOne, and OneToOne
	 * relationships.  Used for the owning side in ManyToMany relationships
	 */
	String name();

	/**
	 * Used for the non-owning side of a ManyToMany relationship.  Ignored
	 * in other relationships
	 */
	String inverseName() default "";
}
