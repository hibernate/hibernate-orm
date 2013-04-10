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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to provide metadata about an {@link Any} or {@link ManyToAny} mapping.
 *
 * @see AnyMetaDefs
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@java.lang.annotation.Target( { PACKAGE, TYPE, METHOD, FIELD } )
@Retention( RUNTIME )
public @interface AnyMetaDef {
	/**
	 * If defined, assign a global meta definition name to be used in an @Any or @ManyToAny annotation.  If
	 * not defined, the metadata applies to the current property or field.
	 */
	String name() default "";

	/**
	 * Names the discriminator Hibernate Type for this Any/ManyToAny mapping.  The default is to use
	 * {@link org.hibernate.type.StringType}
	 */
	String metaType();

	/**
	 * Names the identifier Hibernate Type for the entity associated through this Any/ManyToAny mapping.
	 */
	String idType();

	/**
	 * Maps discriminator values to the matching corresponding entity types.
	 */
	MetaValue[] metaValues();
}
