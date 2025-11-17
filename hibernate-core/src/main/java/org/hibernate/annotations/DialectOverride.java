/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows certain annotations to be overridden in a given SQL {@link Dialect}.
 * <p>
 * For example, a {@link org.hibernate.annotations.Formula} annotation may be
 * customized for a given {@code Dialect} using the {@link Formula} annotation.
 * <pre>
 * &#64;Formula(value = "(rate * 100) || '%'")
 * &#64;DialectOverride.Formula(dialect = MySQLDialect.class,
 *                          override = &#64;Formula("concat(rate * 100, '%')"))
 * &#64;DialectOverride.Formula(dialect = DB2Dialect.class,
 *                          override = &#64;Formula("varchar_format(rate * 100) || '%'"))
 * &#64;DialectOverride.Formula(dialect = OracleDialect.class,
 *                          override = &#64;Formula("to_char(rate * 100) || '%'"))
 * &#64;DialectOverride.Formula(dialect = SQLServerDialect.class,
 *                          override = &#64;Formula("ltrim(str(rate * 100, 10, 2)) + '%'"))
 * &#64;DialectOverride.Formula(dialect = SybaseDialect.class,
 *                          override = &#64;Formula("ltrim(str(rate * 100, 10, 2)) + '%'"))
 * private String ratePercent;
 * </pre>
 * <p>
 * An annotation may even be customized for a specific range of <em>versions</em>
 * of the dialect by specifying a {@link Version}.
 * <ul>
 *     <li>{@link Formula#dialect() dialect} specifies the SQL dialect to which
 *         the override applies,
 *     <li>{@link Formula#sameOrAfter() sameOrAfter} specifies that the override
 *         applies to all versions beginning with the given version, and
 *     <li>{@link Formula#before() before} specifies that the override applies
 *         to all versions earlier than the given version.
 * </ul>
 * <p>
 *
 * @since 6.0
 * @author Gavin King
 */
@Incubating
public interface DialectOverride {

	/**
	 * Identifies a database version.
	 *
	 * @see org.hibernate.dialect.DatabaseVersion
	 */
	@Retention(RUNTIME)
	@interface Version {
		int major();
		int minor() default 0;
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.Check}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(Checks.class)
	@OverridesAnnotation(org.hibernate.annotations.Check.class)
	@interface Check {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.Check override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface Checks {
		Check[] value();
	}

	/**
	 * Specializes an {@link org.hibernate.annotations.SQLOrder}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@Repeatable(SQLOrders.class)
	@OverridesAnnotation(org.hibernate.annotations.SQLOrder.class)
	@interface SQLOrder {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.SQLOrder override();
	}
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@interface SQLOrders {
		SQLOrder[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.ColumnDefault}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@Repeatable(ColumnDefaults.class)
	@OverridesAnnotation(org.hibernate.annotations.ColumnDefault.class)
	@interface ColumnDefault {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.ColumnDefault override();
	}
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@interface ColumnDefaults {
		ColumnDefault[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.GeneratedColumn}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@Repeatable(GeneratedColumns.class)
	@OverridesAnnotation(org.hibernate.annotations.GeneratedColumn.class)
	@interface GeneratedColumn {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.GeneratedColumn override();
	}
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@interface GeneratedColumns {
		GeneratedColumn[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.DiscriminatorFormula}
	 * in a certain dialect.
	 */
	@Target(TYPE)
	@Retention(RUNTIME)
	@Repeatable(DiscriminatorFormulas.class)
	@OverridesAnnotation(org.hibernate.annotations.DiscriminatorFormula.class)
	@interface DiscriminatorFormula {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.DiscriminatorFormula override();
	}
	@Target(TYPE)
	@Retention(RUNTIME)
	@interface DiscriminatorFormulas {
		DiscriminatorFormula[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.Formula}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@Repeatable(Formulas.class)
	@OverridesAnnotation(org.hibernate.annotations.Formula.class)
	@interface Formula {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.Formula override();
	}
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@interface Formulas {
		Formula[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.JoinFormula}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@Repeatable(JoinFormulas.class)
	@OverridesAnnotation(org.hibernate.annotations.JoinFormula.class)
	@interface JoinFormula {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.JoinFormula override();
	}
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@interface JoinFormulas {
		JoinFormula[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.SQLRestriction}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(SQLRestrictions.class)
	@OverridesAnnotation(org.hibernate.annotations.SQLRestriction.class)
	@interface SQLRestriction {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.SQLRestriction override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface SQLRestrictions {
		SQLRestriction[] value();
	}

	/**
	 * Specializes {@link org.hibernate.annotations.Filters}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(FilterOverrides.class)
	@OverridesAnnotation(org.hibernate.annotations.Filters.class)
	@interface Filters {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.Filters override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface FilterOverrides {
		Filters[] value();
	}

	/**
	 * Specializes {@link org.hibernate.annotations.FilterDefs}
	 * in a certain dialect.
	 */
	@Target({PACKAGE, TYPE})
	@Retention(RUNTIME)
	@Repeatable(FilterDefOverrides.class)
	@OverridesAnnotation(org.hibernate.annotations.FilterDefs.class)
	@interface FilterDefs  {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.FilterDefs override();
	}
	@Target({PACKAGE, TYPE})
	@Retention(RUNTIME)
	@interface FilterDefOverrides {
		FilterDefs[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.SQLSelect}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(SQLSelects.class)
	@OverridesAnnotation(org.hibernate.annotations.SQLSelect.class)
	@interface SQLSelect {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.SQLSelect override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface SQLSelects {
		SQLSelect[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.SQLInsert}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(SQLInserts.class)
	@OverridesAnnotation(org.hibernate.annotations.SQLInsert.class)
	@interface SQLInsert {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.SQLInsert override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface SQLInserts {
		SQLInsert[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.SQLUpdate}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(SQLUpdates.class)
	@OverridesAnnotation(org.hibernate.annotations.SQLUpdate.class)
	@interface SQLUpdate {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.SQLUpdate override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface SQLUpdates {
		SQLUpdate[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.SQLDelete}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(SQLDeletes.class)
	@OverridesAnnotation(org.hibernate.annotations.SQLDelete.class)
	@interface SQLDelete {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.SQLDelete override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface SQLDeletes {
		SQLDelete[] value();
	}

	/**
	 * Specializes a {@link org.hibernate.annotations.SQLDeleteAll}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@Repeatable(SQLDeleteAlls.class)
	@OverridesAnnotation(org.hibernate.annotations.SQLDeleteAll.class)
	@interface SQLDeleteAll {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.SQLDeleteAll override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface SQLDeleteAlls {
		SQLDeleteAll[] value();
	}

	/**
	 * Marks an annotation type as a dialect-specific override for
	 * some other annotation type.
	 * <p>
	 * The marked annotation must have the following members:
	 * <ul>
	 *     <li>{@code Class<? extends Dialect> dialect()},
	 *     <li>{@code Version before()},
	 *     <li>{@code Version sameOrAfter()}, and
	 *     <li>{@code A override()}, where {@code A} is the type
	 *     of annotation which the marked annotation overrides.
	 * </ul>
	 */
	@Target({ANNOTATION_TYPE})
	@Retention(RUNTIME)
	@interface OverridesAnnotation {
		/**
		 * The class of the annotation that is overridden.
		 */
		Class<? extends Annotation> value();
	}
}
