/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.tuple.DefaultValueGeneration;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that a column has a {@code DEFAULT} value specified in DDL,
 * and whether Hibernate should fetch the defaulted value from the database.
 * <p>
 * <ul>
 * <li>If {@code fetch=false}, the default, the program must explicitly call
 * {@link org.hibernate.Session#refresh(Object)} to retrieve the generated value.
 * <li>If {@code fetch=true}, Hibernate will automatically retrieve the generated
 * value each time it updates the database.
 * </ul>
 *
 * @author Steve Ebersole
 *
 * @see GeneratedColumn
 */
@Target( {FIELD, METHOD} )
@Retention( RUNTIME )
@ValueGenerationType(generatedBy = DefaultValueGeneration.class)
public @interface ColumnDefault {
	/**
	 * The {@code DEFAULT} value to use in generated DDL.
	 *
	 * @return a SQL expression that evaluates to the default column value
	 */
	String value();

	/**
	 * The name of the generated column. Optional for a field or property
	 * mapped to a single column.
	 * <ul>
	 * <li>If the column name is explicitly specified using the
	 * {@link jakarta.persistence.Column @Column} annotation, the name given
	 * here must match the name specified by
	 * {@link jakarta.persistence.Column#name}.
	 * <li>Or, if the column name is inferred implicitly by Hibernate, the
	 * name given here must match the inferred name.
	 * </ul>
	 */
	String name() default "";

	/**
	 * Determines if the defaulted value is fetched from the database after
	 * every SQL {@code INSERT}.
	 * <p>
	 * Fetching is disabled by default, and so it is the responsibility of
	 * the Java program to maintain the value of the mapped attribute when
	 * its value is first defaulted by the database.
	 * <p>
	 * Note that {@code fetch=true} is a synonym for {@code @Generated(INSERT)}.
	 *
	 * @return {@code true} if a {@code SELECT} should be executed to read
	 *                      the defaulted value
	 */
	boolean fetch() default false;
}
