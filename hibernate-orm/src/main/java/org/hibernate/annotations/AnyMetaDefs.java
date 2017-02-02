/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to group together {@link AnyMetaDef} annotations.  Can be defined at the entity or package level
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@java.lang.annotation.Target( { PACKAGE, TYPE } )
@Retention( RUNTIME )
public @interface AnyMetaDefs {
	/**
	 * The collective set of any meta-defs.
	 */
	AnyMetaDef[] value();
}
