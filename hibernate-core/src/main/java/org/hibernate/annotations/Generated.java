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

import org.hibernate.tuple.GeneratedValueGeneration;
import org.hibernate.tuple.GenerationTiming;

/**
 * Specifies that the value of the annotated property is generated by the
 * database. The generated value will be automatically retrieved using a
 * SQL {@code select} after it is generated.
 * <p>
 * {@code @Generated} relieves the program of the need to explicitly call
 * {@link org.hibernate.Session#refresh(Object)} to synchronize state held
 * in memory with state generated by the database when a SQL {@code insert}
 * or {@code update} is executed.
 * <p>
 * This is most useful when:
 * <ul>
 * <li>a database table has a column value populated by a database trigger,
 * <li>a mapped column has a default value defined in DDL, in which case
 *     {@code Generated(INSERT)} is used in conjunction with
 *     {@link ColumnDefault},
 * <li>a {@linkplain #sql() SQL expression} is used to compute the value of
 *     a mapped column, or
 * <li>when a custom SQL {@link SQLInsert insert} or {@link SQLUpdate update}
 *     statement specified by an entity assigns a value to the annotated
 *     property of the entity, or {@linkplain #writable() transforms} the
 *     value currently assigned to the annotated property.
 * </ul>
 * On the other hand:
 * <ul>
 * <li>for identity/autoincrement columns mapped to an identifier property,
 *     use {@link jakarta.persistence.GeneratedValue}, and
 * <li>for columns with a {@code generated always as} clause, prefer the
 *     {@link GeneratedColumn} annotation, so that Hibernate automatically
 *     generates the correct DDL.
 * </ul>
 *
 * @author Emmanuel Bernard
 *
 * @see jakarta.persistence.GeneratedValue
 * @see ColumnDefault
 * @see GeneratedColumn
 */
@ValueGenerationType( generatedBy = GeneratedValueGeneration.class )
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Generated {
	/**
	 * Specifies the events that cause the value to be generated by the
	 * database.
	 * <ul>
	 * <li>If {@link GenerationTiming#INSERT}, the generated value will be
	 *     selected after each SQL {@code insert} statement is executed.
	 * <li>If {@link GenerationTiming#UPDATE}, the generated value will be
	 *     selected after each SQL {@code update} statement is executed.
	 * <li>If {@link GenerationTiming#ALWAYS}, the generated value will be
	 *     selected after each SQL {@code insert} or {@code update}
	 *     statement is executed.
	 * </ul>
	 */
	GenerationTiming timing() default GenerationTiming.ALWAYS;

	/**
	 * Specifies the events that cause the value to be generated by the
	 * database.
	 * <ul>
	 * <li>If {@link GenerationTime#INSERT}, the generated value will be
	 *     selected after each SQL {@code insert} statement is executed.
	 * <li>If {@link GenerationTime#UPDATE}, the generated value will be
	 *     selected after each SQL {@code update} statement is executed.
	 * <li>If {@link GenerationTime#ALWAYS}, the generated value will be
	 *     selected after each SQL {@code insert} or {@code update}
	 *     statement is executed.
	 * </ul>
	 * @deprecated use {@link #timing()}
	 */
	@Deprecated(since = "6.2")
	GenerationTime value() default GenerationTime.ALWAYS;

	/**
	 * A SQL expression used to generate the value of the column mapped by
	 * the annotated property. The expression is included in generated SQL
	 * {@code insert} and {@code update} statements.
	 */
	String sql() default "";

	/**
	 * Determines if the value currently assigned to the annotated property
	 * is included in SQL {@code insert} and {@code update} statements. This
	 * is useful if the generated value is obtained by transforming the
	 * assigned property value as it is being written.
	 * <p>
	 * Often used in combination with {@link SQLInsert}, {@link SQLUpdate},
	 * or {@link ColumnTransformer#write()}.
	 *
	 * @return {@code true} if the current value should be included in SQL
	 *         {@code insert} and {@code update} statements.
	 */
	boolean writable() default false;
}
