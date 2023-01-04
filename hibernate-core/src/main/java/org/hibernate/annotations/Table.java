/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Complementary information for a table declared using the {@link jakarta.persistence.Table},
 * or {@link jakarta.persistence.SecondaryTable} annotation. Usually used only for secondary
 * tables.
 *
 * @author Emmanuel Bernard
 *
 * @see jakarta.persistence.Table
 * @see jakarta.persistence.SecondaryTable
 *
 * @deprecated The options available here are all now offered by other newer and better-designed
 *             annotations in this package. This annotation will soon be removed, since it's very
 *             annoying to have two annotations named {@code @Table}.
 *
 */
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(Tables.class)
@Deprecated(since = "6.2", forRemoval = true)
public @interface Table {
	/**
	 * The name of the targeted table.
	 */
	String appliesTo();

	/**
	 * Indexes.
	 *
	 * @deprecated use {@link jakarta.persistence.Table#indexes()} or
	 *             {@link jakarta.persistence.SecondaryTable#indexes()}
	 */
	@Deprecated(since = "6.0")
	Index[] indexes() default {};

	/**
	 * A check constraint, written in native SQL.
	 *
	 * @deprecated use {@link Check}.
	 */
	@Deprecated(since = "6.2")
	String checkConstraint() default "";

	/**
	 * Specifies comment to add to the generated DDL for the table.
	 *
	 * @deprecated use {@link Comment}
	 */
	@Deprecated(since = "6.2")
	String comment() default "";

	/**
	 * Specifies a foreign key of a secondary table, which points back to the primary table.
	 *
	 * @apiNote Only relevant to secondary tables
	 * @deprecated use {@link jakarta.persistence.SecondaryTable#foreignKey()}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	ForeignKey foreignKey() default @ForeignKey(name = "");

	/**
	 * @deprecated This setting has no effect in Hibernate 6
	 */
	@Deprecated(since = "6.2")
	FetchMode fetch() default FetchMode.JOIN;

	/**
	 * If enabled, Hibernate will never insert or update the columns of the secondary table.
	 *
	 * @apiNote Only relevant to secondary tables
	 * @deprecated use {@link SecondaryRow#owned()}
	 */
	@Deprecated(since = "6.2")
	boolean inverse() default false;

	/**
	 * If enabled, Hibernate will insert a row only if the columns of the secondary table
	 * would not all be null, and will always use an outer join to read the columns. Thus,
	 * by default, Hibernate avoids creating a row of null values.
	 *
	 * @apiNote Only relevant to secondary tables
	 * @deprecated use {@link SecondaryRow#optional()}
	 */
	@Deprecated(since = "6.2")
	boolean optional() default true;

	/**
	 * Defines a custom SQL insert statement.
	 *
	 * @apiNote Only relevant to secondary tables
	 * @deprecated use {@link SQLInsert#table()} to specify the secondary table
	 */
	@Deprecated(since="6.2")
	SQLInsert sqlInsert() default @SQLInsert(sql="");

	/**
	 * Defines a custom SQL update statement.
	 *
	 * @apiNote Only relevant to secondary tables
	 * @deprecated use {@link SQLInsert#table()} to specify the secondary table
	 */
	@Deprecated(since="6.2")
	SQLUpdate sqlUpdate() default @SQLUpdate(sql="");

	/**
	 * Defines a custom SQL delete statement.
	 *
	 * @apiNote Only relevant to secondary tables
	 * @deprecated use {@link SQLInsert#table()} to specify the secondary table
	 */
	@Deprecated(since="6.2")
	SQLDelete sqlDelete() default @SQLDelete(sql="");
}
