/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Define an explicit target, avoiding reflection and generics resolving.
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({ElementType.FIELD, ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME )
public @interface Target {
	/**
	 * The target entity type.
	 */
	Class value();
}
