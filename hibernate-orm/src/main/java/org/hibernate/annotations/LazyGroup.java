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
 * For use with bytecode-enhanced lazy-loading support.
 * <p/>
 * Identifies grouping for performing lazy attribute loading.  By default all
 * non-collection attributes are loaded in one group named {@code "DEFAULT"}.
 * This annotation allows defining different groups of attributes to be
 * initialized together when access one attribute in the group.
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyGroup {
	String value();
}
