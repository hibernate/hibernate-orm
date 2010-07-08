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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines @Any and @manyToAny metadata
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target( { PACKAGE, TYPE, METHOD, FIELD } )
@Retention( RUNTIME )
public @interface AnyMetaDef {
	/**
	 * If defined, assign a global meta definition name to be used in an @Any or @ManyToAny annotation
	 * If not defined, the metadata applies to the current property or field
	 */
	String name() default "";

	/**
	 * meta discriminator Hibernate type
	 */
	String metaType();

	/**
	 * Hibernate type of the id column
	 * @return
	 */
	String idType();

	/**
	 * Matching discriminator values with their respective entity
	 */
	MetaValue[] metaValues();
}
