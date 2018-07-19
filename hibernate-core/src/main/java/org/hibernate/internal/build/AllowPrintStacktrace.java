/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.build;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to indicate to the Forbidden APIs library that a specific usage
 * of {@link Exception#printStackTrace} is allowable.
 *
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.CLASS )
public @interface AllowPrintStacktrace {
}
