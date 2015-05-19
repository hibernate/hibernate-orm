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
 * Complementary information to a table either primary or secondary.
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Table {
	/**
	 * name of the targeted table.
	 */
	String appliesTo();

	/**
	 * Indexes.
	 */
	Index[] indexes() default {};

	/**
	 * define a table comment.
	 */
	String comment() default "";

	/**
	 * Defines the Foreign Key name of a secondary table pointing back to the primary table.
	 */
	ForeignKey foreignKey() default @ForeignKey( name="" );

	/**
	 * If set to JOIN, the default, Hibernate will use an inner join to retrieve a
	 * secondary table defined by a class or its superclasses and an outer join for a
	 * secondary table defined by a subclass.
	 * If set to select then Hibernate will use a
	 * sequential select for a secondary table defined on a subclass, which will be issued only if a row
	 * turns out to represent an instance of the subclass. Inner joins will still be used to retrieve a
	 * secondary defined by the class and its superclasses.
	 *
	 * <b>Only applies to secondary tables</b>
	 */
	FetchMode fetch() default FetchMode.JOIN;

	/**
	 * If true, Hibernate will not try to insert or update the properties defined by this join.
	 *
	 * <b>Only applies to secondary tables</b>
	 */
	boolean inverse() default false;

	/**
	 * If enabled, Hibernate will insert a row only if the properties defined by this join are non-null
	 * and will always use an outer join to retrieve the properties.
	 *
	 * <b>Only applies to secondary tables</b>
	 */
	boolean optional() default true;

	/**
	 * Defines a custom SQL insert statement.
	 *
	 * <b>Only applies to secondary tables</b>
	 */
	SQLInsert sqlInsert() default @SQLInsert(sql="");

	/**
	 * Defines a custom SQL update statement.
	 *
	 * <b>Only applies to secondary tables</b>
	 */
	SQLUpdate sqlUpdate() default @SQLUpdate(sql="");

	/**
	 * Defines a custom SQL delete statement.
	 *
	 * <b>Only applies to secondary tables</b>
	 */
	SQLDelete sqlDelete() default @SQLDelete(sql="");
}
