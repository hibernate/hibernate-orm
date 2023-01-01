/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that an Oracle-style {@code rowid} should be used in SQL
 * {@code update} statements for an entity, instead of the primary key.
 * <p>
 * If the {@linkplain org.hibernate.dialect.Dialect SQL dialect} does
 * not support some sort of {@code rowid}, this annotation is ignored.
 *
 * @author Steve Ebersole
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface RowId {
	/**
	 * Specifies the {@code rowid} identifier.
	 * <p>
	 * For example, on Oracle, this should be just {@code "rowid"}.
	 *
	 * @deprecated the {@code rowid} identifier is now inferred
	 * automatically from the {@link org.hibernate.dialect.Dialect}
	 */
	@Deprecated(since = "6.2")
	String value() default "";
}
