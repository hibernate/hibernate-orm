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

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import javax.persistence.Column;

/**
 * Define the map key columns as an explicit column holding the map key
 * This is completely different from {@link javax.persistence.MapKey} which use an existing column
 * This annotation and {@link javax.persistence.MapKey} are mutually exclusive
 *
 * @deprecated Use {@link javax.persistence.MapKeyColumn}
 *             This is the default behavior for Map properties marked as @OneToMany, @ManyToMany
 *             or @ElementCollection
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated
public @interface MapKey {
	Column[] columns() default {};
	/**
	 * Represent the key class in a Map
	 * Only useful if the collection does not use generics
	 */
	Class targetElement() default void.class;

	/**
	 * The optional map key type. Guessed if default
	 */
	Type type() default @Type(type = ""); 
}
