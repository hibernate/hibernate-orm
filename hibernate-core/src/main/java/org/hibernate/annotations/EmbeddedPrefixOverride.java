/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Allows overriding the name (prefix) of an Embedded component and change respective value for all contained fields.
 *
 * @author Kevin Dargel
 */
@Target( {FIELD, METHOD} )
@Retention( RUNTIME )
public @interface EmbeddedPrefixOverride {
	/**
	 * The prefix value (name) of an Embedded
	 */
	String value();
}
