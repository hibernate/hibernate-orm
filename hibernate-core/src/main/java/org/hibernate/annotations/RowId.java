/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Support for {@code ROWID} mapping feature of Hibernate.
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target( { TYPE })
@Retention(RUNTIME)
public @interface RowId {
	/**
	 * Names the {@code ROWID} identifier.
	 */
	String value();
}
