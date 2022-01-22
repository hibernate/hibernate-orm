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
import java.lang.annotation.Target;

/**
 * Specifies a restriction written in native SQL to add to the generated
 * SQL when querying the {@link jakarta.persistence.JoinTable join table}
 * of a collection.
 * <p>
 * For example, {@code @Where("deleted = false")} could be used to hide
 * associations which have been soft-deleted from an association table.
 *
 * @author Emmanuel Bernard
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WhereJoinTable {
	/**
	 * A predicate, written in native SQL.
	 */
	String clause();
}
