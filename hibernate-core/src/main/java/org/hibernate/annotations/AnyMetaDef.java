/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
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
@Repeatable(AnyMetaDefs.class)
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
