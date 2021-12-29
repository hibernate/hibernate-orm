/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.dialect.Dialect;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a SQL expression or fragment of SQL that should be used in a given dialect.
 * Allows a portable program to embed unportable native SQL in annotations.
 *
 * @see Formula#overrides()
 * @see JoinFormula#overrides()
 * @see DiscriminatorFormula#overrides()
 * @see Check#overrides()
 * @see ColumnDefault#overrides()
 * @see Filter#overrides()
 * @see FilterDef#overrides()
 * @see Where#overrides()
 * @see OrderBy#overrides()
 *
 * @author Gavin King
 */
@Retention(RUNTIME)
public @interface DialectOverride {
	/**
	 * The {@link Dialect} to which this override applies.
	 *
	 * @return the concrete Java class of the {@code Dialect}
	 */
	Class<? extends Dialect> dialect();

	/**
	 * The SQL expression or SQL fragment that should be used in the specified dialect.
	 */
	String value();
}
