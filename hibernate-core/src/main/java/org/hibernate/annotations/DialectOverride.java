/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 *
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
	 * Specializes an {@link org.hibernate.annotations.OrderBy}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@Repeatable(OrderBys.class)
	@OverridesAnnotation(org.hibernate.annotations.OrderBy.class)
	@interface OrderBy {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.OrderBy override();
	}
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	@interface OrderBys {
		OrderBy[] value();
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
	 * Specializes a {@link org.hibernate.annotations.Where}
	 * in a certain dialect.
	 */
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@OverridesAnnotation(org.hibernate.annotations.Where.class)
	@interface Where {
		/**
		 * The {@link Dialect} in which this override applies.
		 */
		Class<? extends Dialect> dialect();
		Version before() default @Version(major = MAX_VALUE);
		Version sameOrAfter() default @Version(major = MIN_VALUE);

		org.hibernate.annotations.Where override();
	}
	@Target({METHOD, FIELD, TYPE})
	@Retention(RUNTIME)
	@interface Wheres {
		Where[] value();
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
