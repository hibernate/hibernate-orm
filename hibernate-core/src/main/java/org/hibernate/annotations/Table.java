/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.persistence.ForeignKey;
import javax.persistence.Index;

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
